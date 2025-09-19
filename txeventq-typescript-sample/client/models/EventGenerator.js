"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.EventGenerator = void 0;
class EventGenerator {
    static EVENT_TYPES = [
        'user.created',
        'user.updated',
        'order.placed',
        'order.cancelled',
        'payment.processed',
        'notification.sent',
        'product.viewed',
        'cart.updated'
    ];
    static getRandomEventType() {
        return this.EVENT_TYPES[Math.floor(Math.random() * this.EVENT_TYPES.length)];
    }
    static generateRandomPayload(eventType, eventNumber) {
        const userId = Math.floor(Math.random() * 10000);
        const basePayload = {
            eventId: `evt_${Date.now()}_${eventNumber}`,
            timestamp: new Date().toISOString(),
            userId: userId
        };
        switch (eventType) {
            case 'user.created':
            case 'user.updated':
                return {
                    ...basePayload,
                    name: `User ${eventNumber}`,
                    email: `user${eventNumber}@example.com`
                };
            case 'order.placed':
            case 'order.cancelled':
                return {
                    ...basePayload,
                    orderId: Math.floor(Math.random() * 100000),
                    total: (Math.random() * 1000).toFixed(2),
                    items: ['Product A', 'Product B', 'Product C'].slice(0, Math.floor(Math.random() * 3) + 1)
                };
            case 'payment.processed':
                return {
                    ...basePayload,
                    paymentId: `pay_${Math.floor(Math.random() * 100000)}`,
                    amount: (Math.random() * 500).toFixed(2),
                    currency: 'USD',
                    status: 'completed'
                };
            case 'notification.sent':
                return {
                    ...basePayload,
                    type: ['email', 'sms', 'push'][Math.floor(Math.random() * 3)],
                    message: `Notification ${eventNumber}`,
                    channel: 'system'
                };
            case 'product.viewed':
                return {
                    ...basePayload,
                    productId: Math.floor(Math.random() * 1000),
                    category: ['electronics', 'clothing', 'books'][Math.floor(Math.random() * 3)]
                };
            case 'cart.updated':
                return {
                    ...basePayload,
                    itemCount: Math.floor(Math.random() * 10) + 1,
                    totalValue: (Math.random() * 200).toFixed(2)
                };
            default:
                return {
                    ...basePayload,
                    message: `Generic event ${eventNumber}`,
                    data: { random: Math.random() }
                };
        }
    }
    static createEventMessage(eventType, payload) {
        return {
            eventType,
            payload,
            timestamp: new Date().toISOString(),
            userId: payload.userId // Extract userId from payload
        };
    }
}
exports.EventGenerator = EventGenerator;
