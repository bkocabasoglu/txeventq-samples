package com.oracle.osd.processors;

import com.oracle.osd.events.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for Payment events from PaymentUpdatesTopic.
 */
public class PaymentEventProcessor implements EventProcessor<PaymentEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventProcessor.class);
    
    @Override
    public void processEvent(PaymentEvent event, String subscriberName) {
        logger.info("--- {} Event Details ---", getEventType());
        logger.info("Payment ID: {}", event.getPaymentId());
        logger.info("Operation: {}", event.getAction());
        logger.info("Notes: {}", event.getNotes());
        logger.info("Event Timestamp: {}", event.getTimestamp());
        logger.info("Processed by: {}", subscriberName);
        logger.info("------------------------");
    }
    
    @Override
    public String getEventType() {
        return "PAYMENT";
    }
}
