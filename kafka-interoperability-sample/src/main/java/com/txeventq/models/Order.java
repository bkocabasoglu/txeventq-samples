package com.txeventq.models;

public class Order {
    private String orderId;
    private int customerId;
    private String status;
    private int productId;
    private int numberOfUnits;
    private long createdAt;

    // Constructors
    public Order() {
    }

    public Order(String orderId, int customerId, String status, int productId, int numberOfUnits, long createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.productId = productId;
        this.numberOfUnits = numberOfUnits;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getNumberOfUnits() {
        return numberOfUnits;
    }

    public void setNumberOfUnits(int numberOfUnits) {
        this.numberOfUnits = numberOfUnits;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", customerId=" + customerId +
                ", status='" + status + '\'' +
                ", productId=" + productId +
                ", numberOfUnits=" + numberOfUnits +
                ", createdAt=" + createdAt +
                '}';
    }
}