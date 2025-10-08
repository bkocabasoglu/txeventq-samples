package com.oracle.osd.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Statement event DTO for TransactionStatementTopic messages.
 * Handles both StatementsId and TransactionId fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatementEvent extends BaseEvent {

    @JsonProperty("StatementsId")
    private String statementsId;

    @JsonProperty("TransactionId")
    private String transactionId;

    // Constructors
    public StatementEvent() {
    }

    public StatementEvent(String statementsId, String action, String notes, String timestamp) {
        super(action, notes, timestamp);
        this.statementsId = statementsId;
    }

    // Getters and setters
    public String getStatementsId() {
        return statementsId;
    }

    public void setStatementsId(String statementsId) {
        this.statementsId = statementsId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Get the primary ID field, checking both StatementsId and TransactionId.
     *
     * @return The primary ID value
     */
    public String getPrimaryId() {
        if (statementsId != null && !statementsId.trim().isEmpty()) {
            return statementsId;
        }
        return transactionId;
    }

    /**
     * Get the ID field name that is being used.
     *
     * @return The field name
     */
    public String getIdFieldName() {
        if (statementsId != null && !statementsId.trim().isEmpty()) {
            return "StatementsId";
        }
        return "TransactionId";
    }

    @Override
    public String toString() {
        String primaryId = getPrimaryId();
        String fieldName = getIdFieldName();
        return String.format("StatementEvent{%s='%s', %s}", fieldName, primaryId, super.toString());
    }
}
