package com.pms.validation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.mapper.ProtoEntityMapper;
import com.pms.validation.mapper.ProtoInvalidTradeEntityMapper;
import com.pms.validation.proto.TradeEventProto;

@Service
public class KafkaProducerService{

    @Autowired
    private KafkaTemplate<String, TradeEventProto> kafkaTemplate;

    @Autowired
    private ProtoEntityMapper protoEntityMapper;

    @Autowired
    private ProtoInvalidTradeEntityMapper protoInvalidTradeEntityMapper;

    private static final String validationTopic = "validation-topic";

    private static final String invalidTradeTopic = "invalid-trade-topic";

    public void sendValidationEvent(ValidationOutboxEntity event) throws Exception {

        TradeEventProto protoEvent = protoEntityMapper.toProto(event);

        // byte[] eventBytes = protoEvent.toByteArray();

        kafkaTemplate.send(validationTopic, protoEvent.getPortfolioId(), protoEvent).get();
    }

    public void sendInvalidTradeEvent(InvalidTradeEntity event) throws Exception {

        TradeEventProto protoEvent = protoInvalidTradeEntityMapper.toProto(event);

        // byte[] eventBytes = protoEvent.toByteArray();

        kafkaTemplate.send(invalidTradeTopic, protoEvent.getPortfolioId(), protoEvent).get();
    }
}
