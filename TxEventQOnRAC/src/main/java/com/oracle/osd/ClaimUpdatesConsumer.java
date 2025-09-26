package com.oracle.osd;

import com.oracle.osd.models.ClaimEvent;
import com.oracle.osd.utilities.DatabaseConfig;
import com.oracle.osd.utilities.MessageConfig;
import com.oracle.osd.utilities.JsonUtils;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import oracle.jakarta.jms.AQjmsFactory;
import oracle.jakarta.jms.AQjmsSession;
import oracle.jakarta.jms.AQjmsTopicSubscriber;
import oracle.jakarta.jms.JsonMessage;
import oracle.jdbc.pool.OracleDataSource;
import java.sql.SQLException;

public class ClaimUpdatesConsumer {
    private static final DatabaseConfig dbConfig = new DatabaseConfig();
    private static final MessageConfig msgConfig = new MessageConfig();

    public static void main(String[] args) {
        OracleDataSource ds = createDataSource();
        if (ds == null) {
            return;
        }

        try (TopicConnection conn = AQjmsFactory.getTopicConnectionFactory(ds).createTopicConnection()) {

            System.out.println("Connected to the database successfully!");

            AQjmsSession session = (AQjmsSession) conn.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.getTopic(dbConfig.getUsername(), msgConfig.getTopicName());
            conn.start();

            AQjmsTopicSubscriber subscriber = (AQjmsTopicSubscriber) session.createDurableSubscriber(topic, msgConfig.getSubscriberName());

            System.out.println("Waiting for claim events...");

            while (true) {
                JsonMessage message = (JsonMessage) subscriber.receive((int) msgConfig.getConsumerTimeoutMs());

                if (message != null) {
                    String jsonString = message.getJsonString();

                    try {
                        ClaimEvent claimEvent = JsonUtils.fromJson(jsonString, ClaimEvent.class);
                        if (claimEvent != null) {
                            System.out.println("Consumed Claim Event:");
                            System.out.println(claimEvent.toString());
                        } else {
                            System.err.println("Failed to parse JSON message");
                        }
                    } catch (Exception e) {
                        System.out.println("Could not parse as ClaimEvent: " + e.getMessage());
                    }

                    session.commit();
                }
            }
        } catch (SQLException | JMSException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static OracleDataSource createDataSource() {
        try {
            OracleDataSource ds = new OracleDataSource();
            ds.setURL(dbConfig.getUrl());
            ds.setUser(dbConfig.getUsername());
            ds.setPassword(dbConfig.getPassword());
            return ds;
        } catch (SQLException e) {
            System.err.println("Error initializing data source: " + e.getMessage());
            return null;
        }
    }
}