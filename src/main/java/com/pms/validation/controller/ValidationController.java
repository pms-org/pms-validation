package com.pms.validation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pms.validation.entity.ValidationOutbox;
import com.pms.validation.service.ValidationOutboxService;



@RestController
@RequestMapping("/validation")
public class ValidationController {

    @Autowired
    private ValidationOutboxService validationOutboxService;

    @Autowired
    private KafkaTemplate<String, ValidationOutbox> kafkaTemplate;
    
    @PostMapping("/validate/trade")
    public void validateTrade(@RequestBody ValidationOutbox validationOutbox) {
        // ValidationOutbox validatedTrade = validationOutboxService.validateTrade(validationOutbox);
        // return ResponseEntity.ok(validatedTrade);

        kafkaTemplate.send("ingestion-topic", validationOutbox.getPortfolioId().toString(), validationOutbox);
    }

}
