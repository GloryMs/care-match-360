package com.carematchservice.dto;


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
public class OfferHistoryResponse {
    private UUID id;
    private UUID offerId;
    private String oldStatus;
    private String newStatus;
    private UUID changedBy;
    private LocalDateTime changedAt;
    private String notes;
}
