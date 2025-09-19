"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.TxEventQPublisher = void 0;
const oracledb = __importStar(require("oracledb"));
const readline = __importStar(require("readline"));
const EventGenerator_1 = require("./models/EventGenerator");
class TxEventQPublisher {
    connection = null;
    queue = null;
    rl;
    constructor() {
        this.rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });
    }
    async init() {
        try {
            console.log('Initializing Oracle TxEventQ Publisher...');
            // Enable Thick mode
            if (oracledb.thin) {
                try {
                    oracledb.initOracleClient({ libDir: "/Users/ben/instant-client/instantclient_23_3" });
                    console.log('Oracle Thick client initialized');
                }
                catch (initError) {
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
            this.queue = await this.connection.getQueue('EVENT_TOPIC', { payloadType: oracledb.DB_TYPE_JSON });
            console.log('Queue initialized');
            console.log('Ready to publish events');
        }
        catch (error) {
            console.error('Failed to initialize:', error.message);
            throw error;
        }
    }
    async publishEvent(eventType, payload) {
        if (!this.connection || !this.queue) {
            throw new Error('Publisher not initialized');
        }
        try {
            const message = {
                eventType: eventType,
                payload: payload,
                timestamp: new Date().toISOString(),
                userId: payload.userId
            };
            await this.queue.enqOne({
                payload: message,
                correlation: message.userId?.toString() || 'unknown',
                priority: 0,
                delay: 0,
                expiration: -1,
                exceptionQueue: ''
            });
            await this.connection.commit();
            console.log(`Published event: ${eventType}`);
            console.log(`  Correlation: ${message.userId}`);
            console.log(`  Payload:`, JSON.stringify(payload, null, 2));
        }
        catch (error) {
            console.error('Failed to publish event:', error.message);
            throw error;
        }
    }
    async publishEvents(events) {
        if (!this.connection || !this.queue) {
            throw new Error('Publisher not initialized');
        }
        if (events.length === 0) {
            console.log('No events to publish');
            return;
        }
        try {
            const messages = events.map(({ eventType, payload }) => ({
                eventType,
                payload,
                timestamp: new Date().toISOString(),
                userId: payload.userId
            }));
            await this.queue.enqMany(messages.map(message => ({
                payload: message,
                correlation: message.userId?.toString() || 'unknown',
                priority: 0,
                delay: 0,
                expiration: -1,
                exceptionQueue: ''
            })));
            await this.connection.commit();
            console.log(`Published ${events.length} events using enqMany`);
            // Show payload details for each event
            events.forEach((event, index) => {
                console.log(`  Event ${index + 1}: ${event.eventType}`);
                console.log(`    Correlation: ${event.payload.userId || 'unknown'}`);
                console.log(`    Payload:`, JSON.stringify(event.payload, null, 4));
            });
        }
        catch (error) {
            console.error('Failed to publish events:', error.message);
            throw error;
        }
    }
    async startInteractiveMode() {
        console.log('\nTxEventQ Publisher - Interactive Mode');
        console.log('Commands:');
        console.log('  <number> - Publish that many events');
        console.log('  quit, exit, q - Exit the publisher');
        console.log('  help - Show this help\n');
        const askForInput = () => {
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
                }
                catch (error) {
                    console.error('Error publishing events:', error);
                }
                askForInput();
            });
        };
        askForInput();
    }
    async publishRandomEvents(count) {
        let duration;
        if (count === 1) {
            // Use enqOne for single event
            const eventType = EventGenerator_1.EventGenerator.getRandomEventType();
            const payload = EventGenerator_1.EventGenerator.generateRandomPayload(eventType, 1);
            const startTime = Date.now();
            await this.publishEvent(eventType, payload);
            duration = Date.now() - startTime;
        }
        else {
            // Use enqMany for multiple events
            const events = [];
            for (let i = 1; i <= count; i++) {
                const eventType = EventGenerator_1.EventGenerator.getRandomEventType();
                const payload = EventGenerator_1.EventGenerator.generateRandomPayload(eventType, i);
                events.push({ eventType, payload });
            }
            const startTime = Date.now();
            await this.publishEvents(events);
            duration = Date.now() - startTime;
        }
        console.log(`Successfully published ${count} event(s) in ${duration}ms\n`);
    }
    async cleanup() {
        if (this.connection) {
            try {
                await this.connection.close();
                console.log('Database connection closed');
            }
            catch (error) {
                console.error('Error closing database connection:', error);
            }
            this.connection = null;
            this.queue = null;
        }
        this.rl.close();
    }
}
exports.TxEventQPublisher = TxEventQPublisher;
// Main execution
async function main() {
    const publisher = new TxEventQPublisher();
    try {
        await publisher.init();
        await publisher.startInteractiveMode();
    }
    catch (error) {
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
