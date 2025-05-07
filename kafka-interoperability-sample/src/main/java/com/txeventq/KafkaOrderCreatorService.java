package com.txeventq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.txeventq.models.Order;
import com.txeventq.utilities.EventGenerator;
import com.txeventq.utilities.InputUtil;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

public class KafkaOrderCreatorService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaOrderCreatorService.class);

    private static final String TOPIC = "Orders";

    public static void main(String[] args) throws IOException {
        Properties properties = configureProducer();
        try (org.apache.kafka.clients.producer.KafkaProducer<String, String> producer = new KafkaProducer<>(
                properties)) {
            ObjectMapper objectMapper = new ObjectMapper();

            while (true) {
                InputUtil.UserCommand command = InputUtil.readUserCommand();

                if (command.bulkMode) {
                    List<Future<RecordMetadata>> futures = new ArrayList<>();
                    for (int i = 0; i < command.count; i++) {
                        Order order = EventGenerator.generateOrderEvent();
                        int key = order.getCustomerId();
                        String value = objectMapper.writeValueAsString(order); // Convert Order object to JSON string
                        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, String.valueOf(key), value);

                        Future<RecordMetadata> future = producer.send(record, new Callback() {
                            @Override
                            public void onCompletion(RecordMetadata metadata, Exception exception) {
                                if (exception != null) {
                                    exception.printStackTrace();
                                } else {
                                    logger.warn("Record sent to topic {} partition {} with offset {}",
                                            metadata.topic(), metadata.partition(), metadata.offset());
                                }
                            }
                        });
                        logger.warn("Order: {}", order);
                        futures.add(future);
                    }
                    producer.flush(); // Flush at the end of bulk send
                } else {
                    for (int i = 0; i < command.count; i++) {
                        Order order = EventGenerator.generateOrderEvent();
                        int key = order.getCustomerId();
                        String value = objectMapper.writeValueAsString(order); // Convert Order object to JSON string
                        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, String.valueOf(key), value);

                        producer.send(record, new Callback() {
                            @Override
                            public void onCompletion(RecordMetadata metadata, Exception exception) {
                                if (exception != null) {
                                    exception.printStackTrace();
                                } else {
                                    logger.warn("Record sent to topic {} partition {} with offset {}",
                                            metadata.topic(), metadata.partition(), metadata.offset());
                                }
                            }
                        });
                        logger.warn("Order: {}", order);
                        producer.flush(); // Flush after each message if not in bulk mode
                    }
                }
            }
        }
    }

    private static Properties configureProducer() {
        Properties properties = new Properties();
        try (InputStream input = KafkaOrderCreatorService.class.getClassLoader()
                .getResourceAsStream("kafka-order-creator.properties")) {
            if (input == null) {
                logger.error("Sorry, unable to find kafka-order-creator.properties");
                return properties;
            }

            properties.load(input);

            logger.info("Loaded kafka-order-creator.properties");

        } catch (IOException ex) {
            logger.error("Error loading kafka-order-creator.properties", ex);
        }

        return properties;
    }
}
