package com.oracle.osd.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility class for JSON operations.
 * Provides centralized JSON serialization and deserialization functionality.
 */
public class JsonUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // Configure ObjectMapper for better readability
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Serializes an object to JSON string.
     * 
     * @param object the object to serialize
     * @return JSON string representation
     * @throws JsonProcessingException if serialization fails
     */
    public static String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }
    
    /**
     * Deserializes a JSON string to an object of the specified class.
     * 
     * @param json the JSON string to deserialize
     * @param clazz the target class
     * @param <T> the type of the target class
     * @return deserialized object
     * @throws JsonProcessingException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(json, clazz);
    }
    
    /**
     * Safely serializes an object to JSON string.
     * Returns a default message if serialization fails.
     * 
     * @param object the object to serialize
     * @return JSON string or error message
     */
    public static String toJsonSafe(Object object) {
        try {
            return toJson(object);
        } catch (JsonProcessingException e) {
            return "Error serializing object: " + e.getMessage();
        }
    }
    
    /**
     * Safely deserializes a JSON string to an object.
     * Returns null if deserialization fails.
     * 
     * @param json the JSON string to deserialize
     * @param clazz the target class
     * @param <T> the type of the target class
     * @return deserialized object or null if failed
     */
    public static <T> T fromJsonSafe(String json, Class<T> clazz) {
        try {
            return fromJson(json, clazz);
        } catch (JsonProcessingException e) {
            System.err.println("Error deserializing JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the configured ObjectMapper instance.
     * 
     * @return the ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
