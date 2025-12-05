package com.pms.validation.event;

import org.kie.api.prototype.PrototypeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.mapper.ProtoDTOMapper;
import com.pms.validation.mapper.ProtoEntityMapper;
import com.pms.validation.mapper.ProtoInvalidTradeEntityMapper;
import com.pms.validation.proto.TradeEventProto;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, TradeEventProto> kafkaTemplate;

    private static final String validationTopic = "validation-topic";

    private static final String invalidTradeTopic = "invalid-trade-topic";

    public void sendIngestionEvent(TradeDto tradeDto) throws Exception {

        TradeEventProto protoEvent = ProtoDTOMapper.toProto(tradeDto);

        kafkaTemplate.send("ingestion-topic", protoEvent.getPortfolioId(), protoEvent).get();
    }

    public void sendValidationEvent(ValidationOutboxEntity event) throws Exception {

        TradeEventProto protoEvent = ProtoEntityMapper.toProto(event);

        // byte[] eventBytes = protoEvent.toByteArray();

        kafkaTemplate.send(validationTopic, protoEvent.getPortfolioId(), protoEvent).get();
    }

    public void sendInvalidTradeEvent(InvalidTradeEntity event) throws Exception {

        TradeEventProto protoEvent = ProtoInvalidTradeEntityMapper.toProto(event);

        // byte[] eventBytes = protoEvent.toByteArray();

        kafkaTemplate.send(invalidTradeTopic, protoEvent.getPortfolioId(), protoEvent).get();
    }
}
