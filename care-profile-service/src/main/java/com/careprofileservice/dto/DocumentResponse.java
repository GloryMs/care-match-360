package com.careprofileservice.dto;


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
public class DocumentResponse {
    private UUID id;
    private UUID profileId;
    private String profileType;
    private String documentType;
    private String fileName;
    private String fileUrl;
    private String presignedUrl; // Temporary access URL
    private Long fileSize;
    private String mimeType;
    private LocalDateTime uploadedAt;
}
