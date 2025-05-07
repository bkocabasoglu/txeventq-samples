package com.txeventq.utilities;

import com.txeventq.models.Order;

import java.time.Instant;
import java.util.Random;

public class EventGenerator {

    private static final String[] STATUSES = {"Pending", "Processing"};
    private static final Random random = new Random();

    public static Order generateOrderEvent() {
        String orderId = String.valueOf(Instant.now().toEpochMilli());
        int customerId = (random.nextInt(100) + 1); // Customer IDs between 1 and 100
        String status = STATUSES[random.nextInt(STATUSES.length)];
        int productId = random.nextInt(100) + 1; // Product IDs between 1 and 100
        int numberOfUnits = random.nextInt(10) + 1; // Number of units between 1 and 10
        long createdAt = Instant.now().toEpochMilli();

        Order order = new Order(orderId, customerId, status, productId, numberOfUnits, createdAt);

        return order;
    }
}