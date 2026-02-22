package com.carematchservice.kafka.events;

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
public class OfferRejectedEvent {
    private String eventType = "offer.rejected";
    private UUID offerId;
    private UUID patientId;
    private UUID providerId;
    private LocalDateTime timestamp;
}
