# PMS RTTM Client Integration Guide for pms-validation Service

This guide walks you through integrating and using the `pms-rttm-client` library in the pms-validation service.

## Prerequisites

- `pms-rttm-client` v1.0.0 installed locally (`./mvnw clean install`)
- Maven dependency already added to `pom.xml`

## Step 1: Add Maven Dependency to pms-validation

Ensure this is in your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.pms-org</groupId>
    <artifactId>pms-rttm-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Step 2: Configure application.yml

Add the following to `src/main/resources/application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer
      acks: ${KAFKA_PRODUCER_ACKS:all}
      retries: ${KAFKA_PRODUCER_RETRIES:3}

rttm:
  client:
    mode: ${RTTM_MODE:kafka}  # kafka | http | noop
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
      topics:
        trade-events: ${KAFKA_TOPIC_TRADE_EVENTS:rttm.trade.events}
        dlq-events: ${KAFKA_TOPIC_DLQ_EVENTS:rttm.dlq.events}
        queue-metrics: ${KAFKA_TOPIC_QUEUE_METRICS:rttm.queue.metrics}
        error-events: ${KAFKA_TOPIC_ERROR_EVENTS:rttm.error.events}
    send-timeout-ms: ${RTTM_SEND_TIMEOUT_MS:3000}
    retry:
      max-attempts: ${RTTM_RETRY_MAX_ATTEMPTS:3}
      backoff-ms: ${RTTM_RETRY_BACKOFF_MS:100}
```

## Step 3: Create RttmClient Configuration Bean

Create `src/main/java/com/pms/validation/config/RttmClientConfiguration.java`:

```java
package com.pms.validation.config;

import com.google.protobuf.MessageLite;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import com.pms.rttm.client.clients.KafkaRttmClient;
import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.config.RttmClientConfig;

@Configuration
public class RttmClientConfiguration {

    @Bean
    public RttmClient rttmClient(
            KafkaTemplate<String, MessageLite> kafkaTemplate,
            RttmClientProperties properties) {
        
        RttmClientConfig config = RttmClientConfig.builder()
                .mode(properties.getMode())
                .kafkaBootstrapServers(properties.getKafka().getBootstrapServers())
                .kafkaTopicTradeEvents(properties.getKafka().getTopics().getTradeEvents())
                .kafkaTopicDlqEvents(properties.getKafka().getTopics().getDlqEvents())
                .kafkaTopicQueueMetrics(properties.getKafka().getTopics().getQueueMetrics())
                .kafkaTopicErrorEvents(properties.getKafka().getTopics().getErrorEvents())
                .sendTimeoutMs(properties.getSendTimeoutMs())
                .retryMaxAttempts(properties.getRetry().getMaxAttempts())
                .retryBackoffMs(properties.getRetry().getBackoffMs())
                .build();

        return KafkaRttmClient.builder()
                .kafkaTemplate(kafkaTemplate)
                .config(config)
                .build();
    }
}
```

Create `src/main/java/com/pms/validation/config/RttmClientProperties.java`:

```java
package com.pms.validation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rttm.client")
@Data
public class RttmClientProperties {
    
    private String mode;
    private long sendTimeoutMs;
    private Kafka kafka = new Kafka();
    private Retry retry = new Retry();

    @Data
    public static class Kafka {
        private String bootstrapServers;
        private Topics topics = new Topics();

        @Data
        public static class Topics {
            private String tradeEvents;
            private String dlqEvents;
            private String queueMetrics;
            private String errorEvents;
        }
    }

    @Data
    public static class Retry {
        private int maxAttempts;
        private long backoffMs;
    }
}
```

## Step 4: Use RttmClient in Your Service

Inject and use `RttmClient` in any service:

```java
package com.pms.validation.service;

import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.dto.TradeEventPayload;
import com.pms.rttm.client.dto.ErrorEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final RttmClient rttmClient;

    public void validateAndPublishTrade(TradeRequest request) {
        // Your validation logic
        
        // Publish successful trade event
        TradeEventPayload tradeEvent = TradeEventPayload.builder()
                .tradeId(request.getTradeId())
                .symbol(request.getSymbol())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .timestamp(System.currentTimeMillis())
                .build();
        
        rttmClient.sendTradeEvent(tradeEvent);
    }

    public void publishValidationError(String tradeId, String errorMessage) {
        ErrorEventPayload errorEvent = ErrorEventPayload.builder()
                .tradeId(tradeId)
                .errorCode("VALIDATION_ERROR")
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
        
        rttmClient.sendErrorEvent(errorEvent);
    }
}
```

## Step 5: Available RttmClient Methods

```java
// Send trade event
rttmClient.sendTradeEvent(TradeEventPayload payload);

// Send DLQ event
rttmClient.sendDlqEvent(DlqEventPayload payload);

// Send queue metric
rttmClient.sendQueueMetric(QueueMetricPayload payload);

// Send error event
rttmClient.sendErrorEvent(ErrorEventPayload payload);
```

## Step 6: Local Testing

### Option A: Using noop Mode (No Dependencies)
Set environment variable:
```bash
RTTM_MODE=noop
```

This disables Kafka publishing for local testing.

### Option B: Using Local Kafka

**Start Kafka locally:**
```bash
docker run -d \
  --name zookeeper \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  confluentinc/cp-zookeeper:7.5.0

docker run -d \
  --name kafka \
  -p 9092:9092 \
  --link zookeeper:zookeeper \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT \
  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
  confluentinc/cp-kafka:7.5.0
```

**Run pms-validation:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--KAFKA_BOOTSTRAP_SERVERS=localhost:9092 --RTTM_MODE=kafka"
```

### Option C: Integration Tests

Create `src/test/java/com/pms/validation/service/ValidationServiceRttmTest.java`:

```java
package com.pms.validation.service;

import com.pms.rttm.client.clients.RttmClient;
import com.pms.rttm.client.dto.TradeEventPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ValidationServiceRttmTest {

    @Autowired
    private ValidationService validationService;

    @Test
    void testTradeValidationPublishesEvent() {
        // Arrange
        TradeRequest request = TradeRequest.builder()
                .tradeId("TRADE-001")
                .symbol("AAPL")
                .quantity(100)
                .price(150.5)
                .build();

        // Act
        validationService.validateAndPublishTrade(request);

        // Assert - verify event was sent (using mock if needed)
    }
}
```

**Test configuration in `src/test/resources/application-test.yml`:**

```yaml
rttm:
  client:
    mode: noop  # Disable Kafka in tests
```

## Step 7: Environment Variables Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `RTTM_MODE` | kafka | Mode: kafka, http, or noop |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka broker address |
| `KAFKA_TOPIC_TRADE_EVENTS` | rttm.trade.events | Trade events topic |
| `KAFKA_TOPIC_DLQ_EVENTS` | rttm.dlq.events | DLQ events topic |
| `KAFKA_TOPIC_QUEUE_METRICS` | rttm.queue.metrics | Queue metrics topic |
| `KAFKA_TOPIC_ERROR_EVENTS` | rttm.error.events | Error events topic |
| `RTTM_SEND_TIMEOUT_MS` | 3000 | Send operation timeout |
| `RTTM_RETRY_MAX_ATTEMPTS` | 3 | Max retry attempts |
| `RTTM_RETRY_BACKOFF_MS` | 100 | Backoff between retries |

## Step 8: Troubleshooting

### Issue: "RttmClient bean not found"
- Ensure `@SpringBootApplication` scans the package with `RttmClientConfiguration`
- Add `@ComponentScan` if needed: `@ComponentScan(basePackages = {"com.pms.validation", "com.pms.rttm.client"})`

### Issue: "KafkaTemplate not found"
- Add Spring Kafka dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-kafka</artifactId>
</dependency>
```

### Issue: "Protobuf serialization errors"
- Ensure Confluent protobuf serializer is in classpath (already included in pms-rttm-client)
- Verify `spring.kafka.producer.value-serializer` is set to `io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer`

### Issue: "Messages not appearing in Kafka"
- Check Kafka is running: `docker ps | grep kafka`
- Verify `KAFKA_BOOTSTRAP_SERVERS` is correct
- Check topics exist: `kafka-topics --list --bootstrap-server localhost:9092`
- Enable debug logging: Add `logging.level.com.pms.rttm.client=DEBUG` to `application.yml`

## Next Steps

1. Create the configuration classes (RttmClientConfiguration, RttmClientProperties)
2. Update `application.yml` with rttm settings
3. Inject `RttmClient` into your validation service
4. Call `rttmClient.sendTradeEvent()`, `sendErrorEvent()`, etc.
5. Test with `RTTM_MODE=noop` first (no dependencies)
6. Switch to Kafka mode and test with local Kafka instance
