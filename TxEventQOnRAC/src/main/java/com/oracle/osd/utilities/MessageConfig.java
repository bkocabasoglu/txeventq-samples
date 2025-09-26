package com.oracle.osd.utilities;

/**
 * Message configuration class for Oracle TxEventQ messaging.
 * Centralizes all messaging-related configuration settings.
 */
public class MessageConfig {
    
    // Topic and subscriber names
    private static final String DEFAULT_TOPIC_NAME = "ClaimUpdatesTopic";
    private static final String DEFAULT_SUBSCRIBER_NAME = "ClaimUpdatesSubscriber";
    
    // Producer settings
    private static final int DEFAULT_NUMBER_OF_CLAIMS = 4;
    private static final int DEFAULT_ENTRIES_PER_CLAIM = 2;
    private static final long DEFAULT_PRODUCTION_INTERVAL_MS = 6000;
    
    // Consumer settings
    private static final long DEFAULT_CONSUMER_TIMEOUT_MS = 1000; // 1 second
    
    // Environment variable keys
    private static final String TOPIC_NAME_ENV = "TOPIC_NAME";
    private static final String SUBSCRIBER_NAME_ENV = "SUBSCRIBER_NAME";
    private static final String NUMBER_OF_CLAIMS_ENV = "NUMBER_OF_CLAIMS";
    private static final String ENTRIES_PER_CLAIM_ENV = "ENTRIES_PER_CLAIM";
    private static final String PRODUCTION_INTERVAL_ENV = "PRODUCTION_INTERVAL_MS";
    private static final String CONSUMER_TIMEOUT_ENV = "CONSUMER_TIMEOUT_MS";
    
    private final String topicName;
    private final String subscriberName;
    private final int numberOfClaims;
    private final int entriesPerClaim;
    private final long productionIntervalMs;
    private final long consumerTimeoutMs;
    
    /**
     * Creates a MessageConfig with default values.
     */
    public MessageConfig() {
        this.topicName = getEnvOrDefault(TOPIC_NAME_ENV, DEFAULT_TOPIC_NAME);
        this.subscriberName = getEnvOrDefault(SUBSCRIBER_NAME_ENV, DEFAULT_SUBSCRIBER_NAME);
        this.numberOfClaims = getEnvOrDefaultInt(NUMBER_OF_CLAIMS_ENV, DEFAULT_NUMBER_OF_CLAIMS);
        this.entriesPerClaim = getEnvOrDefaultInt(ENTRIES_PER_CLAIM_ENV, DEFAULT_ENTRIES_PER_CLAIM);
        this.productionIntervalMs = getEnvOrDefaultLong(PRODUCTION_INTERVAL_ENV, DEFAULT_PRODUCTION_INTERVAL_MS);
        this.consumerTimeoutMs = getEnvOrDefaultLong(CONSUMER_TIMEOUT_ENV, DEFAULT_CONSUMER_TIMEOUT_MS);
    }
    
    /**
     * Gets the topic name for publishing messages.
     */
    public String getTopicName() {
        return topicName;
    }
    
    /**
     * Gets the subscriber name for consuming messages.
     */
    public String getSubscriberName() {
        return subscriberName;
    }
    
    /**
     * Gets the number of claims to generate per batch.
     */
    public int getNumberOfClaims() {
        return numberOfClaims;
    }
    
    /**
     * Gets the number of entries per claim.
     */
    public int getEntriesPerClaim() {
        return entriesPerClaim;
    }
    
    /**
     * Gets the production interval in milliseconds.
     */
    public long getProductionIntervalMs() {
        return productionIntervalMs;
    }
    
    /**
     * Gets the consumer timeout in milliseconds.
     */
    public long getConsumerTimeoutMs() {
        return consumerTimeoutMs;
    }
    
    /**
     * Gets environment variable value or returns default if not set.
     */
    private String getEnvOrDefault(String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : defaultValue;
    }
    
    /**
     * Gets environment variable value as integer or returns default if not set.
     */
    private int getEnvOrDefaultInt(String envKey, int defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid integer value for " + envKey + ": " + envValue + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    /**
     * Gets environment variable value as long or returns default if not set.
     */
    private long getEnvOrDefaultLong(String envKey, long defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            try {
                return Long.parseLong(envValue);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid long value for " + envKey + ": " + envValue + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    @Override
    public String toString() {
        return "MessageConfig{" +
                "topicName='" + topicName + '\'' +
                ", subscriberName='" + subscriberName + '\'' +
                ", numberOfClaims=" + numberOfClaims +
                ", entriesPerClaim=" + entriesPerClaim +
                ", productionIntervalMs=" + productionIntervalMs +
                ", consumerTimeoutMs=" + consumerTimeoutMs +
                '}';
    }
}
