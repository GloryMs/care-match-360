package com.careidentityservice.controller;


import com.carecommon.dto.ApiResponse;
import com.careidentityservice.dto.TwoFactorSetupResponse;
import com.careidentityservice.dto.VerifyTwoFactorRequest;
import com.careidentityservice.model.User;
import com.careidentityservice.repository.UserRepository;
import com.careidentityservice.service.TwoFactorAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/2fa")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Authentication", description = "Two-factor authentication management")
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final UserRepository userRepository;

    @PostMapping("/setup")
    @Operation(summary = "Setup two-factor authentication")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setupTwoFactor(
            @RequestHeader("X-User-Id") String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        TwoFactorSetupResponse response = twoFactorAuthService.setupTwoFactor(user);
        return ResponseEntity.ok(ApiResponse.success(response, "2FA setup initiated. Please scan QR code with your authenticator app."));
    }

    @PostMapping("/enable")
    @Operation(summary = "Enable two-factor authentication")
    public ResponseEntity<ApiResponse<Void>> enableTwoFactor(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody VerifyTwoFactorRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        twoFactorAuthService.enableTwoFactor(user, request.getCode());
        return ResponseEntity.ok(ApiResponse.success(null, "2FA enabled successfully"));
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable two-factor authentication")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
            @RequestHeader("X-User-Id") String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        twoFactorAuthService.disableTwoFactor(user);
        return ResponseEntity.ok(ApiResponse.success(null, "2FA disabled successfully"));
    }

    @GetMapping("/status")
    @Operation(summary = "Check 2FA status")
    public ResponseEntity<ApiResponse<Boolean>> getTwoFactorStatus(
            @RequestHeader("X-User-Id") String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        boolean isEnabled = twoFactorAuthService.isTwoFactorEnabled(user);
        return ResponseEntity.ok(ApiResponse.success(isEnabled));
    }
}