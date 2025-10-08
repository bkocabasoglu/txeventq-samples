package com.oracle.osd.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Utility class for generating test data.
 */
public class DataGenerationUtils {

    private static final Logger logger = LoggerFactory.getLogger(DataGenerationUtils.class);
    private static final Random random = new Random();

    /**
     * Generates a list of random IDs with a given prefix.
     *
     * @param prefix The prefix for each ID (e.g., "STMT-", "TXN-")
     * @param count Number of IDs to generate
     * @param maxValue Maximum random number value
     * @param shuffle Whether to shuffle the generated IDs
     * @return List of generated IDs
     */
    public static List<String> generateRandomIds(String prefix, int count, int maxValue, boolean shuffle) {
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int randomValue = random.nextInt(maxValue);
            String id = prefix + randomValue;
            ids.add(id);
        }

        if (shuffle) {
            Collections.shuffle(ids);
        }

        // Single informative debug line showing what was created
        if (logger.isDebugEnabled() && !ids.isEmpty()) {
            logger.debug("Generated {} {} IDs (e.g., '{}' to '{}')",
                        ids.size(), prefix.replace("-", ""), ids.get(0), ids.get(ids.size() - 1));
        }

        return ids;
    }

    /**
     * Generates random statement IDs.
     */
    public static List<String> generateStatementIds(int count, int maxValue) {
        return generateRandomIds("STMT-", count, maxValue, true);
    }

    /**
     * Generates random payment IDs.
     */
    public static List<String> generatePaymentIds(int count, int maxValue) {
        return generateRandomIds("PAY-", count, maxValue, true);
    }

    /**
     * Generates random action IDs.
     */
    public static List<String> generateActionIds(int count, int maxValue) {
        return generateRandomIds("ACT-", count, maxValue, true);
    }

    /**
     * Generates random transaction IDs.
     */
    public static List<String> generateTransactionIds(int count, int maxValue) {
        return generateRandomIds("TXN-", count, maxValue, true);
    }

    /**
     * Creates a timestamped message with custom format.
     *
     * @param template Template string with placeholders
     * @param timestamp The timestamp to include
     * @param values Values to replace in template
     * @return Formatted message
     */
    public static String createTimestampedMessage(String template, long timestamp, Object... values) {
        String message = template;
        for (int i = 0; i < values.length; i++) {
            String placeholder = "{" + i + "}";
            String replacement = String.valueOf(values[i]);
            message = message.replace(placeholder, replacement);
        }

        String finalMessage = message.replace("{timestamp}", String.valueOf(timestamp));

        if (logger.isDebugEnabled()) {
            String preview = finalMessage.length() > 80 ?
                           finalMessage.substring(0, 80) + "..." : finalMessage;
            logger.debug("Created message: '{}'", preview);
        }

        return finalMessage;
    }
}
