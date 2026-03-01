package com.carematchservice.dto;

import lombok.Data;

@Data
public class DeclineCareRequestRequest {
    /** Optional explanation from the provider. Max 500 chars. */
    private String declineReason;
}