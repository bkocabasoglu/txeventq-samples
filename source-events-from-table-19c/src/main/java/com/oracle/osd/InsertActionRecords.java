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

public class InsertActionRecords {

    private static final Logger logger = LoggerFactory.getLogger(InsertActionRecords.class);
    private static final int BULK_SIZE = 5000;
    private static final int DEFAULT_RECORDS = 200;
    private static final int MAX_ACTION_ID = 1_000_000;
    private static final int ENTRIES_PER_ACTION = 5;

    public static void main(String[] args) {
        int totalRecords = parseRecordCount(args);
        OracleDataSource dataSource = DatabaseUtils.createDataSource();
        List<ActionEntry> entries = prepareData(totalRecords);
        insertActionEntries(entries, dataSource);
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

    private static List<ActionEntry> prepareData(int totalRecords) {
        logger.debug("Starting data preparation for {} action records", totalRecords);
        List<ActionEntry> entries = new ArrayList<>();

        int numberOfActions = Math.max(1, totalRecords / ENTRIES_PER_ACTION);
        if (numberOfActions * ENTRIES_PER_ACTION < totalRecords) {
            numberOfActions = (int) Math.ceil((double) totalRecords / ENTRIES_PER_ACTION);
        }

        logger.info("Creating {} records across {} actions ({} entries per action on average)",
                totalRecords, numberOfActions, ENTRIES_PER_ACTION);
        logger.debug("Calculated {} actions needed for {} total records", numberOfActions, totalRecords);

        List<String> actionIds = DataGenerationUtils.generateActionIds(numberOfActions, MAX_ACTION_ID);
        logger.debug("Generated {} unique action IDs", actionIds.size());

        int recordsCreated = 0;
        for (String actionId : actionIds) {
            for (int j = 1; j <= ENTRIES_PER_ACTION && recordsCreated < totalRecords; j++) {
                entries.add(new ActionEntry(actionId, j));
                recordsCreated++;
            }
            if (recordsCreated >= totalRecords) {
                logger.debug("Reached target record count of {}, breaking loop", totalRecords);
                break;
            }
        }

        logger.info("Prepared {} action entries", entries.size());
        logger.debug("Action data preparation completed successfully");
        return entries;
    }

    private static void insertActionEntries(List<ActionEntry> entries, OracleDataSource dataSource) {
        PerformanceUtils.Timer timer = new PerformanceUtils.Timer("Action entries insertion", logger);
        logger.info("Starting to insert {} action entries in batches of {}", entries.size(), BULK_SIZE);
        logger.debug("Using bulk size of {} for action batch processing", BULK_SIZE);

        try {
            String insertQuery = "INSERT INTO ActionUpdatesTable (ActionId, Notes, Timestamp) VALUES (?, ?, ?)";
            logger.debug("Using SQL query: {}", insertQuery);
            DatabaseUtils.processRecordsInBatches(dataSource, entries, insertQuery, BULK_SIZE,
                    InsertActionRecords::addEntryToBatch);
            logger.debug("Action batch processing completed successfully");
        } finally {
            timer.logCompletion();
        }
    }

    private static void addEntryToBatch(PreparedStatement pstmt, ActionEntry entry) throws SQLException {
        long currentTimestamp = System.currentTimeMillis();

        String message = DataGenerationUtils.createTimestampedMessage(
                "Action entry: {0}, Timestamp: {timestamp}, for action: {1}",
                currentTimestamp,
                entry.entryNumber(),
                entry.actionId()
        );

        pstmt.setString(1, entry.actionId());
        pstmt.setString(2, message);
        pstmt.setLong(3, currentTimestamp);
        pstmt.addBatch();
    }

    public record ActionEntry(String actionId, int entryNumber) {
    }
}
