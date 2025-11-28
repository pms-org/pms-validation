package com.pms.validation.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.pms.validation.dao.ValidationOutboxDao;
import com.pms.validation.entity.ValidationOutbox;

@Service
public class ValidationOutboxService {

    @Autowired
    private RedisTemplate<String, UUID> redisTemplate;

    @Autowired
    private KafkaTemplate<String, ValidationOutbox> kafkaTemplate;

    @Autowired
    private ValidationOutboxDao validationOutboxDao;

    private static final String TRADE_SET = "ProcessedTrades";
    
    public void validateTrade(ValidationOutbox validationOutbox) {

        if(isDuplicateTrade(validationOutbox.getTradeId()))
        {
            System.out.println("Duplicate trade detected: " + validationOutbox.getTradeId());
            return;
        }

        validationOutbox.setStatus("PENDING");

        ValidationOutbox savedOutbox = validationOutboxDao.save(validationOutbox);

        System.out.println("Saved to DB: " + savedOutbox);

        kafkaTemplate.send("validation-topic",savedOutbox.getPortfolioId().toString(), savedOutbox);
            // .addCallback(success -> {
            //     System.out.println("Published to Kafka: " + savedOutbox);
                
            // },
            // failure -> {
            
            // });

    }

    public boolean isDuplicateTrade(UUID tradeId) {

        boolean exists = redisTemplate.opsForSet().isMember(TRADE_SET, tradeId);

        if (exists) {
            return true;
        }

        redisTemplate.opsForSet().add(TRADE_SET, tradeId);
        return false;
    }
}
