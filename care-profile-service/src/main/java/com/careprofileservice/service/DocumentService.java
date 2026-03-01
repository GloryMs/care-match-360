package com.careprofileservice.service;

import com.carecommon.exception.ResourceNotFoundException;
import com.careprofileservice.dto.DocumentResponse;
import com.careprofileservice.mapper.DocumentMapper;
import com.careprofileservice.model.Document;
import com.careprofileservice.repository.DocumentRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates document metadata persistence and delegates actual file storage
 * to {@link FileStorageService} (local disk, compressed + AES-256-GCM encrypted).
 *
 * CHANGES vs previous version:
 *   • uploadProviderDocument() — new method that enforces:
 *       - FACILITY_MEDIA: max 10 attachments per provider, max 5 MB per file
 *       - Other types: existing limits (configured max-file-size in properties)
 *   • getFacilityMedia()       — returns only FACILITY_MEDIA docs for a provider
 *   • getPublicProviderMedia() — alias used by the public detail endpoint
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final String FACILITY_MEDIA_TYPE = "FACILITY_MEDIA";
    private static final int    FACILITY_MEDIA_MAX_COUNT = 10;
    private static final long   FACILITY_MEDIA_MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private final DocumentRepository documentRepository;
    private final FileStorageService  fileStorageService;
    private final DocumentMapper      documentMapper;

    // ── Generic upload (existing behaviour) ───────────────────────────────────

    @Transactional
    public DocumentResponse uploadDocument(
            UUID profileId,
            Document.ProfileType profileType,
            String documentType,
            MultipartFile file) {

        String folder   = profileType.name().toLowerCase() + "s/" + profileId;
        String fileType = determineFileType(documentType, file.getContentType());

        String fileKey = fileStorageService.uploadFile(file, folder, fileType);
        String fileUrl = fileStorageService.getPublicUrl(fileKey);

        Document document = Document.builder()
                .profileId(profileId)
                .profileType(profileType)
                .documentType(documentType)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileKey(fileKey)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();

        Document saved = documentRepository.save(document);
        log.info("Document saved: id={}, profileId={}, type={}", saved.getId(), profileId, documentType);

        DocumentResponse response = documentMapper.toResponse(saved);
        response.setPresignedUrl(fileUrl);
        return response;
    }

    // ── NEW: Provider-specific upload with FACILITY_MEDIA caps (Requirement 3) ─

    @Transactional
    public DocumentResponse uploadProviderDocument(
            UUID providerId,
            String documentType,
            MultipartFile file) {

        boolean isFacilityMedia = FACILITY_MEDIA_TYPE.equalsIgnoreCase(documentType);

        log.info("Uploading documents ==> Validating provider's documents");
        if (isFacilityMedia) {
            // Enforce 10-attachment cap
            long currentCount = documentRepository
                    .countByProfileIdAndProfileTypeAndDocumentType(
                            providerId,
                            Document.ProfileType.PROVIDER,
                            FACILITY_MEDIA_TYPE);

            if (currentCount >= FACILITY_MEDIA_MAX_COUNT) {
                throw new ValidationException(
                        "Maximum " + FACILITY_MEDIA_MAX_COUNT +
                                " facility media attachments allowed. Please delete an existing attachment before uploading a new one.");
            }

            // Enforce 5 MB per-file limit for media
            if (file.getSize() > FACILITY_MEDIA_MAX_BYTES) {
                throw new ValidationException(
                        "Facility media files must not exceed 5 MB. Uploaded file size: " +
                                String.format("%.2f", file.getSize() / (1024.0 * 1024.0)) + " MB");
            }
        }
        log.info("Uploading documents ==> Now start uploading documents");
        return uploadDocument(providerId, Document.ProfileType.PROVIDER, documentType, file);
    }

    // ── NEW: Retrieve only facility media (Requirement 2 & 3) ────────────────

    public List<DocumentResponse> getFacilityMedia(UUID providerId) {
        List<Document> docs = documentRepository.findByProfileIdAndProfileTypeAndDocumentType(
                providerId,
                Document.ProfileType.PROVIDER,
                FACILITY_MEDIA_TYPE);

        return docs.stream()
                .map(doc -> {
                    DocumentResponse resp = documentMapper.toResponse(doc);
                    resp.setPresignedUrl(doc.getFileUrl());
                    return resp;
                })
                .collect(Collectors.toList());
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<DocumentResponse> getDocuments(UUID profileId, Document.ProfileType profileType) {
        List<Document> docs = documentRepository.findByProfileIdAndProfileType(profileId, profileType);
        return docs.stream()
                .map(doc -> {
                    DocumentResponse resp = documentMapper.toResponse(doc);
                    resp.setPresignedUrl(doc.getFileUrl());
                    return resp;
                })
                .collect(Collectors.toList());
    }

    public DocumentResponse getDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        DocumentResponse response = documentMapper.toResponse(document);
        response.setPresignedUrl(document.getFileUrl());
        return response;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        if (document.getFileKey() != null) {
            fileStorageService.deleteFile(document.getFileKey());
        }

        documentRepository.delete(document);
        log.info("Document deleted: id={}", documentId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String determineFileType(String documentType, String mimeType) {
        return switch (documentType.toUpperCase()) {
            case "FACILITY_MEDIA", "INTRO_VIDEO" -> {
                // Auto-detect image vs video from the actual MIME type
                if (mimeType != null && mimeType.startsWith("video/")) yield "video";
                yield "image";
            }
            case "FACILITY_PHOTO", "PROFILE_IMAGE" -> "image";
            case "MEDICAL_REPORT", "INSURANCE_DOCUMENT" -> "document";
            default -> {
                // Unknown document type — fall back to MIME type detection
                if (mimeType != null) {
                    if (mimeType.startsWith("image/")) yield "image";
                    if (mimeType.startsWith("video/")) yield "video";
                }
                yield "document";
            }
        };
    }
}