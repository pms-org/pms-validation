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
    List<TradeEventProto> tradeProtos;
    Acknowledgment ack;
}
