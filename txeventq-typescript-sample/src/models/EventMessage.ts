export interface EventMessage {
  eventType: string;
  payload: any;
  timestamp: string;
  userId?: number;
}

export interface UserEvent {
  eventId: string;
  timestamp: string;
  userId: number;
  name: string;
  email: string;
}

export interface OrderEvent {
  eventId: string;
  timestamp: string;
  orderId: number;
  userId: number;
  total: string;
  items: string[];
}

export interface PaymentEvent {
  eventId: string;
  timestamp: string;
  paymentId: string;
  amount: string;
  currency: string;
  status: string;
}

export interface NotificationEvent {
  eventId: string;
  timestamp: string;
  userId: number;
  type: string;
  message: string;
  channel: string;
}

export interface ProductEvent {
  eventId: string;
  timestamp: string;
  productId: number;
  userId: number;
  category: string;
}

export interface CartEvent {
  eventId: string;
  timestamp: string;
  userId: number;
  itemCount: number;
  totalValue: string;
}

export type EventPayload = 
  | UserEvent 
  | OrderEvent 
  | PaymentEvent 
  | NotificationEvent 
  | ProductEvent 
  | CartEvent;
