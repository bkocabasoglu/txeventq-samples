import { EventMessage, EventPayload } from './EventMessage';
export declare class EventGenerator {
    private static readonly EVENT_TYPES;
    static getRandomEventType(): string;
    static generateRandomPayload(eventType: string, eventNumber: number): EventPayload;
    static createEventMessage(eventType: string, payload: EventPayload): EventMessage;
}
