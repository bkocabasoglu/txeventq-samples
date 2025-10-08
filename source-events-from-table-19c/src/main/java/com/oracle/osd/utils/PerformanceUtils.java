package com.oracle.osd.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for performance timing and logging.
 */
public class PerformanceUtils {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceUtils.class);

    /**
     * Tracks performance of an operation and logs the results.
     */
    public static class Timer {
        private final long startTime;
        private final String operationName;
        private final Logger operationLogger;

        public Timer(String operationName, Logger operationLogger) {
            this.startTime = System.currentTimeMillis();
            this.operationName = operationName;
            this.operationLogger = operationLogger;
            logger.debug("Started timer for operation: '{}'", operationName);
        }

        public void logCompletion() {
            long endTime = System.currentTimeMillis();
            long durationMillis = endTime - startTime;
            operationLogger.info("{} completed in {} seconds",
                operationName, String.format("%.2f", durationMillis / 1000.0));
            logger.debug("Operation '{}' completed - Duration: {} ms", operationName, durationMillis);
        }

        public long getDurationMillis() {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Current duration for '{}': {} ms", operationName, duration);
            return duration;
        }
    }

    /**
     * Simple method to format duration in seconds.
     */
    public static String formatDurationSeconds(long durationMillis) {
        logger.debug("Formatting duration: {} ms to seconds", durationMillis);
        String formatted = String.format("%.2f", durationMillis / 1000.0);
        logger.debug("Formatted duration: {} seconds", formatted);
        return formatted;
    }
}
