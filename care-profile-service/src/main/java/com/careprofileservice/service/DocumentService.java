package com.careprofileservice.service;

import com.carecommon.exception.ResourceNotFoundException;
import com.careprofileservice.dto.DocumentResponse;
import com.careprofileservice.mapper.DocumentMapper;
import com.careprofileservice.model.Document;
import com.careprofileservice.repository.DocumentRepository;
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
 * The "presignedUrl" field in DocumentResponse now returns the same download URL
 * as fileUrl — there are no expiring tokens in the standalone implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService  fileStorageService;
    private final DocumentMapper      documentMapper;

    // ── Upload ────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentResponse uploadDocument(
            UUID profileId,
            Document.ProfileType profileType,
            String documentType,
            MultipartFile file) {

        String folder   = profileType.name().toLowerCase() + "s/" + profileId;
        String fileType = determineFileType(documentType);

        // Compress + encrypt and persist on local disk
        String fileKey  = fileStorageService.uploadFile(file, folder, fileType);
        String fileUrl  = fileStorageService.getPublicUrl(fileKey);

        Document document = Document.builder()
                .profileId(profileId)
                .profileType(profileType)
                .documentType(documentType)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileKey(fileKey)           // new column — see migration note below
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();

        Document saved = documentRepository.save(document);
        log.info("Document saved: id={}, profileId={}, type={}", saved.getId(), profileId, documentType);

        DocumentResponse response = documentMapper.toResponse(saved);
        // No presigned URL in standalone mode — reuse the public download URL
        response.setPresignedUrl(fileUrl);
        return response;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<DocumentResponse> getDocuments(UUID profileId, Document.ProfileType profileType) {
        List<Document> docs = documentRepository.findByProfileIdAndProfileType(profileId, profileType);
        return docs.stream()
                .map(doc -> {
                    DocumentResponse resp = documentMapper.toResponse(doc);
                    // Download URL is already stored in fileUrl; set presignedUrl = same
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

        // Remove encrypted file from disk (both .gz.enc and .meta sidecar)
        if (document.getFileKey() != null) {
            fileStorageService.deleteFile(document.getFileKey());
        }

        documentRepository.delete(document);
        log.info("Document deleted: id={}", documentId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String determineFileType(String documentType) {
        return switch (documentType.toLowerCase()) {
            case "intro_video"              -> "video";
            case "facility_photo",
                 "profile_image"            -> "image";
            case "medical_report",
                 "insurance_document"       -> "document";
            default                         -> "document";
        };
    }
}
