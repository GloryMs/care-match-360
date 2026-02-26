package com.carecommon.kafkaEvents;

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
public class AccountVerifiedEvent {
    private UUID userId;
    private String email;
    private String role; // PATIENT, RELATIVE, RESIDENTIAL_PROVIDER, AMBULATORY_PROVIDER, ADMIN, SUPER_ADMIN
    private LocalDateTime timestamp;
}
