package com.oracle.osd;

import com.oracle.osd.models.ClaimEvent;
import com.oracle.osd.utilities.DatabaseConfig;
import com.oracle.osd.utilities.MessageConfig;
import com.oracle.osd.utilities.JsonUtils;
import com.oracle.osd.utilities.ClaimEventGenerator;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import oracle.jakarta.jms.AQjmsFactory;
import oracle.jakarta.jms.AQjmsSession;
import oracle.jakarta.jms.AQjmsTopicPublisher;
import oracle.jdbc.datasource.impl.OracleDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ClaimUpdatesProducer {
    private static final DatabaseConfig dbConfig = new DatabaseConfig();
    private static final MessageConfig msgConfig = new MessageConfig();
    private static final ClaimEventGenerator eventGenerator = new ClaimEventGenerator();

    public static void main(String[] args) {
        System.out.println("Starting Continuous Claim Event Producer...");
        System.out.println("Configuration: " + dbConfig);
        System.out.println("Press Ctrl+C to stop");

        OracleDataSource ds = createDataSource();
        if (ds == null) {
            return;
        }

        // Run continuously
        while (true) {
            try {
                List<ClaimEvent> claimEvents = eventGenerator.generateClaimEvents(
                        msgConfig.getNumberOfClaims(),
                        msgConfig.getEntriesPerClaim());

                long startTime = System.currentTimeMillis();
                publishClaimEvents(claimEvents, ds);
                long endTime = System.currentTimeMillis();

                System.out.println("Published " + claimEvents.size() + " messages in " + (endTime - startTime) + " ms");

                // Wait before next batch
                Thread.sleep(msgConfig.getProductionIntervalMs());
            } catch (Exception e) {
                System.err.println("Error in producer loop: " + e.getMessage());
                try {
                    Thread.sleep(10000); // Wait 10 seconds on error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
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
            System.err.println("Error creating data source: " + e.getMessage());
            return null;
        }
    }

    private static void publishClaimEvents(List<ClaimEvent> claimEvents, OracleDataSource ds) {
        try (Connection con = ds.getConnection()) {
            if (con != null) {
                System.out.println("Connected to the database successfully!");
            }

            TopicConnectionFactory tcf = AQjmsFactory.getTopicConnectionFactory(ds);
            try (TopicConnection conn = tcf.createTopicConnection()) {
                conn.start();
                try (AQjmsSession session = (AQjmsSession) conn.createSession(true, Session.AUTO_ACKNOWLEDGE)) {
                    Topic topic = session.getTopic(dbConfig.getUsername(), msgConfig.getTopicName());
                    try (AQjmsTopicPublisher publisher = (AQjmsTopicPublisher) session.createPublisher(topic)) {
                        for (ClaimEvent event : claimEvents) {
                            String jsonMessage = JsonUtils.toJson(event);
                            var message = session.createJsonMessage(jsonMessage);
                            // Set correlation ID to claim ID for ordering
                            message.setJMSCorrelationID(String.valueOf(event.getClaimId()));
                            publisher.publish(message);
                            System.out.println("Published message: " + jsonMessage);
                        }
                        session.commit();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
