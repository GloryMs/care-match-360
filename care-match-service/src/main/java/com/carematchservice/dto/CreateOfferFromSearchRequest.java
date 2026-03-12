package com.carematchservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateOfferFromSearchRequest {

    /** Patient profile ID from the search results. */
    @NotNull
    private UUID patientProfileId;

    /** Personalised message to the patient / family. */
    private String message;

    /** Proposed care start date. */
    private LocalDate proposedStartDate;

    /** Monthly fee quoted in EUR. */
    private BigDecimal monthlyFee;

    /** List of services included in the quoted fee. */
    private List<String> includedServices;

    /** Offer expiry date. */
    private LocalDate validUntil;
}