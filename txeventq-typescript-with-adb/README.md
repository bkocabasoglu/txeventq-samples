# Oracle TxEventQ TypeScript Sample (ADB)

TypeScript publisher/subscriber sample for Oracle Transactional Event Queues (TxEventQ) on Oracle Autonomous Database (ADB).

Client mode used in this implementation: **Oracle Node.js Thin mode only** (no Thick mode / no Oracle Instant Client).

This project shows:
- how to connect to ADB using Oracle Node.js driver in Thin mode with a wallet
- how to enqueue JSON events with `enqOne` / `enqMany`
- how to consume events with a named subscriber using `deqOne` / `deqMany`
- how to run a simple interactive event generator for testing

## What the App Does

1. `publisher.ts` connects to ADB and opens a TxEventQ queue.
2. You enter a number in the terminal.
3. The app generates random business-style events and publishes them.
4. `subscriber.ts` continuously dequeues messages for a configured consumer.
5. Each consumed message is logged and committed.

## Architecture

`Publisher (Node.js)` -> `TxEventQ topic in ADB` -> `Subscriber (Node.js)`

Queue behavior configured by SQL script:
- multiple consumers enabled
- queue sharded (`SHARD_NUM = 4`)
- key-based enqueue enabled
- sticky dequeue enabled

## Prerequisites

- Oracle Autonomous Database (ADB)
- ADB wallet downloaded and unzipped
- Node.js 20+
- npm

## Quick Start

### 1. Install dependencies

```bash
npm install
```

### 2. Configure environment

```bash
cp .env.example .env
```

Set values in `.env`:

- `ORACLE_USER`
- `ORACLE_PASSWORD`
- `ORACLE_CONNECT_STRING` (wallet alias like `myadb_high`)
- `ORACLE_WALLET_PATH` (absolute path)
- `ORACLE_WALLET_PASSWORD` (only if wallet keys are encrypted)

### 3. Create DB user and queue objects

Run scripts in order:

1. As `ADMIN`, run `sql-start-up-scripts/01-create-users.sql`
2. As the app user (default `TXEVENTQ_USER`), run `sql-start-up-scripts/02-create-topics.sql`

Note: `01-create-users.sql` contains a default password placeholder. Change it before running in any real environment.

### 4. Build

```bash
npm run build
```

## Run the Sample

Start subscriber first:

```bash
npm run start:subscriber
```

Start publisher in another terminal:

```bash
npm run start:publisher
```

Publisher commands:
- `<number>` publish that many events
- `help` show commands
- `quit` / `exit` / `q` stop publisher

## Development Mode

Run directly from TypeScript without prebuilding:

```bash
npm run dev:subscriber
npm run dev:publisher
```

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `ORACLE_USER` | Yes | - | DB username |
| `ORACLE_PASSWORD` | Yes | - | DB password |
| `ORACLE_CONNECT_STRING` | Yes | - | Wallet TNS alias (for example `myadb_high`) |
| `ORACLE_WALLET_PATH` | Yes | - | Absolute path to unzipped wallet |
| `ORACLE_WALLET_PASSWORD` | No | empty | Wallet password when keys are encrypted |
| `SUBSCRIBER_BATCH_SIZE` | No | `25` | Messages per dequeue call |
| `SUBSCRIBER_WAIT_SECONDS` | No | `5` | Dequeue wait timeout |
| `SUBSCRIBER_RETRY_ATTEMPTS` | No | `3` | Consecutive retry limit |
| `SUBSCRIBER_RETRY_DELAY_MS` | No | `1000` | Base retry delay (backoff multiplier) |
| `DB_RECONNECT_MAX_ATTEMPTS` | No | `0` | Max reconnect attempts (`0` = unlimited) |
| `DB_RECONNECT_INITIAL_DELAY_MS` | No | `1000` | Initial reconnect delay |
| `DB_RECONNECT_MAX_DELAY_MS` | No | `30000` | Max reconnect backoff delay |

## Code Walkthrough

### `src/config.ts`

Centralized config loading and validation:
- validates required env vars
- parses numeric tuning values with minimum checks
- enforces Oracle driver Thin mode (`assertThinMode`)
- builds shared Oracle connection attributes (`getOracleConnectionAttributes`)
- centralizes DB reconnect policy used by publisher/subscriber

### `src/publisher.ts`

`TxEventQPublisher`:
- initializes DB connection and queue handle
- publishes one message with `enqOne` when count is `1`
- publishes batches with `enqMany` when count is `>1`
- commits after enqueue
- reconnects automatically on recoverable DB/network errors and retries publish once
- includes interactive CLI loop for test traffic generation
- handles `SIGINT` for graceful shutdown

### `src/subscriber.ts`

`TxEventQSubscriber`:
- initializes connection and queue dequeue options
- sets `consumerName` and dequeue wait time
- uses `deqOne` (batch 1) or `deqMany` (batch >1)
- logs message metadata and JSON payload
- commits after successful message processing
- retries transient failures with incremental delay
- reconnects automatically with exponential backoff on recoverable connection failures
- handles `SIGINT` and `SIGTERM`

### `src/models/EventGenerator.ts`

Generates random payloads for multiple event types:
- `user.created`, `user.updated`
- `order.placed`, `order.cancelled`
- `payment.processed`
- `notification.sent`
- `product.viewed`
- `cart.updated`

Used by the publisher to create realistic sample traffic.

### `src/models/EventMessage.ts`

Type definitions for:
- common queue message envelope (`EventMessage`)
- event payload contracts (`UserEvent`, `OrderEvent`, etc.)

### `sql-start-up-scripts/`

- `01-create-users.sql`: creates app user and grants TxEventQ privileges
- `02-create-topics.sql`: creates transactional event queue and adds subscriber
- `03-drop-topics.sql`: stops and drops queue objects for cleanup

## Troubleshooting

- `NJS-505` (TLS/connection setup): verify wallet path, connect string alias, and wallet password.
- Thin mode enforcement error: remove any `oracledb.initOracleClient()` usage; this sample is Thin mode only.
- Missing env var errors: check `.env` and ensure no blank required values.
- Long outage handling: tune `DB_RECONNECT_*` values to control reconnect behavior.
- No messages consumed: confirm subscriber `event_subscriber` exists in queue setup.

## Cleanup (Optional)

To remove queue objects, run:

- `sql-start-up-scripts/03-drop-topics.sql`

## Scripts

- `npm run build`
- `npm run start:publisher`
- `npm run start:subscriber`
- `npm run dev:publisher`
- `npm run dev:subscriber`
