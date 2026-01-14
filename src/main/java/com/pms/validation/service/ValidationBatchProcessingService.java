package com.pms.validation.service;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.mapper.ProtoDTOMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationBatchProcessingService {

    @Autowired
    private TradeIdempotencyService idempotencyService;

    @Autowired
    private TradeProcessingService tradeProcessingService;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void processBatch(List<TradeEventProto> messages) {
        log.info("Processing validation batch of {} trades.", messages.size());

        List<TradeDto> dtos = messages.stream()
                .map(ProtoDTOMapper::toDto)
                .collect(Collectors.toList());

        // collect ids which were successfully processed in this transaction
        List<java.util.UUID> successfulIds = new ArrayList<>();

        for (TradeDto dto : dtos) {
            // idempotency check
            if (dto.getTradeId() == null) {
                log.warn("Skipping trade with null id");
                continue;
            }

            if (idempotencyService.isDone(dto.getTradeId())) {
                log.info("Trade already done, skipping | tradeId={}", dto.getTradeId());
                continue;
            }

            try {
                tradeProcessingService.processTrade(dto);
                // Do NOT mark processed here. Defer to afterCommit callback so we only mark
                // in Redis after the DB transaction has successfully committed.
                successfulIds.add(dto.getTradeId());
            } catch (Exception ex) {
                log.error("Error processing trade {} in batch", dto.getTradeId(), ex);
                throw ex; // let listener pause and handle
            }
        }

        // Register afterCommit callback to mark processed IDs in Redis only after commit
        if (!successfulIds.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (java.util.UUID id : successfulIds) {
                        try {
                            idempotencyService.markDone(id);
                        } catch (Exception ex) {
                            log.error("Failed to mark idempotency DONE for {}", id, ex);
                        }
                    }
                }
            });
        }

        try {
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/position-update", dtos);
            } else {
                log.debug("SimpMessagingTemplate not present, skipping websocket update");
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to send websocket update", ex);
        }
    }
}
