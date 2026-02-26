package com.careprofileservice.controller;


import com.carecommon.dto.ApiResponse;
import com.careprofileservice.dto.CreatePatientProfileRequest;
import com.careprofileservice.dto.DocumentResponse;
import com.careprofileservice.dto.PatientProfileResponse;
import com.careprofileservice.dto.UpdatePatientProfileRequest;
import com.careprofileservice.model.Document;
import com.careprofileservice.service.DocumentService;
import com.careprofileservice.service.PatientProfileService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
//@Tag(name = "Patient Profiles", description = "Patient profile management endpoints")
public class PatientProfileController {

    private final PatientProfileService patientProfileService;
    private final DocumentService documentService;

    @PostMapping
    //@Operation(summary = "Create patient profile")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> createProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreatePatientProfileRequest request) {

        PatientProfileResponse profile = patientProfileService.createProfile(
                UUID.fromString(userId), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(profile, "Patient profile created successfully"));
    }

    @PutMapping
    //@Operation(summary = "Update patient profile")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdatePatientProfileRequest request) {

        PatientProfileResponse profile = patientProfileService.updateProfile(
                UUID.fromString(userId), request);

        return ResponseEntity.ok(ApiResponse.success(profile, "Patient profile updated successfully"));
    }

    @GetMapping("/me")
    //@Operation(summary = "Get current patient profile")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {

        PatientProfileResponse profile = patientProfileService.getProfileByUserId(
                UUID.fromString(userId));

        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{profileId}")
    //@Operation(summary = "Get patient profile by ID")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> getProfileById(
            @PathVariable UUID profileId) {

        PatientProfileResponse profile = patientProfileService.getProfileById(profileId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @DeleteMapping
    //@Operation(summary = "Delete patient profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @RequestHeader("X-User-Id") String userId) {

        patientProfileService.deleteProfile(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(null, "Patient profile deleted successfully"));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    //@Operation(summary = "Upload document")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) {

        // Get patient profile to get profile ID
        PatientProfileResponse profile = patientProfileService.getProfileByUserId(
                UUID.fromString(userId));

        DocumentResponse document = documentService.uploadDocument(
                profile.getId(),
                Document.ProfileType.PATIENT,
                documentType,
                file
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(document, "Document uploaded successfully"));
    }

    @GetMapping("/documents")
    //@Operation(summary = "Get patient documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
            @RequestHeader("X-User-Id") String userId) {

        PatientProfileResponse profile = patientProfileService.getProfileByUserId(
                UUID.fromString(userId));

        List<DocumentResponse> documents = documentService.getDocuments(
                profile.getId(),
                Document.ProfileType.PATIENT
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
     * GET /api/v1/patients/all
     * Returns all patients who have given consent.
     * Internal use only – should be secured to ROLE_ADMIN or called service-to-service
     * with a service-account JWT.  Add @PreAuthorize("hasRole('ADMIN')") if preferred.
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PatientProfileResponse>>> getAllActivePatients() {
        List<PatientProfileResponse> patients = patientProfileService.getAllActivePatients();
        return ResponseEntity.ok(ApiResponse.success(patients, "Active patients retrieved"));
    }
}