package com.careprofileservice.controller;

import com.carecommon.dto.ApiResponse;
import com.careprofileservice.dto.*;
import com.careprofileservice.model.Document;
import com.careprofileservice.service.DocumentService;
import com.careprofileservice.service.ProviderProfileService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Slf4j
//@Tag(name = "Provider Profiles", description = "Provider profile management endpoints")
public class ProviderProfileController {

    private final ProviderProfileService providerProfileService;
    private final DocumentService documentService;

    @PostMapping
    //@Operation(summary = "Create provider profile")
    public ResponseEntity<ApiResponse<ProviderProfileResponse>> createProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateProviderProfileRequest request) {

        ProviderProfileResponse profile = providerProfileService.createProfile(
                UUID.fromString(userId), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(profile, "Provider profile created successfully"));
    }

    @PutMapping
    //@Operation(summary = "Update provider profile")
    public ResponseEntity<ApiResponse<ProviderProfileResponse>> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProviderProfileRequest request) {

        ProviderProfileResponse profile = providerProfileService.updateProfile(
                UUID.fromString(userId), request);

        return ResponseEntity.ok(ApiResponse.success(profile, "Provider profile updated successfully"));
    }

    @GetMapping("/me")
    //@Operation(summary = "Get current provider profile")
    public ResponseEntity<ApiResponse<ProviderProfileResponse>> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {

        ProviderProfileResponse profile = providerProfileService.getProfileByUserId(
                UUID.fromString(userId));

        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{profileId}")
    //@Operation(summary = "Get provider profile by ID")
    public ResponseEntity<ApiResponse<ProviderProfileResponse>> getProfileById(
            @PathVariable UUID profileId) {

        ProviderProfileResponse profile = providerProfileService.getProfileById(profileId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/search")
    //@Operation(summary = "Search providers")
    public ResponseEntity<ApiResponse<ProviderSearchResponse>> searchProviders(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ProviderSearchRequest request) {

        ProviderSearchResponse response = providerProfileService.searchProviders(
                UUID.fromString(userId), request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping
    //@Operation(summary = "Delete provider profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @RequestHeader("X-User-Id") String userId) {

        providerProfileService.deleteProfile(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null, "Provider profile deleted successfully"));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    //@Operation(summary = "Upload document or media")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) {

        ProviderProfileResponse profile = providerProfileService.getProfileByUserId(
                UUID.fromString(userId));

        DocumentResponse document = documentService.uploadDocument(
                profile.getId(),
                Document.ProfileType.PROVIDER,
                documentType,
                file
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(document, "Document uploaded successfully"));
    }

    @GetMapping("/documents")
    //@Operation(summary = "Get provider documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
            @RequestHeader("X-User-Id") String userId) {

        ProviderProfileResponse profile = providerProfileService.getProfileByUserId(
                UUID.fromString(userId));

        List<DocumentResponse> documents = documentService.getDocuments(
                profile.getId(),
                Document.ProfileType.PROVIDER
        );

        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @DeleteMapping("/documents/{documentId}")
    //@Operation(summary = "Delete document")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }

    /**
     * GET /api/v1/providers/all
     *
     * Returns all active (visible) provider profiles.
     * Internal use by the matching engine.
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ProviderProfileResponse>>> getAllActiveProviders() {
        List<ProviderProfileResponse> providers = providerProfileService.getAllActiveProviders();
        return ResponseEntity.ok(ApiResponse.success(providers, "Active providers retrieved"));
    }
}
