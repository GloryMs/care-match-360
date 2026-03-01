package com.careprofileservice.controller;

import com.carecommon.dto.ApiResponse;
import com.careprofileservice.dto.*;
import com.careprofileservice.model.Document;
import com.careprofileservice.service.DocumentService;
import com.careprofileservice.service.ProviderProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * CHANGES in this version:
 *  NEW  GET  /providers                     → paginated public provider directory (Req 1)
 *  NEW  GET  /providers/{providerId}/public → full public detail with media  (Req 2)
 *  NEW  GET  /providers/{id}/media          → FACILITY_MEDIA documents only  (Req 2)
 *  MOD  POST /providers/documents           → now enforces 10-attachment cap + 5 MB (Req 3)
 * Existing endpoints (unchanged):
 *  POST   /providers
 *  PUT    /providers
 *  GET    /providers/me
 *  GET    /providers/{profileId}
 *  POST   /providers/search
 *  DELETE /providers
 *  GET    /providers/documents
 *  DELETE /providers/documents/{documentId}
 *  GET    /providers/all   (internal – matching engine)
 */
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Slf4j
public class ProviderProfileController {

    private final ProviderProfileService providerProfileService;
    private final DocumentService documentService;

    // ── Existing endpoints (unchanged) ───────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<ProviderProfileResponse>> createProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateProviderProfileRequest request) {
        ProviderProfileResponse profile = providerProfileService.createProfile(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profile, "Provider profile created successfully"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProviderProfileResponse>> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProviderProfileRequest request) {
        ProviderProfileResponse profile = providerProfileService.updateProfile(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Provider profile updated successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProviderProfileResponse>> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {
        ProviderProfileResponse profile = providerProfileService.getProfileByUserId(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ApiResponse<ProviderProfileResponse>> getProfileById(
            @PathVariable UUID profileId) {
        ProviderProfileResponse profile = providerProfileService.getProfileById(profileId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<ProviderSearchResponse>> searchProviders(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ProviderSearchRequest request) {
        ProviderSearchResponse response = providerProfileService.searchProviders(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteProfile(@RequestHeader("X-User-Id") String userId) {
        providerProfileService.deleteProfile(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null, "Provider profile deleted successfully"));
    }

    // ── NEW: Public provider directory (Requirement 1) ────────────────────────
    /**
     * GET /api/v1/providers
     * Public-facing paginated provider listing.
     * Accessible by patients, relatives and unauthenticated browsers.
     * Query params:
     *   type      (optional) RESIDENTIAL | AMBULATORY
     *   region    (optional) free-text region filter
     *   careLevel (optional) integer 1-5
     *   page      (default 0)
     *   size      (default 20, max 50)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProviderSummaryResponse>>> listProviders(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer careLevel,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 50); // cap at 50
        Page<ProviderSummaryResponse> result = providerProfileService.listProviders(type, region, careLevel, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── NEW: Full public provider detail (Requirement 2) ──────────────────────
    /**
     * GET /api/v1/providers/{providerId}/public
     * Returns the full provider profile enriched with facility media list.
     * Accessible by patients/relatives.
     */
    @GetMapping("/{providerId}/public")
    public ResponseEntity<ApiResponse<ProviderPublicDetailResponse>> getPublicProviderDetail(
            @PathVariable UUID providerId) {
        ProviderPublicDetailResponse detail = providerProfileService.getPublicDetail(providerId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    // ── NEW: Provider media list (Requirement 2) ──────────────────────────────
    /**
     * GET /api/v1/providers/{providerId}/media
     * Returns only FACILITY_MEDIA documents for a given provider.
     * Publicly accessible (no X-User-Id required).
     */
    @GetMapping("/{providerId}/media")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getProviderMedia(
            @PathVariable UUID providerId) {
        List<DocumentResponse> media = documentService.getFacilityMedia(providerId);
        return ResponseEntity.ok(ApiResponse.success(media));
    }

    // ── MODIFIED: Document upload with 10-attachment cap + 5 MB (Req 3) ──────
    /**
     * POST /api/v1/providers/documents
     * documentType = FACILITY_MEDIA  → images/videos, max 10 attachments, max 5 MB each.
     * Other documentTypes           → existing behaviour, existing max-file-size.
     */
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) {

        log.info("Uploading documents from typ: {} ==> Getting provider's profile first", documentType);
        ProviderProfileResponse profile = providerProfileService.getProfileByUserId(UUID.fromString(userId));
        log.info("Uploading documents ==> Uploading provider's documents");
        DocumentResponse document = documentService.uploadProviderDocument(profile.getId(), documentType, file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(document, "Document uploaded successfully"));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
            @RequestHeader("X-User-Id") String userId) {
        ProviderProfileResponse profile = providerProfileService.getProfileByUserId(UUID.fromString(userId));
        List<DocumentResponse> documents = documentService.getDocuments(profile.getId(), Document.ProfileType.PROVIDER);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }

    // ── Internal: matching engine ─────────────────────────────────────────────
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ProviderProfileResponse>>> getAllActiveProviders() {
        List<ProviderProfileResponse> providers = providerProfileService.getAllActiveProviders();
        return ResponseEntity.ok(ApiResponse.success(providers, "Active providers retrieved"));
    }
}