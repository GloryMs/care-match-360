package com.careidentityservice.service;


import com.carecommon.constants.ErrorCodes;
import com.carecommon.exception.ResourceNotFoundException;
import com.carecommon.exception.UnauthorizedException;
import com.carecommon.exception.ValidationException;
import com.carecommon.util.JwtUtil;
import com.careidentityservice.dto.*;
import com.careidentityservice.mapper.UserMapper;
import com.careidentityservice.model.EmailVerificationToken;
import com.careidentityservice.model.PasswordResetToken;
import com.careidentityservice.model.RefreshToken;
import com.careidentityservice.model.User;
import com.careidentityservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TwoFactorAuthRepository twoFactorAuthRepository;
    
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserMapper userMapper;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Email already registered");
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isVerified(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        // Create verification token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        
        verificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), token);

        UserResponse response = userMapper.toUserResponse(user);
        response.setTwoFactorEnabled(false);
        return response;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user
        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Check if email is verified
        if (!user.getIsVerified()) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Email not verified. Please verify your email first.");
        }

        // Check 2FA
        boolean requires2FA = twoFactorAuthService.isTwoFactorEnabled(user);
        
        if (requires2FA) {
            if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isEmpty()) {
                throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Two-factor authentication code required");
            }
            
            if (!twoFactorAuthService.verifyTwoFactorCode(user, request.getTwoFactorCode())) {
                throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Invalid two-factor authentication code");
            }
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getRole().name());

        // Save refresh token
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusHours(refreshTokenExpiration))
                .build();
        
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("User logged in: {}", user.getEmail());

        // Build response
        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setTwoFactorEnabled(requires2FA);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // Convert to seconds
                .user(userResponse)
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Validate refresh token
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        // Check if revoked or expired
        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = refreshToken.getUser();

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());

        log.info("Access token refreshed for user: {}", user.getEmail());

        // Build response
        UserResponse userResponse = userMapper.toUserResponse(user);
        userResponse.setTwoFactorEnabled(twoFactorAuthService.isTwoFactorEnabled(user));

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken()) // Return same refresh token
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .user(userResponse)
                .build();
    }

    @Transactional
    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());

        log.info("User logged out: {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String email, String token) {
        EmailVerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Invalid verification token"));

        if (!verificationToken.getUser().getEmail().equals(email)) {
            throw new ValidationException(ErrorCodes.INVALID_CREDENTIALS, "Provided token does not match provided email address");
        }

        if (verificationToken.isExpired()) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Verification token has expired");
        }

        if (verificationToken.isUsed()) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Verification token has already been used");
        }

        // Mark user as verified
        User user = verificationToken.getUser();
        user.setIsVerified(true);
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // Generate reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        resetTokenRepository.save(resetToken);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        log.info("Password reset requested for user: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Invalid reset token"));

        if (resetToken.isExpired()) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Reset token has expired");
        }

        if (resetToken.isUsed()) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Reset token has already been used");
        }

        // Update password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsedAt(LocalDateTime.now());
        resetTokenRepository.save(resetToken);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());

        log.info("Password changed for user: {}", user.getEmail());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.getIsVerified()) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Email is already verified");
        }

        // Delete old tokens
        verificationTokenRepository.deleteByUser(user);

        // Create new token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        
        verificationTokenRepository.save(verificationToken);

        // Send email
        emailService.sendVerificationEmail(user.getEmail(), token);

        log.info("Verification email resent to: {}", user.getEmail());
    }
}