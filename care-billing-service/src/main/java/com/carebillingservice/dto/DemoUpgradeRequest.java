package com.carebillingservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for demo tier upgrade/downgrade simulation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoUpgradeRequest {

    @NotBlank(message = "New tier is required (BASIC | PRO | PREMIUM)")
    private String newTier;
}
