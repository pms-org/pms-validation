package com.pms.validation.wrapper;

import java.util.List;

import org.springframework.kafka.support.Acknowledgment;

import com.pms.validation.proto.TradeEventProto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PollBatch {
    private List<TradeEventProto> tradeProtos;
    private Acknowledgment ack;
    private int partition;
    private String topic;
    private List<Long> offsets;
    private String consumerGroup;
}
