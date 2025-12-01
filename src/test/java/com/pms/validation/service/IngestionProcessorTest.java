package com.pms.validation.service;

import com.pms.validation.dto.IngestionEventDto;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationOutputDto;
import com.pms.validation.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// No direct assertions used; test verifies interactions via Mockito
import static org.mockito.Mockito.*;

public class IngestionProcessorTest {

    private IdempotencyService idempotencyService;
    private TradeValidationService tradeValidationService;
    private ValidationOutboxService validationOutboxService;
    private KafkaProducerService kafkaProducerService;
    private IngestionProcessor ingestionProcessor;

    @BeforeEach
    public void setup() {
        idempotencyService = mock(IdempotencyService.class);
        tradeValidationService = mock(TradeValidationService.class);
        validationOutboxService = mock(ValidationOutboxService.class);
        kafkaProducerService = mock(KafkaProducerService.class);

        ingestionProcessor = new IngestionProcessor();
        // inject mocks via reflection
        try {
            var f1 = IngestionProcessor.class.getDeclaredField("idempotencyService");
            f1.setAccessible(true);
            f1.set(ingestionProcessor, idempotencyService);

            var f2 = IngestionProcessor.class.getDeclaredField("tradeValidationService");
            f2.setAccessible(true);
            f2.set(ingestionProcessor, tradeValidationService);

            var f3 = IngestionProcessor.class.getDeclaredField("validationOutboxService");
            f3.setAccessible(true);
            f3.set(ingestionProcessor, validationOutboxService);

            var f4 = IngestionProcessor.class.getDeclaredField("kafkaProducerService");
            f4.setAccessible(true);
            f4.set(ingestionProcessor, kafkaProducerService);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void happyPath_processesAndPublishes() {
        UUID eventId = UUID.randomUUID();
        IngestionEventDto ingestionEvent = IngestionEventDto.builder()
                .eventId(eventId)
                .payloadBytes(new byte[0])
                .build();

        TradeDto trade = TradeDto.builder()
                .tradeId(UUID.randomUUID())
                .portfolioId(UUID.randomUUID())
                .symbol("ABC")
                .side(null)
                .pricePerStock(new BigDecimal("10.00"))
                .quantity(100L)
                .timestamp(LocalDateTime.now())
                .build();

        when(idempotencyService.markAsProcessed(eq(eventId), anyString())).thenReturn(true);

        ValidationResult vr = new ValidationResult();
        when(tradeValidationService.validateTrade(trade)).thenReturn(vr);

        ValidationOutputDto output = ValidationOutputDto.builder()
                .eventId(UUID.randomUUID())
                .tradeId(trade.getTradeId())
                .portfolioId(trade.getPortfolioId())
                .symbol(trade.getSymbol())
                .pricePerStock(trade.getPricePerStock().toPlainString())
                .processedAt(LocalDateTime.now())
                .validationStatus("SUCCESS")
                .build();

        when(validationOutboxService.buildValidationEvent(trade, vr)).thenReturn(output);
        when(validationOutboxService.saveValidationEvent(trade, vr, "SUCCESS")).thenReturn(null);

        ingestionProcessor.process(ingestionEvent, trade);

        verify(idempotencyService).markAsProcessed(eq(eventId), anyString());
        verify(tradeValidationService).validateTrade(trade);
        verify(validationOutboxService).buildValidationEvent(trade, vr);
        verify(validationOutboxService).saveValidationEvent(trade, vr, "SUCCESS");
        verify(kafkaProducerService).sendValidationEvent(eq("validation-topic"), eq(output));
    }

    @Test
    void duplicate_markFails_noProcessing() {
        UUID eventId = UUID.randomUUID();
        IngestionEventDto ingestionEvent = IngestionEventDto.builder()
                .eventId(eventId)
                .payloadBytes(new byte[0])
                .build();

        TradeDto trade = TradeDto.builder().tradeId(UUID.randomUUID()).build();

        when(idempotencyService.markAsProcessed(eq(eventId), anyString())).thenReturn(false);

        ingestionProcessor.process(ingestionEvent, trade);

        verify(idempotencyService).markAsProcessed(eq(eventId), anyString());
        verifyNoInteractions(tradeValidationService);
        verifyNoInteractions(validationOutboxService);
        verifyNoInteractions(kafkaProducerService);
    }
}
