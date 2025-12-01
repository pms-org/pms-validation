package com.pms.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.validation.dto.IngestionEventDto;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.enums.TradeSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KafkaConsumerService}.
 * Tests the legacy Kafka consumer behavior (same ingestion-topic as
 * IngestionListener).
 */
class KafkaConsumerServiceTest {

    private IdempotencyService idempotencyService;
    private ValidationCore ingestionProcessor;
    private ObjectMapper mapper;
    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    public void setup() throws Exception {
        idempotencyService = mock(IdempotencyService.class);
        ingestionProcessor = mock(ValidationCore.class);

        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        kafkaConsumerService = new KafkaConsumerService();

        // Inject mocks via reflection
        var f1 = KafkaConsumerService.class.getDeclaredField("idempotencyService");
        f1.setAccessible(true);
        f1.set(kafkaConsumerService, idempotencyService);

        var f2 = KafkaConsumerService.class.getDeclaredField("ingestionProcessor");
        f2.setAccessible(true);
        f2.set(kafkaConsumerService, ingestionProcessor);

        var f3 = KafkaConsumerService.class.getDeclaredField("mapper");
        f3.setAccessible(true);
        f3.set(kafkaConsumerService, mapper);
    }

    @Test
    void whenDuplicate_ignoresMessage() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID tradeId = UUID.randomUUID();

        TradeDto trade = TradeDto.builder()
                .tradeId(tradeId)
                .portfolioId(UUID.randomUUID())
                .symbol("AAPL")
                .side(TradeSide.BUY)
                .pricePerStock(new BigDecimal("123.45"))
                .quantity(10L)
                .timestamp(LocalDateTime.now())
                .build();

        IngestionEventDto ingestionEvent = IngestionEventDto.builder()
                .eventId(eventId)
                .payloadBytes(mapper.writeValueAsBytes(trade))
                .createdAt(LocalDateTime.now())
                .build();

        String payload = mapper.writeValueAsString(ingestionEvent);

        // Duplicate check returns true
        when(idempotencyService.isAlreadyProcessed(tradeId)).thenReturn(true);

        kafkaConsumerService.onMessage(payload, 0, 0L);

        // Verify idempotency check was performed
        verify(idempotencyService).isAlreadyProcessed(tradeId);
        // Verify no processing occurred
        verifyNoInteractions(ingestionProcessor);
    }

    @Test
    void whenNotProcessed_delegatesToProcessor() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID tradeId = UUID.randomUUID();

        TradeDto trade = TradeDto.builder()
                .tradeId(tradeId)
                .portfolioId(UUID.randomUUID())
                .symbol("MSFT")
                .side(TradeSide.SELL)
                .pricePerStock(new BigDecimal("200.00"))
                .quantity(5L)
                .timestamp(LocalDateTime.now())
                .build();

        IngestionEventDto ingestionEvent = IngestionEventDto.builder()
                .eventId(eventId)
                .payloadBytes(mapper.writeValueAsBytes(trade))
                .createdAt(LocalDateTime.now())
                .build();

        String payload = mapper.writeValueAsString(ingestionEvent);

        // Not a duplicate
        when(idempotencyService.isAlreadyProcessed(tradeId)).thenReturn(false);

        kafkaConsumerService.onMessage(payload, 0, 0L);

        // Verify idempotency check and processor delegation
        verify(idempotencyService).isAlreadyProcessed(tradeId);
        verify(ingestionProcessor).process(any(IngestionEventDto.class), any(TradeDto.class));
    }
}
