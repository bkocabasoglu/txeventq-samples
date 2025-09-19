# Oracle TxEventQ POC

## Overview

Oracle Transactional Event Queue demonstration with TypeScript publisher and subscriber.

## Requirements

Instant Client Basic is required to be able to run the applications. You can download it here: https://www.oracle.com/database/technologies/instant-client/downloads.html based on your OS and processor type. Make sure that after installation you update the `libDir` in the `publisher.ts` and `subscriber.ts` files. 

## Setup

1. Start Oracle database:

   ```bash
   docker-compose up -d
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Build the application:

   ```bash
   npm run build
   ```

## Running the Application

### Publisher

```bash
npm run start:publisher
```

### Subscriber

```bash
npm run start:subscriber
```

## Usage

1. Start the subscriber first
2. Start the publisher in another terminal
3. Enter number of events to publish (e.g., `5` for 5 events)
4. Watch events being consumed by the subscriber

## Configuration

- **Publisher**: Interactive mode with batch publishing
- **Subscriber**: Auto-starts consuming with configurable batch size
- **Queue**: `EVENT_TOPIC` with JSON payloads
- **Consumer**: `event_subscriber`
