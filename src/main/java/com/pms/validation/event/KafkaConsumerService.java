package com.pms.validation.event;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.exception.RetryableException;
import com.pms.validation.mapper.ProtoDTOMapper;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.service.TradeIdempotencyService;
import com.pms.validation.service.TradeProcessingService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaConsumerService {

    @Autowired
    private TradeProcessingService tradeProcessingService;

    @Autowired
    private TradeIdempotencyService tradeIdempotencyService;

    @RetryableTopic(attempts = "5", include = {
            RetryableException.class // only retry 5x if RetryableException, Other exceptions = straight DLT
    }, backoff = @Backoff(delay = 2000, multiplier = 2) // retry 1 = 2sec, 2 = 4sec, 3 = 8sec, 4 = 16sec
    )
    @KafkaListener(topics = "${app.incoming-trades-topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "protobufKafkaListenerContainerFactory")
    public void onTradeIngestion(
            TradeEventProto tradeMessage,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        TradeDto tradeDto = ProtoDTOMapper.toDto(tradeMessage);
        UUID tradeId = tradeDto.getTradeId();

        log.info("Received trade | tradeId={} partition={} offset={}",
                tradeId, partition, offset);

        // Check if already processed
        if (tradeIdempotencyService.isDone(tradeId)) {
            log.info("Trade already DONE, skipping | tradeId={}", tradeId);
            return;
        }

        // Try to acquire processing lock
        if (!tradeIdempotencyService.tryStartProcessing(tradeId)) {
            log.warn("Trade already PROCESSING, retry later | tradeId={}", tradeId);
            throw new RetryableException("Trade locked: " + tradeId);
        }

        log.info("Processing lock acquired | tradeId={}", tradeId);

        try {
            tradeProcessingService.processTrade(tradeDto);

            tradeIdempotencyService.markDone(tradeId);

            log.info("Trade processed successfully | tradeId={}", tradeId);

        } catch (Exception ex) {
            log.error("Trade processing failed, will retry | tradeId={}", tradeId, ex);
            throw ex; // RetryableTopic handles retry / DLT
        }
    }

    @DltHandler
    public void handleDltMessage(
            TradeEventProto dltMessage,
            @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.ORIGINAL_PARTITION) int partition,
            @Header(KafkaHeaders.ORIGINAL_OFFSET) long offset) {

        log.error("Trade moved to DLT | DLT message={} topic={} partition={} offset={}",
                dltMessage, originalTopic, partition, offset);
    }

}
