import * as oracledb from 'oracledb';
import * as readline from 'readline';
import { EventMessage } from './models/EventMessage';
import { EventGenerator } from './models/EventGenerator';

export class TxEventQPublisher {
  private connection: oracledb.Connection | null = null;
  private queue: oracledb.AdvancedQueue<EventMessage> | null = null;
  private rl: readline.Interface;

  constructor() {
    this.rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });
  }

  async init(): Promise<void> {
    try {
      console.log('Initializing Oracle TxEventQ Publisher...');

      // Enable Thick mode
      if (oracledb.thin) {
        try {
          oracledb.initOracleClient({ libDir: "/Users/ben/instant-client/instantclient_23_3" });
          console.log('Oracle Thick client initialized');
        } catch (initError: any) {
          if (initError.code !== 'NJS-509') { // Already initialized
            throw initError;
          }
          console.log('Oracle Thick client already initialized');
        }
      }

      // Connect to the db
      this.connection = await oracledb.getConnection({
        connectString: "localhost:1522/FREEPDB1",
        user: "txeventq_user",
        password: "pass123"
      });

      console.log('Connected to Oracle database');
      
      this.queue = await this.connection.getQueue<EventMessage>('EVENT_TOPIC', { payloadType: oracledb.DB_TYPE_JSON } as any);
      
      console.log('Queue initialized');
      console.log('Ready to publish events');
      
    } catch (error: any) {
      console.error('Failed to initialize:', error.message);
      throw error;
    }
  }

  async publishEvent<T>(eventType: string, payload: T): Promise<void> {
    if (!this.connection || !this.queue) {
      throw new Error('Publisher not initialized');
    }

    try {
      const message: EventMessage = {
        eventType: eventType,
        payload: payload,
        timestamp: new Date().toISOString(),
        userId: (payload as any).userId
      };

      await this.queue.enqOne({
        payload: message,
        correlation: message.userId?.toString() || 'unknown',
        priority: 0,
        delay: 0,
        expiration: -1,
        exceptionQueue: ''
      } as any);

      await this.connection.commit();

      console.log(`Published event: ${eventType}`);
      console.log(`  Correlation: ${message.userId}`);
      console.log(`  Payload:`, JSON.stringify(payload, null, 2));

    } catch (error: any) {
      console.error('Failed to publish event:', error.message);
      throw error;
    }
  }

  async publishEvents<T>(events: Array<{ eventType: string; payload: T }>): Promise<void> {
    if (!this.connection || !this.queue) {
      throw new Error('Publisher not initialized');
    }

    if (events.length === 0) {
      console.log('No events to publish');
      return;
    }

    try {
      const messages: EventMessage[] = events.map(({ eventType, payload }) => ({
        eventType,
        payload,
        timestamp: new Date().toISOString(),
        userId: (payload as any).userId
      }));

      await this.queue.enqMany(messages.map(message => ({
        payload: message,
        correlation: message.userId?.toString() || 'unknown',
        priority: 0,
        delay: 0,
        expiration: -1,
        exceptionQueue: ''
      })) as any);

      await this.connection.commit();

      console.log(`Published ${events.length} events using enqMany`);

      // Show payload details for each event
      events.forEach((event, index) => {
        console.log(`  Event ${index + 1}: ${event.eventType}`);
        console.log(`    Correlation: ${(event.payload as any).userId || 'unknown'}`);
        console.log(`    Payload:`, JSON.stringify(event.payload, null, 4));
      });

    } catch (error: any) {
      console.error('Failed to publish events:', error.message);
      throw error;
    }
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


  async cleanup(): Promise<void> {
    if (this.connection) {
      try {
        await this.connection.close();
        console.log('Database connection closed');
      } catch (error) {
        console.error('Error closing database connection:', error);
      }
      this.connection = null;
      this.queue = null;
    }

    this.rl.close();
  }
}

// Main execution
async function main() {
  const publisher = new TxEventQPublisher();

  try {
    await publisher.init();
    await publisher.startInteractiveMode();
  } catch (error) {
    console.error('Fatal error:', error);
    await publisher.cleanup();
    process.exit(1);
  }
}

// Handle graceful shutdown
process.on('SIGINT', async () => {
  console.log('\nShutting down gracefully...');
  process.exit(0);
});

main().catch(console.error);