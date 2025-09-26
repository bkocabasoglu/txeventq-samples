# Insurance Claim Event Processing with Oracle Transaction Event Queue

A demonstration of real-time insurance claim processing using Oracle Transaction Event Queue (TxEventQ). This system generates claim events, publishes them to TxEventQ, and processes them with guaranteed ordering and scalability.

## Quick Start

1. **Start everything:**

```bash
docker-compose up --build -d
```

2. **View logs:**

```bash
# All logs
docker-compose logs -f

# Consumer logs only
docker-compose logs -f consumer

# Producer logs only
docker-compose logs -f producer
```

3. **Stop:**

```bash
docker-compose down
```

if you want to tear down data as well:

```bash
docker-compose down -v
```

## Data Model

Each claim event contains:

- **Event ID**: Unique identifier (UUID)
- **Claim ID**: Claim identifier (integer)
- **Entry Number**: Sequential entry within a claim (1-10)
- **Claim Type**: Insurance type (LIFE, HOME, AUTO, TRAVEL, HEALTH)
- **Status**: Claim status (PENDING, IN_REVIEW, APPROVED, REJECTED, CANCELLED)
- **Entry Status**: Entry status (PENDING, PROCESSED, FAILED, CANCELLED)
- **Amount**: Claim amount in dollars
- **Timestamp**: Event creation time

## What You'll See

- **2 Producer containers** - Continuously generating claim events
- **2 Consumer containers** - Processing events from the queue
- **1 Database container** - Oracle with TxEventQ configured for ordered processing

## Scaling

```bash
# Scale consumers to 5 instances
docker-compose up --scale consumer=5

# Scale producers to 2 instances
docker-compose up --scale producer=2

# Scale both
docker-compose up --scale consumer=5 --scale producer=2
```

### Sample Log Output

```bash
Producer: Published 200 messages in 10 ms
Consumer: Consumed Claim Event:
  Event ID: c2616640-33fd-483f-9171-f78008e786e4
  Claim ID: 6978
  Entry #: 10
  Type: LIFE
  Status: IN_REVIEW
  Entry Status: PROCESSED
  Amount: $6862.56
  Timestamp: Thu Sep 25 22:49:16 CDT 2025
```

## Architecture

```bash
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Producer 1    │    │                  │    │   Consumer 1    │
├─────────────────┤    │  Oracle Database │    ├─────────────────┤
│   Producer 2    │───▶│  + TxEventQ      │───▶│   Consumer 2    │
└─────────────────┘    │  (8 Shards)      │    └─────────────────┘
                       │  Key-based       │
                       │  Partitioning    │
                       └──────────────────┘
```

**Key Features:**

- Key-based partitioning for ordered processing
- Sticky dequeue for claim ordering
- JSON payload support
- Horizontal scaling

## Development

```bash
# To run producer locally - you need java23 at your machine 
mvn exec:java -Dexec.mainClass="com.oracle.osd.ClaimUpdatesProducer"

# To run consumer locally - you need java23 at your machine 
mvn exec:java -Dexec.mainClass="com.oracle.osd.ClaimUpdatesConsumer"

# Check running containers
docker-compose ps -a
```

## Requirements

- Docker & Docker Compose
- Git
- Java 23 (for local development)
