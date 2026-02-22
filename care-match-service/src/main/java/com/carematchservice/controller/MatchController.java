package com.carematchservice.controller;



import com.carecommon.dto.ApiResponse;
import com.carematchservice.dto.MatchScoreResponse;
import com.carematchservice.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
//@Tag(name = "Matches", description = "Match calculation and retrieval endpoints")
public class MatchController {

    private final MatchingService matchingService;

    @PostMapping("/calculate")
    //@Operation(summary = "Calculate match between patient and provider")
    public ResponseEntity<ApiResponse<MatchScoreResponse>> calculateMatch(
            @RequestParam UUID patientId,
            @RequestParam UUID providerId) {

        MatchScoreResponse match = matchingService.calculateMatch(patientId, providerId);
        return ResponseEntity.ok(ApiResponse.success(match, "Match calculated successfully"));
    }

    @GetMapping("/patient/{patientId}")
    //@Operation(summary = "Get all matches for a patient")
    public ResponseEntity<ApiResponse<List<MatchScoreResponse>>> getMatchesForPatient(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        List<MatchScoreResponse> matches = matchingService.getMatchesForPatient(patientId, page, size);
        return ResponseEntity.ok(ApiResponse.success(matches));
    }

    @GetMapping("/provider/{providerId}")
    //@Operation(summary = "Get all matches for a provider")
    public ResponseEntity<ApiResponse<List<MatchScoreResponse>>> getMatchesForProvider(
            @PathVariable UUID providerId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        List<MatchScoreResponse> matches = matchingService.getMatchesForProvider(providerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(matches));
    }

    @GetMapping("/patient/{patientId}/top")
    //@Operation(summary = "Get top matches for a patient")
    public ResponseEntity<ApiResponse<List<MatchScoreResponse>>> getTopMatchesForPatient(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "10") int limit) {

        List<MatchScoreResponse> matches = matchingService.getTopMatchesForPatient(patientId, limit);
        return ResponseEntity.ok(ApiResponse.success(matches));
    }

    @GetMapping("/patient/{patientId}/provider/{providerId}")
    //@Operation(summary = "Get specific match between patient and provider")
    public ResponseEntity<ApiResponse<MatchScoreResponse>> getMatch(
            @PathVariable UUID patientId,
            @PathVariable UUID providerId) {

        MatchScoreResponse match = matchingService.getMatch(patientId, providerId);
        return ResponseEntity.ok(ApiResponse.success(match));
    }

    @PostMapping("/recalculate/patient/{patientId}")
    //@Operation(summary = "Recalculate all matches for a patient")
    public ResponseEntity<ApiResponse<Void>> recalculateMatchesForPatient(@PathVariable UUID patientId) {
        matchingService.recalculateMatchesForPatient(patientId);
        return ResponseEntity.ok(ApiResponse.success(null, "Match recalculation initiated"));
    }

    @PostMapping("/recalculate/provider/{providerId}")
    //@Operation(summary = "Recalculate all matches for a provider")
    public ResponseEntity<ApiResponse<Void>> recalculateMatchesForProvider(@PathVariable UUID providerId) {
        matchingService.recalculateMatchesForProvider(providerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Match recalculation initiated"));
    }
}
