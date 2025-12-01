package com.pms.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.validation.dto.IngestionEventDto;
import com.pms.validation.dto.TradeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class IngestionListenerTest {

    private IdempotencyService idempotencyService;
    private IngestionProcessor ingestionProcessor;
    private IngestionListener ingestionListener;
    private ObjectMapper mapper = new ObjectMapper();
    {
        // enable Java time module for LocalDateTime serialization/deserialization
        mapper.findAndRegisterModules();
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @BeforeEach
    public void setup() throws Exception {
        idempotencyService = mock(IdempotencyService.class);
        ingestionProcessor = mock(IngestionProcessor.class);

        ingestionListener = new IngestionListener();
        var f1 = IngestionListener.class.getDeclaredField("idempotencyService");
        f1.setAccessible(true);
        f1.set(ingestionListener, idempotencyService);

        var f2 = IngestionListener.class.getDeclaredField("ingestionProcessor");
        f2.setAccessible(true);
        f2.set(ingestionListener, ingestionProcessor);

        var f3 = IngestionListener.class.getDeclaredField("mapper");
        f3.setAccessible(true);
        f3.set(ingestionListener, mapper);
    }

    @Test
    void whenAlreadyProcessed_delegationSkipped() throws Exception {
        UUID eventId = UUID.randomUUID();
        TradeDto trade = TradeDto.builder()
                .tradeId(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .symbol("AAPL")
                .side(com.pms.validation.enums.TradeSide.BUY)
                .pricePerStock(new java.math.BigDecimal("123.45"))
                .quantity(10L)
                .timestamp(LocalDateTime.now())
                .build();

        IngestionEventDto ingestionEvent = IngestionEventDto.builder()
                .eventId(eventId)
                .payloadBytes(mapper.writeValueAsBytes(trade))
                .createdAt(LocalDateTime.now())
                .build();

        String payload = mapper.writeValueAsString(ingestionEvent);

        when(idempotencyService.isAlreadyProcessed(trade.getTradeId())).thenReturn(true);

        ingestionListener.onMessage(payload, 0, 0L);

        verify(idempotencyService).isAlreadyProcessed(trade.getTradeId());
        verifyNoInteractions(ingestionProcessor);
    }

    @Test
    void whenNotProcessed_delegateCalled() throws Exception {
        UUID eventId = UUID.randomUUID();
        TradeDto trade = TradeDto.builder()
                .tradeId(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .symbol("MSFT")
                .side(com.pms.validation.enums.TradeSide.SELL)
                .pricePerStock(new java.math.BigDecimal("200.00"))
                .quantity(5L)
                .timestamp(LocalDateTime.now())
                .build();

        IngestionEventDto ingestionEvent = IngestionEventDto.builder()
                .eventId(eventId)
                .payloadBytes(mapper.writeValueAsBytes(trade))
                .createdAt(LocalDateTime.now())
                .build();

        String payload = mapper.writeValueAsString(ingestionEvent);

        when(idempotencyService.isAlreadyProcessed(trade.getTradeId())).thenReturn(false);

        ingestionListener.onMessage(payload, 0, 0L);

        verify(ingestionProcessor).process(any(IngestionEventDto.class), any(TradeDto.class));
    }
}
