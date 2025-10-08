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

public class InsertTransactionRecords {

    private static final Logger logger = LoggerFactory.getLogger(InsertTransactionRecords.class);
    private static final int BULK_SIZE = 5000;
    private static final int DEFAULT_RECORDS = 200;
    private static final int MAX_TRANSACTION_ID = 1_000_000;
    private static final int ENTRIES_PER_TRANSACTION = 5;

    public static void main(String[] args) {
        int totalRecords = parseRecordCount(args);
        OracleDataSource dataSource = DatabaseUtils.createDataSource();
        List<TransactionEntry> entries = prepareData(totalRecords);
        insertTransactionEntries(entries, dataSource);
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

    private static List<TransactionEntry> prepareData(int totalRecords) {
        logger.debug("Starting data preparation for {} records", totalRecords);
        List<TransactionEntry> entries = new ArrayList<>();

        int numberOfTransactions = Math.max(1, totalRecords / ENTRIES_PER_TRANSACTION);
        if (numberOfTransactions * ENTRIES_PER_TRANSACTION < totalRecords) {
            numberOfTransactions = (int) Math.ceil((double) totalRecords / ENTRIES_PER_TRANSACTION);
        }

        logger.info("Creating {} records across {} transactions ({} entries per transaction on average)",
                totalRecords, numberOfTransactions, ENTRIES_PER_TRANSACTION);
        logger.debug("Calculated {} transactions needed for {} total records", numberOfTransactions, totalRecords);

        List<String> transactionIds = DataGenerationUtils.generateTransactionIds(numberOfTransactions, MAX_TRANSACTION_ID);
        logger.debug("Generated {} unique transaction IDs", transactionIds.size());

        int recordsCreated = 0;
        for (String transactionId : transactionIds) {
            for (int j = 1; j <= ENTRIES_PER_TRANSACTION && recordsCreated < totalRecords; j++) {
                entries.add(new TransactionEntry(transactionId, j));
                recordsCreated++;
            }
            if (recordsCreated >= totalRecords) {
                logger.debug("Reached target record count of {}, breaking loop", totalRecords);
                break;
            }
        }

        logger.info("Prepared {} transaction entries", entries.size());
        logger.debug("Data preparation completed successfully");
        return entries;
    }

    private static void insertTransactionEntries(List<TransactionEntry> entries, OracleDataSource dataSource) {
        PerformanceUtils.Timer timer = new PerformanceUtils.Timer("Transaction entries insertion", logger);
        logger.info("Starting to insert {} transaction entries in batches of {}", entries.size(), BULK_SIZE);
        logger.debug("Using bulk size of {} for batch processing", BULK_SIZE);

        try {
            String insertQuery = "INSERT INTO TransactionUpdatesTable (TransactionId, Notes, Timestamp) VALUES (?, ?, ?)";
            logger.debug("Using SQL query: {}", insertQuery);
            DatabaseUtils.processRecordsInBatches(dataSource, entries, insertQuery, BULK_SIZE,
                    InsertTransactionRecords::addEntryToBatch);
            logger.debug("Batch processing completed successfully");
        } finally {
            timer.logCompletion();
        }
    }

    private static void addEntryToBatch(PreparedStatement pstmt, TransactionEntry entry) throws SQLException {
        long currentTimestamp = System.currentTimeMillis();

        String message = DataGenerationUtils.createTimestampedMessage(
                "Transaction entry: {0}, Timestamp: {timestamp}, for transaction: {1}",
                currentTimestamp,
                entry.entryNumber(),
                entry.transactionId()
        );

        pstmt.setString(1, entry.transactionId());
        pstmt.setString(2, message);
        pstmt.setLong(3, currentTimestamp);
        pstmt.addBatch();
    }

    public record TransactionEntry(String transactionId, int entryNumber) {
    }
}
