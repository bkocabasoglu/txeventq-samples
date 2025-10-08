package com.oracle.osd;

import com.oracle.osd.utils.DataGenerationUtils;
import com.oracle.osd.utils.DatabaseUtils;
import com.oracle.osd.utils.PerformanceUtils;
import oracle.jdbc.pool.OracleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InsertPaymentRecords {

    private static final Logger logger = LoggerFactory.getLogger(InsertPaymentRecords.class);
    private static final int BULK_SIZE = 5000;
    private static final int DEFAULT_RECORDS = 200;
    private static final int MAX_PAYMENT_ID = 1_000_000;
    private static final int ENTRIES_PER_PAYMENT = 5;

    public static void main(String[] args) {
        int totalRecords = parseRecordCount(args);
        OracleDataSource dataSource = DatabaseUtils.createDataSource();
        List<PaymentEntry> entries = prepareData(totalRecords);
        insertPaymentEntries(entries, dataSource);
    }

    private static int parseRecordCount(String[] args) {
        if (args.length == 0) {
            System.out.printf("Enter number of records to create (default %d): ", DEFAULT_RECORDS);
            try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
                String input = scanner.nextLine().trim();
                return input.isEmpty() ? DEFAULT_RECORDS : Integer.parseInt(input);
            } catch (Exception e) {
                return DEFAULT_RECORDS;
            }
        }
        return Integer.parseInt(args[0]);
    }

    private static List<PaymentEntry> prepareData(int totalRecords) {
        logger.debug("Starting data preparation for {} payment records", totalRecords);
        List<PaymentEntry> entries = new ArrayList<>();

        int numberOfPayments = Math.max(1, totalRecords / ENTRIES_PER_PAYMENT);
        if (numberOfPayments * ENTRIES_PER_PAYMENT < totalRecords) {
            numberOfPayments = (int) Math.ceil((double) totalRecords / ENTRIES_PER_PAYMENT);
        }

        logger.info("Creating {} records across {} payments ({} entries per payment on average)",
                totalRecords, numberOfPayments, ENTRIES_PER_PAYMENT);
        logger.debug("Calculated {} payments needed for {} total records", numberOfPayments, totalRecords);

        List<String> paymentIds = DataGenerationUtils.generatePaymentIds(numberOfPayments, MAX_PAYMENT_ID);
        logger.debug("Generated {} unique payment IDs", paymentIds.size());

        int recordsCreated = 0;
        for (String paymentId : paymentIds) {
            for (int j = 1; j <= ENTRIES_PER_PAYMENT && recordsCreated < totalRecords; j++) {
                entries.add(new PaymentEntry(paymentId, j));
                recordsCreated++;
            }
            if (recordsCreated >= totalRecords) {
                logger.debug("Reached target record count of {}, breaking loop", totalRecords);
                break;
            }
        }

        logger.info("Prepared {} payment entries", entries.size());
        logger.debug("Payment data preparation completed successfully");
        return entries;
    }

    private static void insertPaymentEntries(List<PaymentEntry> entries, OracleDataSource dataSource) {
        PerformanceUtils.Timer timer = new PerformanceUtils.Timer("Payment entries insertion", logger);
        logger.info("Starting to insert {} payment entries in batches of {}", entries.size(), BULK_SIZE);
        logger.debug("Using bulk size of {} for payment batch processing", BULK_SIZE);

        try {
            String insertQuery = "INSERT INTO PaymentUpdatesTable (PaymentId, Notes, Timestamp) VALUES (?, ?, ?)";
            logger.debug("Using SQL query: {}", insertQuery);
            DatabaseUtils.processRecordsInBatches(dataSource, entries, insertQuery, BULK_SIZE,
                    InsertPaymentRecords::addEntryToBatch);
            logger.debug("Payment batch processing completed successfully");
        } finally {
            timer.logCompletion();
        }
    }

    private static void addEntryToBatch(PreparedStatement pstmt, PaymentEntry entry) throws SQLException {
        long currentTimestamp = System.currentTimeMillis();

        String message = DataGenerationUtils.createTimestampedMessage(
                "Payment entry: {0}, Timestamp: {timestamp}, for payment: {1}",
                currentTimestamp,
                entry.entryNumber(),
                entry.paymentId()
        );

        pstmt.setString(1, entry.paymentId());
        pstmt.setString(2, message);
        pstmt.setLong(3, currentTimestamp);
        pstmt.addBatch();
    }

    public record PaymentEntry(String paymentId, int entryNumber) {
    }
}
