package com.carematchservice.controller;

import com.carecommon.dto.ApiResponse;
import com.carematchservice.dto.*;
import com.carematchservice.service.CareRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Care Request endpoints.
 *
 *  POST   /api/v1/care-requests                          — patient submits request (Req 4)
 *  GET    /api/v1/care-requests/patient                  — patient views their requests (Req 4)
 *  GET    /api/v1/care-requests/provider                 — provider inbox (Req 5)
 *  PUT    /api/v1/care-requests/{requestId}/decline      — provider declines (Req 5)
 */
@RestController
@RequestMapping("/api/v1/care-requests")
@RequiredArgsConstructor
public class CareRequestController {

    private final CareRequestService careRequestService;

    // ── Patient: submit a care request ───────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<CareRequestResponse>> submitRequest(
            @RequestHeader("X-Patient-Id") String patientId,
            @Valid @RequestBody CreateCareRequestRequest request) {

        CareRequestResponse response = careRequestService.submitRequest(
                UUID.fromString(patientId), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Care request submitted successfully"));
    }

    // ── Patient: view my submitted requests ──────────────────────────────────

    @GetMapping("/patient")
    public ResponseEntity<ApiResponse<Page<CareRequestResponse>>> getMyRequests(
            @RequestHeader("X-Patient-Id") String patientId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<CareRequestResponse> requests = careRequestService.getPatientRequests(
                UUID.fromString(patientId), status, page, size);

        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    // ── Provider: view incoming request inbox ────────────────────────────────

    @GetMapping("/provider")
    public ResponseEntity<ApiResponse<Page<CareRequestResponse>>> getProviderInbox(
            @RequestHeader("X-Provider-Id") String providerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<CareRequestResponse> requests = careRequestService.getProviderRequests(
                UUID.fromString(providerId), status, page, size);

        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    // ── Provider: decline a request ──────────────────────────────────────────

    @PutMapping("/{requestId}/decline")
    public ResponseEntity<ApiResponse<CareRequestResponse>> declineRequest(
            @PathVariable UUID requestId,
            @RequestHeader("X-Provider-Id") String providerId,
            @RequestBody DeclineCareRequestRequest body) {

        CareRequestResponse response = careRequestService.declineRequest(
                requestId, UUID.fromString(providerId), body);

        return ResponseEntity.ok(ApiResponse.success(response, "Request declined"));
    }
}