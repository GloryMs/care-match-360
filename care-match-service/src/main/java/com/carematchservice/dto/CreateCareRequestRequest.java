package com.carematchservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCareRequestRequest {

    @NotNull(message = "providerId is required")
    private UUID providerId;

    /** Optional message from the patient/relative to the provider. Max 1000 chars. */
    private String patientMessage;
}
