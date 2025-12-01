## Idempotent Consumer Pattern Implementation

### Overview
Implemented an idempotent consumer pattern to handle duplicate Kafka messages safely using a PROCESSED_MESSAGES table.

### Files Created/Updated

#### 1. Entity: ProcessedMessage
- **File**: `entity/ProcessedMessage.java`
- **Purpose**: JPA entity to track processed event IDs
- **Schema**: 
  - `id`: Primary key (BIGINT auto-increment)
  - `event_id`: UUID of the incoming event (NOT NULL)
  - `consumer_group`: Consumer group ID (NOT NULL) - "validation-consumer-group"
  - `topic`: Topic name (NOT NULL) - "ingestion-topic"
  - `processed_at`: Timestamp of processing (NOT NULL, DEFAULT now())
  - `version`: Optimistic locking version
  - **Unique Constraint**: (event_id, consumer_group) - ensures same event cannot be processed twice by same consumer

#### 2. Repository: ProcessedMessageRepository
- **File**: `dao/ProcessedMessageRepository.java`
- **Methods**:
  - `findByEventIdAndConsumerGroup()`: Retrieves a processed message record
  - `existsByEventIdAndConsumerGroup()`: Fast check if event already processed

#### 3. Service: IdempotencyService
- **File**: `service/IdempotencyService.java`
- **Key Method**: 
  - `isAlreadyProcessed(UUID eventId)`: Quick check before processing begins
  - `markAsProcessed(UUID eventId, String topic)`: Atomically records the event as processed
    - Uses @Transactional to ensure atomicity
    - Catches constraint violation exceptions (duplicate event insertion)
    - Returns true if marked successfully, false if already processed

#### 4. Updated: KafkaConsumerService
- **File**: `service/KafkaConsumerService.java`
- **Changes**:
  - Added `@Autowired IdempotencyService idempotencyService`
  - Added early check: `if (isAlreadyProcessed(eventId)) return;` (avoids deserialization of duplicate)
  - Added marking: `markAsProcessed()` call right after deserialization
  - If marking fails, the message is ignored (duplicate detected at DB constraint level)

#### 5. Updated: Schema
- **File**: `db/schema.sql`
- **New Table**: `processed_messages` with UNIQUE constraint on (event_id, consumer_group)

### Flow Diagram

```
Kafka Message Arrives (OutboxEventDto with eventId)
        ↓
[1] isAlreadyProcessed(eventId)? 
    YES → Log "Ignoring duplicate" → RETURN
    NO → Continue
        ↓
[2] Deserialize payload (OutboxEventDto → TradeDto)
        ↓
[3] markAsProcessed(eventId) 
    SUCCESS → Continue to validation
    FAILURE → Duplicate detected at DB level → RETURN (log warning)
        ↓
[4] Validate with Drools rules
        ↓
[5] Route to validation-topic or validation-dlq
        ↓
[6] Save to validation_outbox table (transactional)
```

### How Idempotency Works

1. **First Delivery**: 
   - Event ID not in PROCESSED_MESSAGES
   - isAlreadyProcessed() returns false
   - markAsProcessed() inserts row → SUCCESS
   - Validation proceeds normally

2. **Duplicate/Retry**: 
   - Event ID already in PROCESSED_MESSAGES
   - isAlreadyProcessed() returns true
   - Message ignored immediately (fast path, no reprocessing)
   - OR markAsProcessed() fails with unique constraint violation
   - Duplicate handling graceful

3. **Database Guarantee**:
   - UNIQUE(event_id, consumer_group) constraint prevents duplicate inserts
   - Even if markAsProcessed() is called twice (race condition), only one succeeds
   - Failed inserts caught gracefully in IdempotencyService

### Key Design Patterns

1. **Transactional Outbox**: 
   - ValidationOutbox table persists every validation result
   - Kafka publish happens after DB commit

2. **Idempotent Consumer**:
   - PROCESSED_MESSAGES table tracks ingestion
   - Unique constraint prevents reprocessing same event

3. **Graceful Duplicate Handling**:
   - Early check (isAlreadyProcessed) avoids expensive deserialization
   - DB constraint provides fallback guarantee
   - Exception handling for race conditions

### Thread Safety
- IdempotencyService is stateless (autowired singleton)
- All DB operations are transactional
- Kafka consumer runs in Spring's message listener thread pool
- Multiple instances of validation service can safely run (DB enforces uniqueness)

### Consumer Group & Scalability
- Consumer group: "validation-consumer-group"
- Multiple instances: Each registers with same group
- Partition distribution: Kafka handles automatically
- Idempotency guarantees: Per consumer group (multiple groups can process same event)
- Scaling: Safe to add more instances (constraint prevents duplicates across all instances)
