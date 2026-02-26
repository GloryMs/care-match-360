package com.carenotificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSummaryDTO {
    private UUID id;
    private UUID userId;
    private String email;
}
