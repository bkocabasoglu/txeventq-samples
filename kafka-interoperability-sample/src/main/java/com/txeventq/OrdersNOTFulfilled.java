package com.txeventq;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.oracle.okafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OrdersNOTFulfilled {
    private static String topicName = "";
    private static final Logger logger = LoggerFactory.getLogger(OrdersNOTFulfilled.class);

    public static void main(String[] args) {
        Properties appProperties = getProperties();
        OrdersNOTFulfilled.topicName = appProperties.getProperty("topic.name");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(appProperties);

        consumeOrders(consumer, topicName);
    }

    private static Properties getProperties() {
        Properties appProperties = new Properties();
        try (InputStream inputStream = OrdersNOTFulfilled.class.getClassLoader().getResourceAsStream("orders-not-fulfilled-consumer-config.properties")) {
            if (inputStream != null) {
                appProperties.load(inputStream);
            } else {
                throw new FileNotFoundException("property file 'order-processor-consumer-config.properties' not found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return appProperties;
    }

    private static void consumeOrders(KafkaConsumer<String, String> consumer, final String topicName) {
        consumer.subscribe(Collections.singletonList(topicName));
        try (consumer) {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    logger.warn("Order Not Fulfilled: {}", record.value());
                }

                if (records.count() > 0) {
                    consumer.commitSync();
                } 
            }
        } catch (Exception e) {
            System.out.println("Exception from consumer " + e);
            e.printStackTrace();
        }
    }
}
