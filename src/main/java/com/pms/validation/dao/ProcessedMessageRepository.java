package com.pms.validation.dao;

import com.pms.validation.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {
    Optional<ProcessedMessage> findByTradeIdAndConsumerGroup(UUID tradeId, String consumerGroup);

    boolean existsByTradeIdAndConsumerGroup(UUID tradeId, String consumerGroup);
}
