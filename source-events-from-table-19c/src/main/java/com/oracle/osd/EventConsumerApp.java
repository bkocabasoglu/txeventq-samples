package com.oracle.osd;

import com.oracle.osd.config.ConsumerConfig;
import com.oracle.osd.processors.MessageHandler;
import com.oracle.osd.utils.DatabaseUtils;
import jakarta.jms.*;
import oracle.jakarta.AQ.AQException;
import oracle.jakarta.jms.AQjmsFactory;
import oracle.jakarta.jms.AQjmsSession;
import oracle.jakarta.jms.AQjmsTextMessage;
import oracle.jakarta.jms.AQjmsTopicSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Consumer application that consumes events from all 3 TxEventQ subscribers.
 * Configuration is externalized to consumer-config.properties file.
 */
public class EventConsumerApp {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumerApp.class);

    private final ConsumerConfig config;
    private final MessageHandler messageHandler;
    private volatile boolean running = true;
    private ExecutorService executorService;
    private CountDownLatch shutdownLatch;

    public EventConsumerApp() {
        this.config = new ConsumerConfig();
        this.messageHandler = new MessageHandler();
    }

    public static void main(String[] args) {
        EventConsumerApp app = new EventConsumerApp();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));

        try {
            app.start();
        } catch (Exception e) {
            logger.error("Failed to start EventConsumerApp", e);
            System.exit(1);
        }
    }

    public void start() throws AQException, SQLException {
        // Log configuration for debugging
        config.logConfiguration();

        logger.info("Starting EventConsumerApp with {} consumer threads", config.getConsumerThreads());

        executorService = Executors.newFixedThreadPool(config.getConsumerThreads());
        shutdownLatch = new CountDownLatch(config.getConsumerThreads());

        // Start consumer for each subscriber using configuration
        executorService.submit(() -> consumeFromQueue(config.getActionQueue(), config.getActionSubscriber(), "ACTION"));
        executorService.submit(() -> consumeFromQueue(config.getPaymentQueue(), config.getPaymentSubscriber(), "PAYMENT"));
        executorService.submit(() -> consumeFromQueue(config.getStatementQueue(), config.getStatementSubscriber(), "STATEMENT"));

        logger.info("All consumer threads started successfully");

        // Wait for all consumers to finish
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Main thread interrupted, shutting down...");
        }
    }

    private void consumeFromQueue(String queueName, String subscriberName, String eventType) {
        logger.info("Starting consumer for queue: {}, subscriber: {}, type: {}",
                queueName, subscriberName, eventType);

        try {
            var dataSource = DatabaseUtils.createDataSource();

            try (TopicConnection conn = AQjmsFactory.getTopicConnectionFactory(dataSource).createTopicConnection()) {
                // Create TRANSACTED session (like the working example)
                TopicSession session = conn.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
                Topic topic = ((AQjmsSession) session).getTopic(dataSource.getUser(), queueName);
                conn.start();

                // Create durable subscriber
                AQjmsTopicSubscriber subscriber = (AQjmsTopicSubscriber) session.createDurableSubscriber(topic, subscriberName);

                logger.info("Consumer connected to queue: {} with subscriber: {}", queueName, subscriberName);
                logger.info("Starting to check for messages on {} with subscriber {}...", queueName, subscriberName);

                // Use synchronous receive() with configurable timeout
                while (running) {
                    try {
                        // Receive with configurable timeout
                        AQjmsTextMessage message = (AQjmsTextMessage) subscriber.receive(config.getReceiveTimeoutMs());

                        if (message != null) {
                            String messageText = Objects.requireNonNullElse(message.getText(), "");
                            if (!messageText.isEmpty()) {
                                processMessage(message, eventType, subscriberName);
                            } else {
                                logger.warn("Received empty message from subscriber: {}", subscriberName);
                            }
                            session.commit();
                        } else {
                            logger.debug("No messages received from queue: {}, subscriber: {}", queueName, subscriberName);
                        }

                    } catch (JMSException e) {
                        if (running) {
                            logger.error("JMS error receiving message from queue: {}, subscriber: {}: {}",
                                    queueName, subscriberName, e.getMessage());
                            try {
                                session.rollback();
                            } catch (JMSException rollbackEx) {
                                logger.error("Failed to rollback transaction: {}", rollbackEx.getMessage());
                            }
                            TimeUnit.SECONDS.sleep(config.getRetryDelaySeconds());
                        }
                    }
                }

            } catch (JMSException e) {
                if (running) {
                    logger.error("JMS connection error for queue: {}, subscriber: {}: {}", queueName, subscriberName, e.getMessage());
                }
            }
        } catch (Exception e) {
            if (running) {
                logger.error("Unexpected error for queue: {}, subscriber: {}: {}", queueName, subscriberName, e.getMessage());
            }
        }

        logger.info("Consumer stopped for queue: {}, subscriber: {}", queueName, subscriberName);
        shutdownLatch.countDown();
    }

    private void processMessage(Message message, String eventType, String subscriberName) {
        try {
            String messageText = "";
            String messageId = message.getJMSMessageID();
            long timestamp = message.getJMSTimestamp();

            if (message instanceof AQjmsTextMessage textMessage) {
                messageText = Objects.requireNonNullElse(textMessage.getText(), "");
            }

            long currentTimestamp = System.currentTimeMillis();

            // Log basic message information
            logger.info("=== EVENT CONSUMED ===");
            logger.info("Event Type: {}", eventType);
            logger.info("Subscriber: {}", subscriberName);
            logger.info("Message ID: {}", messageId);
            logger.info("JMS Timestamp: {}", new Timestamp(timestamp));
            logger.info("Consumed At: {}", new Timestamp(currentTimestamp));
            logger.info("Message Content: {}", messageText);
            logger.info("=====================");

            // Use structured message processing with validation
            boolean processed = messageHandler.processMessage(messageText, eventType, subscriberName);
            if (!processed) {
                logger.warn("Failed to process {} event from subscriber {} - message may be invalid",
                        eventType, subscriberName);
            }

        } catch (JMSException e) {
            logger.error("Error processing message from subscriber {}: {}", subscriberName, e.getMessage());
        }
    }

    public void shutdown() {
        logger.info("Initiating graceful shutdown...");
        running = false;

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(config.getShutdownTimeoutSeconds(), TimeUnit.SECONDS)) {
                    logger.warn("Executor did not terminate gracefully, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
        }

        logger.info("EventConsumerApp shutdown complete");
    }
}
