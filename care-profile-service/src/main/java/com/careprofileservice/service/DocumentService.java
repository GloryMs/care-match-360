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

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentMapper documentMapper;

    @Transactional
    public DocumentResponse uploadDocument(
            UUID profileId,
            Document.ProfileType profileType,
            String documentType,
            MultipartFile file) {

        // Determine folder and file type
        String folder = profileType.name().toLowerCase() + "s/" + profileId;
        String fileType = determineFileType(documentType);

        // Upload to S3
        String fileKey = fileStorageService.uploadFile(file, folder, fileType);
        String fileUrl = fileStorageService.getPublicUrl(fileKey);

        // Save document metadata
        Document document = Document.builder()
                .profileId(profileId)
                .profileType(profileType)
                .documentType(documentType)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();

        document = documentRepository.save(document);
        log.info("Document uploaded: profileId={}, documentId={}, type={}",
                profileId, document.getId(), documentType);

        // Generate presigned URL
        DocumentResponse response = documentMapper.toResponse(document);
        response.setPresignedUrl(fileStorageService.generatePresignedUrl(fileKey));

        return response;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(UUID profileId, Document.ProfileType profileType) {
        List<Document> documents = documentRepository.findByProfileIdAndProfileType(profileId, profileType);

        return documents.stream()
                .map(doc -> {
                    DocumentResponse response = documentMapper.toResponse(doc);
                    // Generate presigned URL for each document
                    String fileKey = extractKeyFromUrl(doc.getFileUrl());
                    response.setPresignedUrl(fileStorageService.generatePresignedUrl(fileKey));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        DocumentResponse response = documentMapper.toResponse(document);
        String fileKey = extractKeyFromUrl(document.getFileUrl());
        response.setPresignedUrl(fileStorageService.generatePresignedUrl(fileKey));

        return response;
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        documentRepository.delete(document);
        log.info("Document deleted: documentId={}", documentId);
    }

    private String determineFileType(String documentType) {
        return switch (documentType.toLowerCase()) {
            case "intro_video" -> "video";
            case "facility_photo", "profile_image" -> "image";
            case "medical_report", "insurance_document" -> "document";
            default -> "document";
        };
    }

    private String extractKeyFromUrl(String url) {
        // Extract S3 key from full URL
        // Example: https://bucket.s3.region.amazonaws.com/path/to/file.jpg -> path/to/file.jpg
        int lastSlashIndex = url.indexOf(".com/");
        if (lastSlashIndex != -1) {
            return url.substring(lastSlashIndex + 5);
        }
        return url;
    }
}
