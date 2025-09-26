package com.oracle.osd.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class ClaimEvent {
    @JsonProperty("eventId")
    private final String eventId;
    
    @JsonProperty("claimId")
    private final int claimId;
    
    @JsonProperty("entryNumber")
    private final int entryNumber;
    
    @JsonProperty("claimType")
    private final ClaimType claimType;
    
    @JsonProperty("status")
    private final ClaimStatus status;
    
    @JsonProperty("entryStatus")
    private final EntryStatus entryStatus;
    
    @JsonProperty("amount")
    private final double amount;
    
    @JsonProperty("timestamp")
    private final long timestamp;

    // Enum types for better type safety and performance
    public enum ClaimType {
        LIFE, HOME, AUTO, TRAVEL, HEALTH
    }
    
    public enum ClaimStatus {
        PENDING, IN_REVIEW, APPROVED, REJECTED, CANCELLED
    }
    
    public enum EntryStatus {
        PENDING, PROCESSED, FAILED, CANCELLED
    }

    // JsonCreator constructor for Jackson deserialization
    @JsonCreator
    public ClaimEvent(@JsonProperty("eventId") String eventId,
                     @JsonProperty("claimId") int claimId,
                     @JsonProperty("entryNumber") int entryNumber,
                     @JsonProperty("claimType") ClaimType claimType,
                     @JsonProperty("status") ClaimStatus status,
                     @JsonProperty("entryStatus") EntryStatus entryStatus,
                     @JsonProperty("amount") double amount,
                     @JsonProperty("timestamp") long timestamp) {
        this.eventId = Objects.requireNonNull(eventId, "eventId cannot be null");
        this.claimId = claimId;
        this.entryNumber = entryNumber;
        this.claimType = Objects.requireNonNull(claimType, "claimType cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.entryStatus = Objects.requireNonNull(entryStatus, "entryStatus cannot be null");
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Getters (immutable object - no setters)
    public String getEventId() {
        return eventId;
    }

    public int getClaimId() {
        return claimId;
    }

    public int getEntryNumber() {
        return entryNumber;
    }

    public ClaimType getClaimType() {
        return claimType;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public EntryStatus getEntryStatus() {
        return entryStatus;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClaimEvent that = (ClaimEvent) o;
        return claimId == that.claimId &&
               entryNumber == that.entryNumber &&
               Double.compare(that.amount, amount) == 0 &&
               timestamp == that.timestamp &&
               Objects.equals(eventId, that.eventId) &&
               claimType == that.claimType &&
               status == that.status &&
               entryStatus == that.entryStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, claimId, entryNumber, claimType, status, entryStatus, amount, timestamp);
    }

    @Override
    public String toString() {
        return "  Event ID: " + eventId + "\n" +
                "  Claim ID: " + claimId + "\n" +
                "  Entry #: " + entryNumber + "\n" +
                "  Type: " + claimType + "\n" +
                "  Status: " + status + "\n" +
                "  Entry Status: " + entryStatus + "\n" +
                "  Amount: $" + String.format("%.2f", amount) + "\n" +
                "  Timestamp: " + new java.util.Date(timestamp);
    }
}
