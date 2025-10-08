package com.oracle.osd.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payment event DTO for PaymentUpdatesTopic messages.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent extends BaseEvent {

    @JsonProperty("PaymentId")
    private String paymentId;

    // Constructors
    public PaymentEvent() {
    }

    public PaymentEvent(String paymentId, String action, String notes, String timestamp) {
        super(action, notes, timestamp);
        this.paymentId = paymentId;
    }

    // Getters and setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    @Override
    public String toString() {
        return String.format("PaymentEvent{paymentId='%s', %s}", paymentId, super.toString());
    }
}
