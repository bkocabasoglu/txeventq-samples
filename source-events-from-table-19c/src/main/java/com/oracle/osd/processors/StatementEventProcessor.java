package com.oracle.osd.processors;

import com.oracle.osd.events.StatementEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for Statement events from TransactionStatementTopic.
 * Handles both Statement and Transaction events.
 */
public class StatementEventProcessor implements EventProcessor<StatementEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(StatementEventProcessor.class);
    
    @Override
    public void processEvent(StatementEvent event, String subscriberName) {
        logger.info("--- {} Event Details ---", getEventType());
        logger.info("{}: {}", event.getIdFieldName(), event.getPrimaryId());
        logger.info("Operation: {}", event.getAction());
        logger.info("Notes: {}", event.getNotes());
        logger.info("Event Timestamp: {}", event.getTimestamp());
        logger.info("Processed by: {}", subscriberName);
        logger.info("------------------------");
    }
    
    @Override
    public String getEventType() {
        return "STATEMENT";
    }
}
