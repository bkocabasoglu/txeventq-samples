package com.oracle.osd;

import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import oracle.jakarta.AQ.AQException;
import oracle.jakarta.jms.AQjmsFactory;
import oracle.jakarta.jms.AQjmsSession;
import oracle.jakarta.jms.AQjmsTextMessage;
import oracle.jakarta.jms.AQjmsTopicSubscriber;
import oracle.jdbc.pool.OracleDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class ConsumeTxEventQ19c {

    private static final String node1Url = "[your_node1_url]";
    private static final String node2Url = "[your_node2_url]";
    private static final String username = "[your_user]";
    private static final String password = "[your_password]";
    private static final String topicName = "ClaimUpdatesTopic";
    private static final String subscriberName = "ClaimUpdatesSubscriber";

    public static void main(String[] args) throws AQException, SQLException, JMSException {
        if (args.length < 1) {
            System.err.println("Please provide a parameter: 'node1' or 'node2'");
            return;
        }

        String selectedUrl = args[0].equalsIgnoreCase("node2") ? node2Url : node1Url;

        // Database connection settings
        OracleDataSource ds = null;
        try {
            ds = new OracleDataSource();
            ds.setURL(selectedUrl);
            ds.setUser(username);
            ds.setPassword(password);
        } catch (SQLException e) {
            System.err.println("Error initializing data source: " + e.getMessage());
            return;
        }

        try (Connection con = ds.getConnection(); TopicConnection conn = AQjmsFactory.getTopicConnectionFactory(ds).createTopicConnection()) {

            System.out.println("Connected to database and JMS." + (args[0].equalsIgnoreCase("node2") ? "node2" : "node1"));

            // Create session and topic
            AQjmsSession session = (AQjmsSession) conn.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.getTopic(username, topicName);
            conn.start();

            // Create durable subscriber
            AQjmsTopicSubscriber subscriber = (AQjmsTopicSubscriber) session.createDurableSubscriber(topic, subscriberName);

            System.out.println("Waiting for messages...");

            // Infinite loop to receive messages
            while (true) {
                AQjmsTextMessage message = (AQjmsTextMessage) subscriber.receive(1_000); // Timeout: 1 second

                if (message != null) {
                    String messageText = message.getText();
                    System.out.println(Objects.requireNonNullElse(messageText, "Received empty message."));
                    session.commit();  // Only commit if message received
                }
            }
        } catch (SQLException | JMSException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
