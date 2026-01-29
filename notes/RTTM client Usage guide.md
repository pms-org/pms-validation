# PMS RTTM Client: Usage and Testing Guide

This library sends RTTM protobuf events (trade, DLQ, queue metric, error) over Kafka or HTTP. Use it from downstream services such as `pms-validation` to publish telemetry to the running `pms-rttm` application.

## Prerequisites
- JDK 21, Maven 3.9+
- Kafka broker for `kafka` mode or RTTM HTTP ingestion endpoint for `http` mode
- Confluent protobuf serializer on the classpath (already in this project)
- Access to the `pms-rttm` service (for manual end-to-end tests)

## Add the dependency
```xml
<dependency>
    <groupId>com.pms</groupId>
    <artifactId>pms-rttm-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration
Common environment variables (examples):
- `KAFKA_BOOTSTRAP_SERVERS` (e.g., `localhost:9092`)
- `KAFKA_PRODUCER_ACKS` (default `all`), `KAFKA_PRODUCER_RETRIES` (default `3`)
- `KAFKA_CONSUMER_GROUP_ID` (default `pms-consumer-group`), `KAFKA_CONSUMER_AUTO_OFFSET_RESET` (default `earliest`)
- `KAFKA_CONSUMER_ENABLE_AUTO_COMMIT` (default `false`)
- `KAFKA_LISTENER_ACK_MODE` (default `manual_immediate`)
- HTTP mode: `RTTM_API_KEY`, and `rttm.client.http.base-url` in your Spring config

Sample `application.yml` fragment:
```yaml
rttm:
  client:
    mode: kafka # kafka | http | noop
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      topics:
        trade-events: rttm.trade.events
        dlq-events: rttm.dlq.events
        queue-metrics: rttm.queue.metrics
        error-events: rttm.error.events
    http:
      base-url: https://rttm.example.com
      api-key: ${RTTM_API_KEY:}
    send-timeout-ms: 3000
    retry:
      max-attempts: 3
      backoff-ms: 100
```

## Creating a client
### Kafka client (preferred when Kafka is reachable)
Load values from `application.yml` (which itself reads env vars with fallbacks) using `@Value`:

```java
@Configuration
public class RttmClientConfiguration {

      @Value("${rttm.client.kafka.bootstrap-servers:${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}}")
      private String kafkaBootstrap;

      @Value("${rttm.client.kafka.topics.trade-events:${RTTM_TRADE_TOPIC:rttm.trade.events}}")
      private String tradeTopic;

      @Value("${rttm.client.kafka.topics.dlq-events:${RTTM_DLQ_TOPIC:rttm.dlq.events}}")
      private String dlqTopic;

      @Value("${rttm.client.kafka.topics.queue-metrics:${RTTM_QUEUE_METRIC_TOPIC:rttm.queue.metrics}}")
      private String queueMetricTopic;

      @Value("${rttm.client.kafka.topics.error-events:${RTTM_ERROR_TOPIC:rttm.error.events}}")
      private String errorTopic;

      @Value("${rttm.client.send-timeout-ms:${RTTM_SEND_TIMEOUT_MS:3000}}")
      private int sendTimeoutMs;

      @Value("${rttm.client.retry.max-attempts:${RTTM_RETRY_MAX_ATTEMPTS:3}}")
      private int retryMaxAttempts;

      @Value("${rttm.client.retry.backoff-ms:${RTTM_RETRY_BACKOFF_MS:100}}")
      private int retryBackoffMs;

      @Bean
      public RttmClient rttmClient(KafkaTemplate<String, MessageLite> kafkaTemplate) {
            RttmClientConfig config = RttmClientConfig.builder()
                        .mode("kafka")
                        .kafkaBootstrapServers(kafkaBootstrap)
                        .kafkaTopicTradeEvents(tradeTopic)
                        .kafkaTopicDlqEvents(dlqTopic)
                        .kafkaTopicQueueMetrics(queueMetricTopic)
                        .kafkaTopicErrorEvents(errorTopic)
                        .sendTimeoutMs(sendTimeoutMs)
                        .retryMaxAttempts(retryMaxAttempts)
                        .retryBackoffMs(retryBackoffMs)
                        .build();

            return KafkaRttmClient.builder()
                        .kafkaTemplate(kafkaTemplate)
                        .config(config)
                        .build();
      }
}
```

Sample `application.yml` fragment that backs the `@Value` bindings (env vars with sensible defaults):

```yaml
rttm:
   client:
      mode: kafka
      kafka:
         bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
         topics:
            trade-events: ${RTTM_TRADE_TOPIC:rttm.trade.events}
            dlq-events: ${RTTM_DLQ_TOPIC:rttm.dlq.events}
            queue-metrics: ${RTTM_QUEUE_METRIC_TOPIC:rttm.queue.metrics}
            error-events: ${RTTM_ERROR_TOPIC:rttm.error.events}
      send-timeout-ms: ${RTTM_SEND_TIMEOUT_MS:3000}
      retry:
         max-attempts: ${RTTM_RETRY_MAX_ATTEMPTS:3}
         backoff-ms: ${RTTM_RETRY_BACKOFF_MS:100}
```

### HTTP client (when Kafka is unavailable)
```java
RttmClientConfig config = RttmClientConfig.builder()
        .mode("http")
        .httpEndpointBaseUrl("https://rttm.example.com")
        .httpApiKey(System.getenv("RTTM_API_KEY"))
        .build();

RttmClient rttmClient = HttpRttmClient.builder()
        .config(config)
        .build();
```

### No-op client (local dev)
```java
RttmClient rttmClient = new NoopRttmClient();
```

## How services should send data
Typical payloads with sample values when invoked from a service (e.g., `pms-validation`):

```java
import com.pms.rttm.client.enums.EventStage;
import com.pms.rttm.client.enums.EventType;

// Trade event (happy path)
rttmClient.sendTradeEvent(TradeEventPayload.builder()
        .tradeId("TR-20250101-0001")
        .serviceName("pms-validation")
        .eventType(EventType.TRADE_VALIDATED)
        .eventStage(EventStage.VALIDATED)
        .eventStatus("OK")
        .sourceQueue("pms.validation.in")
        .targetQueue("pms.validation.out")
        .topicName("rttm.trade.events")
        .consumerGroup("pms-validation-cg")
        .partitionId(0)
        .offsetValue(12345L)
        .message("Trade accepted")
        .build());

// Error event (validation failure)
rttmClient.sendErrorEvent(ErrorEventPayload.builder()
        .tradeId("TR-20250101-0002")
        .serviceName("pms-validation")
        .errorType("VALIDATION_ERROR")
        .errorMessage("Missing notional field")
        .eventStage(EventStage.VALIDATE)
        .build());

// DLQ event (processing failure)
rttmClient.sendDlqEvent(DlqEventPayload.builder()
        .tradeId("TR-20250101-0003")
        .serviceName("pms-validation")
        .topicName("rttm.dlq.events")
        .originalTopic("pms.validation.in")
        .reason("Deserialization error")
        .eventStage(EventStage.CONSUME)
        .build());

// Queue metric (snapshot)
rttmClient.sendQueueMetric(QueueMetricPayload.builder()
        .serviceName("pms-validation")
        .topicName("pms.validation.in")
        .partitionId(0)
        .producedOffset(20000L)
        .consumedOffset(19990L)
        .consumerGroup("pms-validation-cg")
        .build());
```

Notes:
- `EventType` and `EventStage` enums provide type-safe event classification (e.g., `TRADE_VALIDATED`, `ENRICHED`, `VALIDATE`, `CONSUME`).
- All long text fields (message/reason/errorMessage) are auto-truncated to 1000 chars by the DTOs.
- `eventTime`/`snapshotTime` default to `System.currentTimeMillis()` unless explicitly set.
- In Kafka mode, the client uses tradeId or serviceName as keys to keep partitioning stable.

## Unit tests
- Run all tests: `./mvnw test`
- What is covered: DTO truncation safeguards and proto round-trip conversion in `ProtoConverter`.
- Add more coverage: mock `KafkaTemplate` or use `spring-kafka-test` embedded broker for send/ack paths; mock `RestTemplate` for HTTP paths; verify retry/backoff configuration where applicable.

## Manual end-to-end test with `pms-validation`
1. **Start dependencies**
   - Kafka broker reachable at `KAFKA_BOOTSTRAP_SERVERS`.
   - `pms-rttm` service running and pointing to the same Kafka cluster (or HTTP ingestion endpoint if you choose `http` mode).
2. **Configure `pms-validation`**
   - Include the dependency above.
   - Set `rttm.client.mode=kafka` (or `http`).
   - Ensure topics match `pms-rttm`: `rttm.trade.events`, `rttm.dlq.events`, `rttm.queue.metrics`, `rttm.error.events`.
3. **Trigger events from `pms-validation`**
   - Send a trade through the normal flow to emit `TradeEventPayload` (expected on `rttm.trade.events`).
   - Force a validation failure to emit `ErrorEventPayload` and optionally `DlqEventPayload`.
   - Emit a queue metric snapshot to `rttm.queue.metrics`.
4. **Observe results**
   - Kafka mode: use `kafka-console-consumer` (or Spring `@KafkaListener` in a test app) to read each topic and confirm payload fields.
   - HTTP mode: check `pms-rttm` ingress logs/metrics or its persistence layer for received events.
   - Verify message truncation (long message/reason/errorMessage capped at 1000 chars) and timestamps present.
5. **Negative scenarios**
   - Bring Kafka down and confirm the client surfaces `RttmClientException` within the configured timeout.
   - Provide invalid API key in HTTP mode and confirm 401/403 surfaced to the caller.

## Minimal manual test matrix
- Happy path trade event in Kafka mode → message appears on `rttm.trade.events`.
- Error event from validation failure → message appears on `rttm.error.events` with `errorType` and truncated message.
- DLQ event when processing fails → message appears on `rttm.dlq.events` with `originalTopic`.
- Queue metric snapshot → offsets and partition published to `rttm.queue.metrics`.
- HTTP mode smoke test → POST succeeds (2xx) and RTTM receives payload.
- No-op mode → methods return without throwing; only debug logs emitted.

## Troubleshooting
- Missing protobuf serializer: ensure `io.confluent:kafka-protobuf-serializer` is on the runtime classpath.
- Serialization errors: compare payloads against proto contracts under `src/main/proto` and confirm required fields are set.
- Timeouts: raise `send-timeout-ms` and ensure broker address is reachable; check `acks` configuration for sync sends.
- HTTP failures: confirm `base-url` and `X-API-Key` headers; inspect server logs for validation errors.
