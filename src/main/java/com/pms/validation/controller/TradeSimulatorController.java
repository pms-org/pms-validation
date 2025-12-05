package com.pms.validation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.event.KafkaProducerService;

@RestController
@RequestMapping("/trade-simulator")
public class TradeSimulatorController {

    @Autowired
    private KafkaProducerService kafkaProducerService;
    
    @PostMapping("/simulate")
    public void simulateTrade(@RequestBody TradeDto tradeDto) {
        // Simulation logic here
        try {
            kafkaProducerService.sendIngestionEvent(tradeDto);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
