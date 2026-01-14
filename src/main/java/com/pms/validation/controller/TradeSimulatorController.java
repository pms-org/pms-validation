package com.pms.validation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Random;
import com.pms.validation.enums.TradeSide;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.event.KafkaProducerService;

@RestController
@RequestMapping("/trade-simulator")
public class TradeSimulatorController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @PostMapping("/simulate")
    public void simulateTrade(@RequestBody TradeDto tradeDto) {
        try {
            kafkaProducerService.sendIngestionEvent(tradeDto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/simulate-batch")
    public void simulateBatch(@RequestBody List<TradeDto> trades) {
        for (TradeDto tradeDto : trades) {
            try {
                kafkaProducerService.sendIngestionEvent(tradeDto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @PostMapping("/simulate-generate")
    public void simulateGenerate(@RequestBody(required = false) GenerateRequest req) {
        int n = 10;
        String portfolioIdStr = null;
        if (req != null) {
            if (req.count != null && req.count > 0) n = req.count;
            portfolioIdStr = req.portfolioId;
        }

        UUID portfolio;
        if (portfolioIdStr == null || portfolioIdStr.isBlank()) {
            portfolio = UUID.randomUUID();
        } else {
            try {
                portfolio = UUID.fromString(portfolioIdStr);
            } catch (IllegalArgumentException ex) {
                // allow non-UUID strings by generating a name-based UUID
                portfolio = UUID.nameUUIDFromBytes(portfolioIdStr.getBytes());
            }
        }
        Random rnd = new Random();

        for (int i = 0; i < n; i++) {
            TradeDto trade = TradeDto.builder()
                    .tradeId(UUID.randomUUID())
                    .portfolioId(portfolio)
                    .symbol("SYM" + (rnd.nextInt(10) + 1))
                    .side(rnd.nextBoolean() ? TradeSide.BUY : TradeSide.SELL)
                    .pricePerStock(BigDecimal.valueOf(100 + rnd.nextDouble() * 50))
                    .quantity((long) (1 + rnd.nextInt(100)))
                    .timestamp(LocalDateTime.now())
                    .build();

            try {
                kafkaProducerService.sendIngestionEvent(trade);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Request body for simulate-generate. kept simple so controller doesn't need a new file.
    public static class GenerateRequest {
        public Integer count;
        public String portfolioId;

        public GenerateRequest() {
        }
    }
}
