# pms-validation — Validation Microservice

This microservice validates trade events using a Drools rules engine, persists validation results to an outbox table, and publishes validation events to Kafka.

## Key components (refactored)

- `IngestionListener` — Kafka consumer (ingestion-topic). Single responsibility: consume raw messages, perform fast idempotency check, deserialize payload into `TradeDto`, and delegate processing to `IngestionProcessor`.

- `IngestionProcessor` — Orchestrator for a single ingestion event. Responsibilities:
  - Atomically mark events as processed (via `IdempotencyService`)
  - Validate trade using `TradeValidationService` (Drools)
  - Persist validation results into the DB via `ValidationOutboxService`
  - Publish validation events via `KafkaProducerService`

- `TradeValidationService` — Runs Drools rules against a `TradeDto` and returns a `ValidationResult`.

- `ValidationOutboxService` — Persists validation outcome records (`ValidationOutboxEntity`) and builds `ValidationOutputDto` events for publishing.

- `KafkaProducerService` — Publishes `ValidationOutputDto` objects to Kafka topics (`validation-topic` or `validation-dlq`).

- `IdempotencyService` — Tracks processed events in `processed_messages` (table) and provides `isAlreadyProcessed()` and `markAsProcessed()` operations.

## DTOs / Entities (current)

- DTOs:
  - `IngestionEventDto` — Envelope received on ingestion-topic (eventId, payloadBytes, etc.)
  - `TradeDto` — Trade payload used for validation
  - `ValidationOutputDto` — Event published after validation
  - `ValidationResult` — Result object with `valid` flag and `errors` list

- Entities:
  - `ValidationOutboxEntity` — persistence for validation results
  - `ProcessedMessage` — tracks processed event ids and consumer group
  - `StockEntity` — stock reference data

## Notes
- Refactor preserved behavior; changes are naming & responsibility splits only.
- Deprecated legacy classes were removed.

## Tests
- Added unit tests for `IngestionProcessor` and `IngestionListener` (see `src/test/java/...`).

If you'd like additional renames or removal of any other legacy artifacts, tell me which and I'll apply them in small commits.