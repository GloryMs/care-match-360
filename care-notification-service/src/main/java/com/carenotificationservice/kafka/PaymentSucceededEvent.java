package com.carenotificationservice.kafka;

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
public class PaymentSucceededEvent {
    private String eventType;
    private UUID subscriptionId;
    private UUID invoiceId;
    private UUID providerId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
}
