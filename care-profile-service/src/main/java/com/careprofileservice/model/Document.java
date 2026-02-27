package com.careprofileservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists document metadata.
 *
 * fileUrl  – public download URL (built from fileKey)
 * fileKey  – relative storage key used by FileStorageService (e.g. "patients/<uuid>/file.pdf")
 *            Needed so we can delete the physical file when the record is removed.
 */
@Entity
@Table(name = "documents", schema = "care_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_type", nullable = false, length = 20)
    private ProfileType profileType;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Public download URL served by FileDownloadController. */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /**
     * Logical storage key (relative path under base-dir, without .gz.enc suffix).
     * New column — add via migration:
     *   ALTER TABLE care_profiles.documents ADD COLUMN file_key VARCHAR(500);
     */
    @Column(name = "file_key", length = 500)
    private String fileKey;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "uploaded_at", nullable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public enum ProfileType {
        PATIENT,
        PROVIDER
    }
}