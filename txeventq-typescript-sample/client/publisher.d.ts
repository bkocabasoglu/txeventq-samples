export declare class TxEventQPublisher {
    private connection;
    private queue;
    private rl;
    constructor();
    init(): Promise<void>;
    publishEvent<T>(eventType: string, payload: T): Promise<void>;
    publishEvents<T>(events: Array<{
        eventType: string;
        payload: T;
    }>): Promise<void>;
    startInteractiveMode(): Promise<void>;
    private publishRandomEvents;
    cleanup(): Promise<void>;
}
