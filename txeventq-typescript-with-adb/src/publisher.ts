import * as oracledb from 'oracledb';
import * as readline from 'readline';
import { EventMessage } from './models/EventMessage';
import { EventGenerator } from './models/EventGenerator';
import { assertThinMode, getConfig, getOracleConnectionAttributes, QUEUE_CONFIG } from './config';
import { getErrorMessage, isRecoverableConnectionError, sleep } from './connection-utils';

export class TxEventQPublisher {
  private connection: oracledb.Connection | null = null;
  private queue: oracledb.AdvancedQueue<EventMessage> | null = null;
  private rl: readline.Interface;
  private readonly config = getConfig();
  private reconnectInFlight: Promise<void> | null = null;
  private isShuttingDown = false;

  constructor() {
    this.rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });
  }

  async init(): Promise<void> {
    try {
      console.log('Initializing Oracle TxEventQ Publisher...');

      assertThinMode();
      await this.reconnectWithBackoff('startup');
      console.log('Ready to publish events');
      
    } catch (error: any) {
      if (error?.code === 'NJS-505') {
        console.error('Thin TLS setup failed. Check ORACLE_WALLET_PATH, ORACLE_CONNECT_STRING, and ORACLE_WALLET_PASSWORD.');
      }
      console.error('Failed to initialize:', error.message);
      throw error;
    }
  }

  async publishEvent<T>(eventType: string, payload: T): Promise<void> {
    await this.runWithReconnect('publishEvent', async () => {
      const connection = this.connection;
      const queue = this.queue;
      if (!connection || !queue) {
        throw new Error('Publisher is not connected');
      }

      const message: EventMessage = {
        eventType: eventType,
        payload: payload,
        timestamp: new Date().toISOString(),
        userId: (payload as any).userId
      };

      await queue.enqOne({
        payload: message,
        correlation: message.userId?.toString() || 'unknown',
        priority: 0,
        delay: 0,
        expiration: -1,
        exceptionQueue: ''
      } as any);

      await connection.commit();

      console.log(`Published event: ${eventType}`);
      console.log(`  Correlation: ${message.userId}`);
      console.log(`  Payload:`, JSON.stringify(payload, null, 2));
    });
  }

  async publishEvents<T>(events: Array<{ eventType: string; payload: T }>): Promise<void> {
    if (events.length === 0) {
      console.log('No events to publish');
      return;
    }

    await this.runWithReconnect('publishEvents', async () => {
      const connection = this.connection;
      const queue = this.queue;
      if (!connection || !queue) {
        throw new Error('Publisher is not connected');
      }

      const messages: EventMessage[] = events.map(({ eventType, payload }) => ({
        eventType,
        payload,
        timestamp: new Date().toISOString(),
        userId: (payload as any).userId
      }));

      await queue.enqMany(messages.map(message => ({
        payload: message,
        correlation: message.userId?.toString() || 'unknown',
        priority: 0,
        delay: 0,
        expiration: -1,
        exceptionQueue: ''
      })) as any);

      await connection.commit();

      console.log(`Published ${events.length} events using enqMany`);

      // Show payload details for each event
      events.forEach((event, index) => {
        console.log(`  Event ${index + 1}: ${event.eventType}`);
        console.log(`    Correlation: ${(event.payload as any).userId || 'unknown'}`);
        console.log(`    Payload:`, JSON.stringify(event.payload, null, 4));
      });
    });
  }

  async startInteractiveMode(): Promise<void> {
    console.log('\nTxEventQ Publisher - Interactive Mode');
    console.log('Commands:');
    console.log('  <number> - Publish that many events');
    console.log('  quit, exit, q - Exit the publisher');
    console.log('  help - Show this help\n');

    const askForInput = (): void => {
      this.rl.question('Enter number of events to publish (or command): ', async (input) => {
        const trimmedInput = input.trim().toLowerCase();

        if (trimmedInput === 'quit' || trimmedInput === 'exit' || trimmedInput === 'q') {
          console.log('Exiting publisher...');
          await this.cleanup();
          process.exit(0);
        }

        if (trimmedInput === 'help') {
          console.log('\nCommands:');
          console.log('  <number> - Publish that many events');
          console.log('  quit, exit, q - Exit the publisher');
          console.log('  help - Show this help\n');
          askForInput();
          return;
        }

        const numEvents = parseInt(trimmedInput);
        if (isNaN(numEvents) || numEvents < 1) {
          console.log('Please enter a valid number or command');
          askForInput();
          return;
        }

        try {
          await this.publishRandomEvents(numEvents);
        } catch (error) {
          console.error('Error publishing events:', error);
        }

        askForInput();
      });
    };

    askForInput();
  }

  private async publishRandomEvents(count: number): Promise<void> {
    let duration: number;

    if (count === 1) {
      // Use enqOne for single event
      const eventType = EventGenerator.getRandomEventType();
      const payload = EventGenerator.generateRandomPayload(eventType, 1);

      const startTime = Date.now();
      await this.publishEvent(eventType, payload);
      duration = Date.now() - startTime;
    } else {
      // Use enqMany for multiple events
      const events: Array<{ eventType: string; payload: any }> = [];
      for (let i = 1; i <= count; i++) {
        const eventType = EventGenerator.getRandomEventType();
        const payload = EventGenerator.generateRandomPayload(eventType, i);
        events.push({ eventType, payload });
      }

      const startTime = Date.now();
      await this.publishEvents(events);
      duration = Date.now() - startTime;
    }

    console.log(`Successfully published ${count} event(s) in ${duration}ms\n`);
  }

  private async runWithReconnect<T>(operationName: string, work: () => Promise<T>): Promise<T> {
    let retryCount = 0;

    while (!this.isShuttingDown) {
      await this.ensureConnected();

      try {
        return await work();
      } catch (error) {
        if (!isRecoverableConnectionError(error) || retryCount >= 1) {
          console.error(`${operationName} failed: ${getErrorMessage(error)}`);
          throw error;
        }

        retryCount += 1;
        console.warn(
          `${operationName} hit a recoverable connection issue. Reconnecting and retrying once...`
        );
        await this.reconnectWithBackoff(`${operationName} retry`, error);
      }
    }

    throw new Error('Publisher is shutting down');
  }

  private async ensureConnected(): Promise<void> {
    if (this.connection && this.queue) {
      return;
    }
    await this.reconnectWithBackoff('ensureConnected');
  }

  private async connectAndInitializeQueue(): Promise<void> {
    this.connection = await oracledb.getConnection(getOracleConnectionAttributes(this.config));
    this.queue = await this.connection.getQueue<EventMessage>(
      QUEUE_CONFIG.name,
      { payloadType: oracledb.DB_TYPE_JSON } as any
    );
    console.log('Connected to Oracle database (Thin mode)');
    console.log('Queue initialized');
  }

  private async reconnectWithBackoff(reason: string, cause?: unknown): Promise<void> {
    if (this.reconnectInFlight) {
      return this.reconnectInFlight;
    }

    this.reconnectInFlight = (async () => {
      let attempt = 0;
      let delay = this.config.connectionResilience.initialReconnectDelayMs;

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
          const maxAttempts = this.config.connectionResilience.maxReconnectAttempts;

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
    this.isShuttingDown = true;

    await this.closeConnection();
    console.log('Database connection closed');

    this.rl.close();
  }
}

// Main execution
async function main() {
  const publisher = new TxEventQPublisher();

  try {
    process.on('SIGINT', async () => {
      console.log('\nReceived SIGINT, shutting down gracefully...');
      await publisher.cleanup();
      process.exit(0);
    });

    await publisher.init();
    await publisher.startInteractiveMode();
  } catch (error) {
    console.error('Fatal error:', error);
    await publisher.cleanup();
    process.exit(1);
  }
}

main().catch(console.error);
