package com.oracle.osd.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration manager for EventConsumerApp.
 * Loads configuration from properties file and provides typed access to configuration values.
 */
public class ConsumerConfig {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerConfig.class);
    private static final String CONFIG_FILE = "consumer-config.properties";

    private final Properties properties;

    public ConsumerConfig() {
        this.properties = loadProperties();
    }

    private Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                logger.warn("Configuration file {} not found, using default values", CONFIG_FILE);
                return getDefaultProperties();
            }

            props.load(inputStream);
            logger.info("Loaded configuration from {}", CONFIG_FILE);

        } catch (IOException e) {
            logger.error("Failed to load configuration file {}, using defaults", CONFIG_FILE, e);
            return getDefaultProperties();
        }

        return props;
    }

    private Properties getDefaultProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("consumer.threads", "3");
        defaults.setProperty("consumer.receive.timeout.ms", "1000");
        defaults.setProperty("consumer.action.queue", "ActionUpdatesTopic");
        defaults.setProperty("consumer.action.subscriber", "ActionUpdatesSubscriber1");
        defaults.setProperty("consumer.payment.queue", "PaymentUpdatesTopic");
        defaults.setProperty("consumer.payment.subscriber", "PaymentUpdatesSubscriber1");
        defaults.setProperty("consumer.statement.queue", "TransactionStatementTopic");
        defaults.setProperty("consumer.statement.subscriber", "StatementsUpdatesSubscriber1");
        defaults.setProperty("consumer.error.retry.delay.seconds", "2");
        defaults.setProperty("consumer.shutdown.timeout.seconds", "10");
        return defaults;
    }

    // Threading Configuration
    public int getConsumerThreads() {
        return getIntProperty("consumer.threads", 3);
    }

    // Message Consumption Configuration
    public long getReceiveTimeoutMs() {
        return getLongProperty("consumer.receive.timeout.ms", 1000L);
    }

    // Queue Configurations
    public String getActionQueue() {
        return getProperty("consumer.action.queue", "ActionUpdatesTopic");
    }

    public String getActionSubscriber() {
        return getProperty("consumer.action.subscriber", "ActionUpdatesSubscriber1");
    }

    public String getPaymentQueue() {
        return getProperty("consumer.payment.queue", "PaymentUpdatesTopic");
    }

    public String getPaymentSubscriber() {
        return getProperty("consumer.payment.subscriber", "PaymentUpdatesSubscriber1");
    }

    public String getStatementQueue() {
        return getProperty("consumer.statement.queue", "TransactionStatementTopic");
    }

    public String getStatementSubscriber() {
        return getProperty("consumer.statement.subscriber", "StatementsUpdatesSubscriber1");
    }

    // Error Handling Configuration
    public int getRetryDelaySeconds() {
        return getIntProperty("consumer.error.retry.delay.seconds", 2);
    }

    public int getShutdownTimeoutSeconds() {
        return getIntProperty("consumer.shutdown.timeout.seconds", 10);
    }

    // Helper methods
    private String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for property {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    private long getLongProperty(String key, long defaultValue) {
        try {
            return Long.parseLong(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid long value for property {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Logs the current configuration values for debugging purposes.
     */
    public void logConfiguration() {
        logger.info("=== Consumer Configuration ===");
        logger.info("Consumer Threads: {}", getConsumerThreads());
        logger.info("Receive Timeout (ms): {}", getReceiveTimeoutMs());
        logger.info("Action Queue: {} -> Subscriber: {}", getActionQueue(), getActionSubscriber());
        logger.info("Payment Queue: {} -> Subscriber: {}", getPaymentQueue(), getPaymentSubscriber());
        logger.info("Statement Queue: {} -> Subscriber: {}", getStatementQueue(), getStatementSubscriber());
        logger.info("Retry Delay (seconds): {}", getRetryDelaySeconds());
        logger.info("Shutdown Timeout (seconds): {}", getShutdownTimeoutSeconds());
        logger.info("===============================");
    }
}
