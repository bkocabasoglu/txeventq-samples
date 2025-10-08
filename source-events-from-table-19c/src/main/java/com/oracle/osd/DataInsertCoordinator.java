package com.oracle.osd;

import com.oracle.osd.utils.PerformanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Coordinator class that orchestrates data insertion across all record types.
 * Accepts the number of records and calls all insert classes with that number.
 */
public class DataInsertCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(DataInsertCoordinator.class);
    private static final int DEFAULT_RECORDS = 1000;

    private final int recordCount;
    private final boolean parallelExecution;

    public DataInsertCoordinator(int recordCount) {
        this(recordCount, true);
    }

    public DataInsertCoordinator(int recordCount, boolean parallelExecution) {
        this.recordCount = recordCount;
        this.parallelExecution = parallelExecution;
    }

    public static void main(String[] args) {
        int recordCount = parseRecordCount(args);
        boolean parallel = shouldRunInParallel(args);

        DataInsertCoordinator coordinator = new DataInsertCoordinator(recordCount, parallel);
        coordinator.insertAllRecords();
    }

    private static int parseRecordCount(String[] args) {
        if (args.length == 0) {
            System.out.printf("Enter number of records to create for each type (default %d): ", DEFAULT_RECORDS);
            try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
                String input = scanner.nextLine().trim();
                return input.isEmpty() ? DEFAULT_RECORDS : Integer.parseInt(input);
            } catch (Exception e) {
                logger.warn("Invalid input, using default: {}", DEFAULT_RECORDS);
                return DEFAULT_RECORDS;
            }
        }

        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            logger.warn("Invalid record count '{}', using default: {}", args[0], DEFAULT_RECORDS);
            return DEFAULT_RECORDS;
        }
    }

    private static boolean shouldRunInParallel(String[] args) {
        if (args.length > 1) {
            return "parallel".equalsIgnoreCase(args[1]) || "true".equalsIgnoreCase(args[1]);
        }
        return true; // default to parallel execution
    }

    /**
     * Inserts records for all data types using the specified record count.
     * Can run insertions sequentially or in parallel based on configuration.
     */
    public void insertAllRecords() {
        logger.info("Starting coordinated data insertion for {} records per type", recordCount);
        logger.info("Execution mode: {}", parallelExecution ? "parallel" : "sequential");

        PerformanceUtils.Timer totalTimer = new PerformanceUtils.Timer("Total data insertion", logger);

        if (parallelExecution) {
            insertRecordsInParallel();
        } else {
            insertRecordsSequentially();
        }

        long totalTimeMs = totalTimer.getDurationMillis();
        logger.info("All data insertion completed in {} ms", totalTimeMs);
        logger.info("Total records inserted: {} (across 4 types)", recordCount * 4);
    }

    private void insertRecordsInParallel() {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            CompletableFuture<Void> actionsFuture = CompletableFuture.runAsync(() -> {
                logger.info("Starting action records insertion...");
                InsertActionRecords.main(new String[]{String.valueOf(recordCount)});
                logger.info("Action records insertion completed");
            }, executor);

            CompletableFuture<Void> paymentsFuture = CompletableFuture.runAsync(() -> {
                logger.info("Starting payment records insertion...");
                InsertPaymentRecords.main(new String[]{String.valueOf(recordCount)});
                logger.info("Payment records insertion completed");
            }, executor);

            CompletableFuture<Void> statementsFuture = CompletableFuture.runAsync(() -> {
                logger.info("Starting statement records insertion...");
                InsertStatementRecords.main(new String[]{String.valueOf(recordCount)});
                logger.info("Statement records insertion completed");
            }, executor);

            CompletableFuture<Void> transactionsFuture = CompletableFuture.runAsync(() -> {
                logger.info("Starting transaction records insertion...");
                InsertTransactionRecords.main(new String[]{String.valueOf(recordCount)});
                logger.info("Transaction records insertion completed");
            }, executor);

            // Wait for all insertions to complete
            CompletableFuture.allOf(actionsFuture, paymentsFuture, statementsFuture, transactionsFuture)
                    .join();

        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void insertRecordsSequentially() {
        logger.info("Starting action records insertion...");
        InsertActionRecords.main(new String[]{String.valueOf(recordCount)});
        logger.info("Action records insertion completed");

        logger.info("Starting payment records insertion...");
        InsertPaymentRecords.main(new String[]{String.valueOf(recordCount)});
        logger.info("Payment records insertion completed");

        logger.info("Starting statement records insertion...");
        InsertStatementRecords.main(new String[]{String.valueOf(recordCount)});
        logger.info("Statement records insertion completed");

        logger.info("Starting transaction records insertion...");
        InsertTransactionRecords.main(new String[]{String.valueOf(recordCount)});
        logger.info("Transaction records insertion completed");
    }

    /**
     * Get the configured record count for this coordinator.
     * @return the number of records to insert for each type
     */
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * Check if parallel execution is enabled.
     * @return true if insertions run in parallel, false for sequential
     */
    public boolean isParallelExecution() {
        return parallelExecution;
    }
}
