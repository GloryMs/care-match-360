package com.carenotificationservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferAcceptedEvent {
    private String eventType;
    private UUID offerId;
    private UUID patientId;
    private UUID providerId;
    private LocalDateTime timestamp;
}
