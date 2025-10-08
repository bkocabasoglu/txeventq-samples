package com.oracle.osd.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.osd.events.ActionEvent;
import com.oracle.osd.events.PaymentEvent;
import com.oracle.osd.events.StatementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles message parsing and routing to appropriate event processors.
 * Provides robust JSON parsing and message validation.
 */
public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<String, EventProcessor<?>> processors;

    public MessageHandler() {
        this.objectMapper = new ObjectMapper();
        this.processors = new HashMap<>();

        // Register event processors
        processors.put("ACTION", new ActionEventProcessor());
        processors.put("PAYMENT", new PaymentEventProcessor());
        processors.put("STATEMENT", new StatementEventProcessor());
    }

    /**
     * Process a message by parsing JSON and routing to the appropriate processor.
     *
     * @param messageText    The JSON message content
     * @param eventType      The type of event (ACTION, PAYMENT, STATEMENT)
     * @param subscriberName The subscriber that received the message
     * @return true if message was processed successfully, false otherwise
     */
    public boolean processMessage(String messageText, String eventType, String subscriberName) {
        if (!isValidJson(messageText)) {
            logger.warn("Invalid JSON format for {} event from subscriber {}: {}", eventType, subscriberName, messageText);
            return false;
        }

        try {
            switch (eventType) {
                case "ACTION":
                    return processActionEvent(messageText, subscriberName);
                case "PAYMENT":
                    return processPaymentEvent(messageText, subscriberName);
                case "STATEMENT":
                    return processStatementEvent(messageText, subscriberName);
                default:
                    logger.warn("Unknown event type: {} from subscriber {}", eventType, subscriberName);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error processing {} event from subscriber {}: {}", eventType, subscriberName, e.getMessage());
            logger.error("Malformed JSON rejected: {}", messageText);
            return false;
        }
    }

    private boolean processActionEvent(String messageText, String subscriberName) {
        try {
            ActionEvent event = objectMapper.readValue(messageText, ActionEvent.class);
            if (isValidActionEvent(event)) {
                ActionEventProcessor processor = (ActionEventProcessor) processors.get("ACTION");
                processor.processEvent(event, subscriberName);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to parse ACTION event: {}", e.getMessage());
            return false;
        }
    }

    private boolean processPaymentEvent(String messageText, String subscriberName) {
        try {
            PaymentEvent event = objectMapper.readValue(messageText, PaymentEvent.class);
            if (isValidPaymentEvent(event)) {
                PaymentEventProcessor processor = (PaymentEventProcessor) processors.get("PAYMENT");
                processor.processEvent(event, subscriberName);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to parse PAYMENT event: {}", e.getMessage());
            return false;
        }
    }

    private boolean processStatementEvent(String messageText, String subscriberName) {
        try {
            StatementEvent event = objectMapper.readValue(messageText, StatementEvent.class);
            if (isValidStatementEvent(event)) {
                StatementEventProcessor processor = (StatementEventProcessor) processors.get("STATEMENT");
                processor.processEvent(event, subscriberName);
                return true;
            } else {
                // Log the raw JSON when validation fails for debugging
                logger.debug("STATEMENT validation failed. Raw JSON: {}", messageText);
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to parse STATEMENT event: {}", e.getMessage());
            logger.debug("Problematic STATEMENT JSON: {}", messageText);
            return false;
        }
    }

    // Validation methods
    private boolean isValidJson(String messageText) {
        if (messageText == null || messageText.trim().isEmpty()) {
            return false;
        }

        String trimmed = messageText.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    private boolean isValidActionEvent(ActionEvent event) {
        if (event.getActionId() == null || event.getActionId().trim().isEmpty()) {
            logger.warn("ACTION event missing required ActionId");
            return false;
        }
        return true;
    }

    private boolean isValidPaymentEvent(PaymentEvent event) {
        if (event.getPaymentId() == null || event.getPaymentId().trim().isEmpty()) {
            logger.warn("PAYMENT event missing required PaymentId");
            return false;
        }
        return true;
    }

    private boolean isValidStatementEvent(StatementEvent event) {
        String primaryId = event.getPrimaryId();
        if (primaryId == null || primaryId.trim().isEmpty()) {
            logger.warn("STATEMENT event missing required ID field. Event details: action='{}', notes='{}', timestamp='{}', statementsId='{}', transactionId='{}'",
                    event.getAction(), event.getNotes(), event.getTimestamp(), event.getStatementsId(), event.getTransactionId());
            return false;
        }
        return true;
    }
}
