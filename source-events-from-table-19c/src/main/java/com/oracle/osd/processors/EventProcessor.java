package com.oracle.osd.processors;

import com.oracle.osd.events.BaseEvent;

/**
 * Interface for event processors that handle specific event types.
 */
public interface EventProcessor<T extends BaseEvent> {

    /**
     * Process the parsed event and log relevant information.
     *
     * @param event          The parsed event object
     * @param subscriberName The name of the subscriber that received the message
     */
    void processEvent(T event, String subscriberName);

    /**
     * Get the event type name for logging purposes.
     *
     * @return The event type name
     */
    String getEventType();
}
