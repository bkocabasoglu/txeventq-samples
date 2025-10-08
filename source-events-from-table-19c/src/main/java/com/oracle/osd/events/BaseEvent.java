package com.oracle.osd.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for all event types with common fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseEvent {

    @JsonProperty("Action")
    private String action;

    @JsonProperty("Notes")
    private String notes;

    @JsonProperty("Timestamp")
    private String timestamp;

    // Constructors
    public BaseEvent() {
    }

    public BaseEvent(String action, String notes, String timestamp) {
        this.action = action;
        this.notes = notes;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("Action: %s, Notes: %s, Timestamp: %s", action, notes, timestamp);
    }
}
