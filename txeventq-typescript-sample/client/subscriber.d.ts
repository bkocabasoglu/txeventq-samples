interface SubscriberOptions {
    batchSize?: number;
    waitTime?: number;
    autoCommit?: boolean;
    retryAttempts?: number;
    retryDelay?: number;
    consumerName?: string;
}
export declare class TxEventQSubscriber<T = any> {
    private connection;
    private queue;
    private isRunning;
    private options;
    constructor(options?: SubscriberOptions);
    init(): Promise<void>;
    startConsuming(): Promise<void>;
    private logMessage;
    private handleRetry;
    cleanup(): Promise<void>;
}
export {};
