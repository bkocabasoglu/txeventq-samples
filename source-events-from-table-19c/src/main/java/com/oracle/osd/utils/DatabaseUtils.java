package com.oracle.osd.utils;

import oracle.jdbc.pool.OracleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class DatabaseUtils {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);

    private static final String URL = "replace_me_url";
    private static final String USERNAME = "replace_me_username";
    private static final String PASSWORD = "replace_me_password";

    /**
     * Creates and returns an Oracle DataSource with the configured connection parameters.
     *
     * @return OracleDataSource configured with connection details
     * @throws RuntimeException if there's an error creating the data source
     */
    public static OracleDataSource createDataSource() {
        try {
            OracleDataSource ds = new OracleDataSource();
            ds.setURL(URL);
            ds.setUser(USERNAME);
            ds.setPassword(PASSWORD);
            logger.debug("DataSource created with URL: {}", URL);
            return ds;
        } catch (SQLException e) {
            logger.error("Failed to create Oracle DataSource", e);
            throw new RuntimeException("Failed to create Oracle DataSource", e);
        }
    }

    /**
     * Creates and returns an Oracle DataSource with custom URL.
     *
     * @param url Custom database URL
     * @return OracleDataSource configured with custom URL and default credentials
     * @throws RuntimeException if there's an error creating the data source
     */
    public static OracleDataSource createDataSource(String url) {
        try {
            OracleDataSource ds = new OracleDataSource();
            ds.setURL(url);
            ds.setUser(USERNAME);
            ds.setPassword(PASSWORD);
            logger.debug("DataSource created with custom URL: {}", url);
            return ds;
        } catch (SQLException e) {
            logger.error("Failed to create Oracle DataSource with custom URL: {}", url, e);
            throw new RuntimeException("Failed to create Oracle DataSource with custom URL", e);
        }
    }

    /**
     * Processes a list of records in batches using the provided batch processor.
     *
     * @param <T> The type of records to process
     * @param ds The data source
     * @param records List of records to process
     * @param insertQuery The SQL insert query
     * @param bulkSize The batch size
     * @param batchProcessor Function to add a single record to the batch
     * @throws RuntimeException if there's an error processing the batch
     */
    public static <T> void processRecordsInBatches(OracleDataSource ds, List<T> records, String insertQuery,
                                                   int bulkSize, BatchProcessor<T> batchProcessor) {
        try (Connection con = ds.getConnection();
             PreparedStatement pstmt = con.prepareStatement(insertQuery)) {

            con.setAutoCommit(false);
            logger.debug("Database connection established for batch of {} records", records.size());

            processBatch(con, pstmt, records, bulkSize, batchProcessor);

        } catch (SQLException e) {
            logger.error("Error processing records in database", e);
            throw new RuntimeException("Failed to process records", e);
        }
    }

    /**
     * Internal method to process batches with error handling and performance tracking.
     */
    private static <T> void processBatch(Connection con, PreparedStatement pstmt, List<T> records,
                                        int bulkSize, BatchProcessor<T> batchProcessor) throws SQLException {
        long batchStartTime = System.currentTimeMillis();
        int count = 0;

        try {
            logger.debug("Starting batch processing for {} records with bulk size {}", records.size(), bulkSize);

            for (T record : records) {
                batchProcessor.addToBatch(pstmt, record);
                count++;

                if (count % bulkSize == 0) {
                    executeBatch(con, pstmt, bulkSize);
                }
            }

            // Execute remaining records
            if (count % bulkSize != 0) {
                int remainingRecords = count % bulkSize;
                logger.debug("Processing remaining {} records", remainingRecords);
                executeBatch(con, pstmt, remainingRecords);
            }

        } catch (SQLException e) {
            con.rollback();
            logger.warn("Transaction rolled back due to an error after processing {} records", count);
            throw e;
        } finally {
            logBatchPerformance(records.size(), batchStartTime);
        }
    }

    /**
     * Executes a batch and commits the transaction with debug logging.
     */
    private static void executeBatch(Connection con, PreparedStatement pstmt, int batchSize) throws SQLException {
        long startTime = System.currentTimeMillis();
        logger.debug("Executing batch of {} records", batchSize);

        int[] results = pstmt.executeBatch();
        con.commit();

        long endTime = System.currentTimeMillis();
        logger.debug("Batch execution completed in {} ms, {} records processed",
                    (endTime - startTime), results.length);

        pstmt.clearBatch();
    }

    /**
     * Logs batch processing performance metrics.
     */
    private static void logBatchPerformance(int totalRecords, long startTime) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double recordsPerSecond = totalRecords / (duration / 1000.0);

        logger.info("Batch processing completed: {} records in {} ms ({} records/sec)",
                   totalRecords, duration, String.format("%.2f", recordsPerSecond));
        logger.debug("Performance details - Total: {}, Duration: {} ms, Rate: {} rec/sec",
                    totalRecords, duration, String.format("%.2f", recordsPerSecond));
    }

    /**
     * Functional interface for processing individual records in a batch.
     *
     * @param <T> The type of record to process
     */
    @FunctionalInterface
    public interface BatchProcessor<T> {
        /**
         * Adds a single record to the prepared statement batch.
         *
         * @param pstmt The prepared statement
         * @param record The record to add
         * @throws SQLException if there's an error adding the record
         */
        void addToBatch(PreparedStatement pstmt, T record) throws SQLException;
    }
}
