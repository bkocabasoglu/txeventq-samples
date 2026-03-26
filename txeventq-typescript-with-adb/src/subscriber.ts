import * as oracledb from 'oracledb';
import { EventMessage } from './models/EventMessage';
import { assertThinMode, getConfig, getOracleConnectionAttributes, QUEUE_CONFIG } from './config';
import {
  getErrorMessage,
  isQueueEmptyError,
  isRecoverableConnectionError,
  sleep
} from './connection-utils';

interface SubscriberOptions {
  batchSize?: number;
  waitSeconds?: number;
  autoCommit?: boolean;
  retryAttempts?: number;
  retryDelayMs?: number;
  consumerName?: string;
}

export class TxEventQSubscriber<T = any> {
  private connection: oracledb.Connection | null = null;
  private queue: oracledb.AdvancedQueue<EventMessage> | null = null;
  private isRunning: boolean = false;
  private options: SubscriberOptions;
  private readonly config = getConfig();
  private consecutiveErrors = 0;
  private reconnectInFlight: Promise<void> | null = null;
  private isShuttingDown = false;

  constructor(options: SubscriberOptions = {}) {
    this.options = {
      batchSize: this.config.subscriber.batchSize,
      waitSeconds: this.config.subscriber.waitSeconds,
      autoCommit: true,
      retryAttempts: this.config.subscriber.retryAttempts,
      retryDelayMs: this.config.subscriber.retryDelayMs,
      consumerName: QUEUE_CONFIG.consumerName,
      ...options
    };
  }

  async init(): Promise<void> {
    try {
      console.log('Initializing Oracle TxEventQ Subscriber...');

      assertThinMode();
      await this.reconnectWithBackoff('startup');
      console.log('Ready to consume events');
      
    } catch (error: any) {
      if (error?.code === 'NJS-505') {
        console.error('Thin TLS setup failed. Check ORACLE_WALLET_PATH, ORACLE_CONNECT_STRING, and ORACLE_WALLET_PASSWORD.');
      }
      console.error('Failed to initialize:', error.message);
      throw error;
    }
  }

  async startConsuming(): Promise<void> {
    this.isRunning = true;
    console.log('\nStarting message consumption...');
    console.log(`Configuration:`);
    console.log(`  Batch Size: ${this.options.batchSize}`);
    console.log(`  Wait Time: ${this.options.waitSeconds}s`);
    console.log(`  Auto Commit: ${this.options.autoCommit}`);
    console.log(`  Retry Attempts: ${this.options.retryAttempts}`);
    console.log(`  Retry Delay: ${this.options.retryDelayMs}ms`);
    console.log(`  Consumer Name: ${this.options.consumerName}\n`);

    try {
      while (this.isRunning) {
        await this.ensureConnected();

        try {
          let messages: oracledb.AdvancedQueueMessage<EventMessage>[] = [];
          const queue = this.queue;
          const connection = this.connection;
          if (!queue || !connection) {
            throw new Error('Subscriber is not connected');
          }

          if (this.options.batchSize === 1) {
            const message = await queue.deqOne();
            if (message) {
              messages = [message];
            }
          } else {
            const dequeuedMessages = await queue.deqMany(this.options.batchSize!);
            if (dequeuedMessages) {
              messages = dequeuedMessages;
            }
          }

          if (messages && messages.length > 0) {
            for (const message of messages) {
              this.logMessage(message);
            }

            // Commit the transaction if auto-commit is enabled
            if (this.options.autoCommit) {
              await connection.commit();
              console.log(`Transaction committed for ${messages.length} message(s)`);
            }
          }

          this.consecutiveErrors = 0;

        } catch (error: any) {
          console.error('Error during consumption:', error.message);
          
          if (isQueueEmptyError(error)) {
            this.consecutiveErrors = 0;
            continue;
          }

          if (isRecoverableConnectionError(error)) {
            console.warn(`Recoverable connection issue detected: ${getErrorMessage(error)}`);
            await this.reconnectWithBackoff('consume loop', error);
            this.consecutiveErrors = 0;
            continue;
          }

          // Retry logic for other errors
          await this.handleRetry(error);
        }
      }

    } catch (error: any) {
      console.error('Fatal error during consumption:', error.message);
      throw error;
    }
  }

  private logMessage(message: oracledb.AdvancedQueueMessage<any>): void {
    const eventData: EventMessage = message.payload as any;
    
    console.log('\nConsumed Event:');
    console.log(`  Message ID: ${message.msgId.toString('hex')}`);
    console.log(`  CorrelationId: ${message.correlation}`);
    console.log(`  Event Type: ${eventData.eventType}`);
    console.log(`  Timestamp: ${eventData.timestamp}`);
    console.log(`  Payload:`, JSON.stringify(eventData.payload, null, 4));
  }

  private async handleRetry(error: any): Promise<void> {
    this.consecutiveErrors += 1;
    if (this.consecutiveErrors > this.options.retryAttempts!) {
      throw new Error(
        `Stopping consumer after ${this.options.retryAttempts} consecutive errors. Last error: ${error.message}`
      );
    }

    const retryDelay = this.options.retryDelayMs! * this.consecutiveErrors;
    console.log(
      `Retry ${this.consecutiveErrors}/${this.options.retryAttempts} after error: ${error.message}. Waiting ${retryDelay}ms.`
    );

    await sleep(retryDelay);
  }

  private async ensureConnected(): Promise<void> {
    if (this.connection && this.queue) {
      return;
    }
    await this.reconnectWithBackoff('ensureConnected');
  }

  private async reconnectWithBackoff(reason: string, cause?: unknown): Promise<void> {
    if (this.reconnectInFlight) {
      return this.reconnectInFlight;
    }

    this.reconnectInFlight = (async () => {
      let attempt = 0;
      let delay = this.config.connectionResilience.initialReconnectDelayMs;
      const maxAttempts = this.config.connectionResilience.maxReconnectAttempts;

      while (!this.isShuttingDown) {
        attempt += 1;

        try {
          await this.closeConnection();
          await this.connectAndInitializeQueue();
          if (attempt > 1 || cause) {
            console.log(`Reconnected successfully (${reason}).`);
          }
          return;
        } catch (error) {
          const errorMessage = getErrorMessage(error);
          const recoverable = isRecoverableConnectionError(error);

          if (!recoverable) {
            throw new Error(`Non-recoverable connection error during ${reason}: ${errorMessage}`);
          }

          if (maxAttempts > 0 && attempt >= maxAttempts) {
            throw new Error(
              `Reconnect failed after ${attempt} attempts during ${reason}. Last error: ${errorMessage}`
            );
          }

          const waitMs = Math.min(delay, this.config.connectionResilience.maxReconnectDelayMs);
          console.warn(
            `Reconnect attempt ${attempt} failed (${reason}): ${errorMessage}. Retrying in ${waitMs}ms...`
          );
          await sleep(waitMs);
          delay = Math.min(waitMs * 2, this.config.connectionResilience.maxReconnectDelayMs);
        }
      }
    })();

    try {
      await this.reconnectInFlight;
    } finally {
      this.reconnectInFlight = null;
    }
  }

  private async connectAndInitializeQueue(): Promise<void> {
    this.connection = await oracledb.getConnection(getOracleConnectionAttributes(this.config));
    this.queue = await this.connection.getQueue<EventMessage>(
      QUEUE_CONFIG.name,
      { payloadType: oracledb.DB_TYPE_JSON } as any
    );
    this.queue.deqOptions.wait = this.options.waitSeconds!;
    this.queue.deqOptions.consumerName = this.options.consumerName!;
    console.log('Connected to Oracle database (Thin mode)');
    console.log('Queue initialized');
  }

  private async closeConnection(): Promise<void> {
    if (!this.connection) {
      this.queue = null;
      return;
    }

    try {
      await this.connection.close();
    } catch {
      // Ignore close errors while replacing broken connections.
    } finally {
      this.connection = null;
      this.queue = null;
    }
  }

  async cleanup(): Promise<void> {
    this.isRunning = false;
    this.isShuttingDown = true;
    await this.closeConnection();
    console.log('Database connection closed');
  }
}

// Main execution
async function main() {
  const subscriber = new TxEventQSubscriber<EventMessage>({
    autoCommit: true
  });

  const shutdown = async (signal: string): Promise<void> => {
    console.log(`\nReceived ${signal}, shutting down gracefully...`);
    await subscriber.cleanup();
    process.exit(0);
  };

  process.on('SIGINT', () => {
    void shutdown('SIGINT');
  });

  process.on('SIGTERM', () => {
    void shutdown('SIGTERM');
  });

  try {
    await subscriber.init();
    await subscriber.startConsuming();
  } catch (error) {
    console.error('Fatal error:', error);
    await subscriber.cleanup();
    process.exit(1);
  }
}

main().catch(console.error);
