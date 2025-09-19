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
exports.TxEventQSubscriber = void 0;
const oracledb = __importStar(require("oracledb"));
class TxEventQSubscriber {
    connection = null;
    queue = null;
    isRunning = false;
    options;
    constructor(options = {}) {
        this.options = {
            batchSize: 1,
            waitTime: 1000,
            autoCommit: true,
            retryAttempts: 3,
            retryDelay: 2000,
            ...options
        };
    }
    async init() {
        try {
            console.log('Initializing Oracle TxEventQ Subscriber...');
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
            this.queue.deqOptions.wait = this.options.batchSize > 1 ? oracledb.AQ_DEQ_NO_WAIT : this.options.waitTime;
            this.queue.deqOptions.consumerName = this.options.consumerName;
            console.log('Queue initialized');
            console.log('Ready to consume events');
        }
        catch (error) {
            console.error('Failed to initialize:', error.message);
            throw error;
        }
    }
    async startConsuming() {
        if (!this.connection || !this.queue) {
            throw new Error('Subscriber not initialized');
        }
        this.isRunning = true;
        console.log('\nStarting message consumption...');
        console.log(`Configuration:`);
        console.log(`  Batch Size: ${this.options.batchSize}`);
        console.log(`  Wait Time: ${this.options.waitTime}ms`);
        console.log(`  Auto Commit: ${this.options.autoCommit}`);
        console.log(`  Retry Attempts: ${this.options.retryAttempts}`);
        console.log(`  Retry Delay: ${this.options.retryDelay}ms`);
        console.log(`  Consumer Name: ${this.options.consumerName}\n`);
        try {
            while (this.isRunning) {
                try {
                    let messages = [];
                    // Demo: Show both deqOne() and deqMany() Oracle AQ methods
                    if (this.options.batchSize === 1) {
                        console.log('Using deqOne()');
                        const message = await this.queue.deqOne();
                        if (message) {
                            messages = [message];
                        }
                    }
                    else {
                        const dequeuedMessages = await this.queue.deqMany(this.options.batchSize);
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
                            await this.connection.commit();
                            console.log(`Transaction committed for ${messages.length} message(s)`);
                        }
                    }
                }
                catch (error) {
                    console.error('Error during consumption:', error.message);
                    // Handle specific Oracle AQ errors
                    if (error.code === 25228) { // No messages available
                        console.log('No messages in queue, continuing to wait...');
                        continue;
                    }
                    // Retry logic for other errors
                    await this.handleRetry(error);
                }
            }
        }
        catch (error) {
            console.error('Fatal error during consumption:', error.message);
            throw error;
        }
    }
    logMessage(message) {
        const eventData = message.payload;
        console.log('\nConsumed Event:');
        console.log(`  Message ID: ${message.msgId.toString('hex')}`);
        console.log(`  CorrelationId: ${message.correlation}`);
        console.log(`  Event Type: ${eventData.eventType}`);
        console.log(`  Timestamp: ${eventData.timestamp}`);
        console.log(`  Payload:`, JSON.stringify(eventData.payload, null, 4));
    }
    async handleRetry(error) {
        console.log(`Retrying after error: ${error.message}`);
        // Simple retry with exponential backoff
        const retryDelay = this.options.retryDelay * Math.pow(2, 0); // Start with base delay
        console.log(`Waiting ${retryDelay}ms before retry...`);
        await new Promise(resolve => setTimeout(resolve, retryDelay));
    }
    async cleanup() {
        this.isRunning = false;
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
    }
}
exports.TxEventQSubscriber = TxEventQSubscriber;
// Main execution
async function main() {
    const subscriber = new TxEventQSubscriber({
        batchSize: 1,
        waitTime: 1000,
        autoCommit: true,
        retryAttempts: 3,
        retryDelay: 1000,
        consumerName: 'event_subscriber'
    });
    try {
        await subscriber.init();
        await subscriber.startConsuming();
    }
    catch (error) {
        console.error('Fatal error:', error);
        await subscriber.cleanup();
        process.exit(1);
    }
}
// Handle graceful shutdown
process.on('SIGINT', async () => {
    console.log('\nShutting down gracefully...');
    process.exit(0);
});
main().catch(console.error);
