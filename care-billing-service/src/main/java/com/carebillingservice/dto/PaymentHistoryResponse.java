package com.carebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {
    private UUID id;
    private UUID invoiceId;
    private UUID subscriptionId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String failureReason;
    private LocalDateTime processedAt;
}
