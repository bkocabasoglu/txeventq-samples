package com.oracle.osd.utilities;

/**
 * Database configuration class for Oracle TxEventQ connections.
 * Centralizes all database-related configuration settings.
 */
public class DatabaseConfig {

    // Database connection settings
    private static final String DEFAULT_URL = "jdbc:oracle:thin:@localhost:1522/FREEPDB1";
    private static final String DEFAULT_USERNAME = "txeventq_user";
    private static final String DEFAULT_PASSWORD = "pass123";

    // Environment variable keys
    private static final String DB_URL_ENV = "DB_URL";
    private static final String DB_USER_ENV = "DB_USER";
    private static final String DB_PASSWORD_ENV = "DB_PASSWORD";

    private final String url;
    private final String username;
    private final String password;

    /**
     * Creates a DatabaseConfig with default values.
     */
    public DatabaseConfig() {
        this.url = getEnvOrDefault(DB_URL_ENV, DEFAULT_URL);
        this.username = getEnvOrDefault(DB_USER_ENV, DEFAULT_USERNAME);
        this.password = getEnvOrDefault(DB_PASSWORD_ENV, DEFAULT_PASSWORD);
    }

    /**
     * Creates a DatabaseConfig with custom values.
     */
    public DatabaseConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the database URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the database username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the database password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets environment variable value or returns default if not set.
     */
    private String getEnvOrDefault(String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : defaultValue;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='[HIDDEN]'" +
                '}';
    }
}
