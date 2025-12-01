package com.pms.validation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionEventDto {
    private UUID eventId;
    private String aggregateId;
    private String eventType;
    private byte[] payloadBytes;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
