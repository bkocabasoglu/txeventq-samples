package com.txeventq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.txeventq.models.Order;
import oracle.jdbc.pool.OracleDataSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.oracle.okafka.clients.consumer.KafkaConsumer;
import org.oracle.okafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OrderProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessorService.class);
    private static final String url = "jdbc:oracle:thin:@[YOUR_TNS_ALIAS]]?TNS_ADMIN=[YOUR_WALLET_FOLDER_PATH]";
    private static final String username = "TXEVENTQ_ADMIN";
    private static final String password = "[Your User Password]";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String topicName = "";
    private static String ordersToShipTopic = "";
    private static String ordersToReconcileTopic = "";
    private static OracleDataSource ods;

    public static void main(String[] args) {

        Properties appProperties = getProperties();
        OrderProcessorService.topicName = appProperties.getProperty("topic.name");
        OrderProcessorService.ordersToShipTopic = appProperties.getProperty("topic.orders.ship");
        OrderProcessorService.ordersToReconcileTopic = appProperties.getProperty("topic.orders.reconcile");

        // Initialize the OracleDataSource
        ods = createDataSource();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(appProperties); Producer<String, String> producer = new KafkaProducer<>(appProperties)) {
            consumeOrders(consumer, topicName, producer);
        }
    }

    private static void consumeOrders(KafkaConsumer<String, String> consumer, final String topicName, Producer<String, String> producer) {
        consumer.subscribe(Collections.singletonList(topicName));
        logger.info("Subscribed to topic: {}", topicName);

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, String> record : records) {
                try {
                    Order consumedOrder = objectMapper.readValue(record.value(), Order.class);
                    logger.warn("Consumed Order: {} from topic {}, partition {}, offset {}", consumedOrder, record.topic(), record.partition(), record.offset());
                    //System.out.printf("partition = %d, offset = %d, key = %s, value =%s\n  ", record.partition(), record.offset(), record.key(), record.value());

                    int productId = consumedOrder.getProductId();
                    int numberOfUnits = consumedOrder.getNumberOfUnits();
                    logger.info("Processing order for productId: {}, quantity: {}", productId, numberOfUnits);

                    // 1. Query Product Inventory
                    ProductInventory productInventory = getProductInventory(productId);

                    if (productInventory != null) {
                        // 2. Check if enough items are in stock
                        if (productInventory.itemsInStock() >= numberOfUnits) {
                            logger.info("Sufficient stock available. itemsInStock = {}, numberOfUnits = {}", productInventory.itemsInStock(), numberOfUnits);

                            // 3. Deduct amount from inventory
                            deductFromInventory(productId, numberOfUnits);

                            // 4. Calculate total order amount
                            BigDecimal unitPrice = productInventory.unitPrice();
                            BigDecimal totalOrderAmount = unitPrice.multiply(BigDecimal.valueOf(numberOfUnits));
                            logger.info("Calculated total order amount: {}", totalOrderAmount);

                            // Create a new JSON payload with totalOrderAmount
                            JsonNode orderNode = objectMapper.readTree(record.value());
                            ((ObjectNode) orderNode).put("totalOrderAmount", totalOrderAmount);

                            // Produce event to "OrdersToShip"
                            String orderString = orderNode.toString();
                            ProducerRecord<String, String> shipRecord = new ProducerRecord<>(ordersToShipTopic, String.valueOf(consumedOrder.getCustomerId()), orderString);
                            producer.send(shipRecord);
                            logger.info("Order sent to OrdersToShip topic: {}", orderString);

                        } else {
                            logger.warn("Insufficient stock available. itemsInStock = {}, numberOfUnits = {}", productInventory.itemsInStock(), numberOfUnits);
                            // Produce event to "OrdersToReconcile"
                            ProducerRecord<String, String> reconcileRecord = new ProducerRecord<>(ordersToReconcileTopic, String.valueOf(consumedOrder.getCustomerId()), record.value());
                            producer.send(reconcileRecord);
                            logger.warn("Order sent to OrdersToReconcile topic: {}", record.value());
                        }
                    } else {
                        logger.error("Product not found in inventory for productId: {}", productId);
                        // Optionally send to OrdersToReconcile or handle the error as needed
                        ProducerRecord<String, String> reconcileRecord = new ProducerRecord<>(ordersToReconcileTopic, String.valueOf(consumedOrder.getCustomerId()), record.value());
                        producer.send(reconcileRecord);
                        logger.warn("Order sent to OrdersToReconcile topic: {}", record.value());
                    }

                } catch (Exception e) {
                    logger.error("Error processing record: {}", record.value(), e);
                }
            }

            if (records.count() > 0) {
                consumer.commitSync();
            }
        }
    }

    private static ProductInventory getProductInventory(int productId) {
        ProductInventory product = null;
        try (Connection connection = ods.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("SELECT productName, description, itemsInStock, unitPrice FROM ProductInventory WHERE productId = ?")) {

            preparedStatement.setInt(1, productId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String productName = resultSet.getString("productName");
                String description = resultSet.getString("description");
                int itemsInStock = resultSet.getInt("itemsInStock");
                BigDecimal unitPrice = resultSet.getBigDecimal("unitPrice");
                product = new ProductInventory(productId, productName, description, itemsInStock, unitPrice);
                logger.info("Retrieved product inventory: " + product);
            } else {
                logger.warn("Product with productId " + productId + " not found in inventory.");
            }

        } catch (SQLException e) {
            logger.error("Error querying ProductInventory table:", e);
        }
        return product;
    }

    private static void deductFromInventory(int productId, int numberOfUnits) {
        try (Connection connection = ods.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("UPDATE ProductInventory SET itemsInStock = itemsInStock - ? WHERE productId = ?")) {

            preparedStatement.setInt(1, numberOfUnits);
            preparedStatement.setInt(2, productId);
            int rowsUpdated = preparedStatement.executeUpdate();

            if (rowsUpdated > 0) {
                logger.info("Deducted " + numberOfUnits + " units from inventory for productId: " + productId + " and inventory updated successfully.");
            } else {
                logger.warn("Could not deduct from inventory for productId: " + productId + ". Product may not exist.");
            }

        } catch (SQLException e) {
            logger.error("Error updating ProductInventory table:", e);
        }
    }

    private static OracleDataSource createDataSource() {
        OracleDataSource ds = null;
        try {
            ds = new OracleDataSource();
            ds.setURL(OrderProcessorService.url);
            ds.setUser(username);
            ds.setPassword(password);
            logger.info("OracleDataSource created successfully.");
        } catch (SQLException e) {
            logger.error("Error creating OracleDataSource:", e);
            e.printStackTrace();
        }
        return ds;
    }

    private static Properties getProperties() {
        Properties appProperties = new Properties();
        try (InputStream inputStream = OrderProcessorService.class.getClassLoader().getResourceAsStream("order-processor-consumer-config.properties")) {
            if (inputStream != null) {
                appProperties.load(inputStream);
                logger.info("Loaded properties from order-processor-consumer-config.properties");
            } else {
                throw new FileNotFoundException("property file 'order-processor-consumer-config.properties' not found.");
            }
        } catch (IOException e) {
            logger.error("Error loading properties:", e);
            e.printStackTrace();
        }
        return appProperties;
    }

    //Record to represent ProductInventory
    private record ProductInventory(int productId, String productName, String description, int itemsInStock,
                                    BigDecimal unitPrice) {
    }
}
