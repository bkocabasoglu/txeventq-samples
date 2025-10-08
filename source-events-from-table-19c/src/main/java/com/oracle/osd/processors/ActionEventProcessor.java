package com.oracle.osd.processors;

import com.oracle.osd.events.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for Action events from ActionUpdatesTopic.
 */
public class ActionEventProcessor implements EventProcessor<ActionEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(ActionEventProcessor.class);
    
    @Override
    public void processEvent(ActionEvent event, String subscriberName) {
        logger.info("--- {} Event Details ---", getEventType());
        logger.info("Action ID: {}", event.getActionId());
        logger.info("Operation: {}", event.getAction());
        logger.info("Notes: {}", event.getNotes());
        logger.info("Event Timestamp: {}", event.getTimestamp());
        
        if (event.getNotesOld() != null && !event.getNotesOld().isEmpty()) {
            logger.info("Previous Notes: {}", event.getNotesOld());
        }
        
        logger.info("Processed by: {}", subscriberName);
        logger.info("------------------------");
    }
    
    @Override
    public String getEventType() {
        return "ACTION";
    }
}
