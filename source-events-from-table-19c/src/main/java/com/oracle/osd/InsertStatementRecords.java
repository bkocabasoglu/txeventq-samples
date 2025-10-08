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

public class InsertStatementRecords {

    private static final Logger logger = LoggerFactory.getLogger(InsertStatementRecords.class);
    private static final int BULK_SIZE = 5000;
    private static final int DEFAULT_RECORDS = 200;
    private static final int MAX_STATEMENT_ID = 1_000_000;
    private static final int ENTRIES_PER_STATEMENT = 5;

    public static void main(String[] args) {
        int totalRecords = parseRecordCount(args);
        OracleDataSource dataSource = DatabaseUtils.createDataSource();
        List<StatementEntry> entries = prepareData(totalRecords);
        insertStatementEntries(entries, dataSource);
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

    private static List<StatementEntry> prepareData(int totalRecords) {
        logger.debug("Starting data preparation for {} statement records", totalRecords);
        List<StatementEntry> entries = new ArrayList<>();

        int numberOfStatements = Math.max(1, totalRecords / ENTRIES_PER_STATEMENT);
        if (numberOfStatements * ENTRIES_PER_STATEMENT < totalRecords) {
            numberOfStatements = (int) Math.ceil((double) totalRecords / ENTRIES_PER_STATEMENT);
        }

        logger.info("Creating {} records across {} statements ({} entries per statement on average)",
                totalRecords, numberOfStatements, ENTRIES_PER_STATEMENT);
        logger.debug("Calculated {} statements needed for {} total records", numberOfStatements, totalRecords);

        List<String> statementIds = DataGenerationUtils.generateStatementIds(numberOfStatements, MAX_STATEMENT_ID);
        logger.debug("Generated {} unique statement IDs", statementIds.size());

        int recordsCreated = 0;
        for (String statementId : statementIds) {
            for (int j = 1; j <= ENTRIES_PER_STATEMENT && recordsCreated < totalRecords; j++) {
                entries.add(new StatementEntry(statementId, j));
                recordsCreated++;
            }
            if (recordsCreated >= totalRecords) {
                logger.debug("Reached target record count of {}, breaking loop", totalRecords);
                break;
            }
        }

        logger.info("Prepared {} statement entries", entries.size());
        logger.debug("Statement data preparation completed successfully");
        return entries;
    }

    private static void insertStatementEntries(List<StatementEntry> entries, OracleDataSource dataSource) {
        PerformanceUtils.Timer timer = new PerformanceUtils.Timer("Statement entries insertion", logger);
        logger.info("Starting to insert {} statement entries in batches of {}", entries.size(), BULK_SIZE);
        logger.debug("Using bulk size of {} for statement batch processing", BULK_SIZE);

        try {
            String insertQuery = "INSERT INTO StatementsUpdatesTable (StatementsId, Notes, Timestamp) VALUES (?, ?, ?)";
            logger.debug("Using SQL query: {}", insertQuery);
            DatabaseUtils.processRecordsInBatches(dataSource, entries, insertQuery, BULK_SIZE,
                    InsertStatementRecords::addEntryToBatch);
            logger.debug("Statement batch processing completed successfully");
        } finally {
            timer.logCompletion();
        }
    }

    private static void addEntryToBatch(PreparedStatement pstmt, StatementEntry entry) throws SQLException {
        long currentTimestamp = System.currentTimeMillis();

        String message = DataGenerationUtils.createTimestampedMessage(
                "Statement entry: {0}, Timestamp: {timestamp}, for statement: {1}",
                currentTimestamp,
                entry.entryNumber(),
                entry.statementsId()
        );

        pstmt.setString(1, entry.statementsId());
        pstmt.setString(2, message);
        pstmt.setLong(3, currentTimestamp);
        pstmt.addBatch();
    }

    public record StatementEntry(String statementsId, int entryNumber) {
    }
}
