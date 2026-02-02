package com.pms.validation.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.mapper.ProtoDTOMapper;
import com.pms.validation.mapper.ProtoEntityMapper;
import com.pms.validation.mapper.ProtoInvalidTradeEntityMapper;
import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.proto.InvalidTradeEventProto;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, TradeEventProto> kafkaTemplate;

    @Autowired
    private KafkaTemplate<String, InvalidTradeEventProto> invalidTradeKafkaTemplate;

    @Value("${app.incoming-trades-topic}")
    private String incomingTradesTopic;

    @Value("${app.outgoing-valid-trades-topic}")
    private String validTradesTopic;

    @Value("${app.outgoing-invalid-trades-topic}")
    private String invalidTradesTopic;

    public void sendIngestionEvent(TradeDto tradeDto) throws Exception {

        TradeEventProto protoEvent = ProtoDTOMapper.toProto(tradeDto);

        kafkaTemplate.send(incomingTradesTopic, protoEvent.getPortfolioId(), protoEvent).get();
    }

    public void sendValidationEvent(ValidationOutboxEntity event) throws Exception {

        TradeEventProto protoEvent = ProtoEntityMapper.toProto(event);

        kafkaTemplate.send(validTradesTopic, protoEvent.getPortfolioId(), protoEvent).get();
    }

    public void sendInvalidTradeEvent(InvalidTradeEntity event) throws Exception {

        InvalidTradeEventProto protoEvent = ProtoInvalidTradeEntityMapper.toProto(event);

        invalidTradeKafkaTemplate.send(invalidTradesTopic, protoEvent.getPortfolioId(), protoEvent).get();
    }
}
