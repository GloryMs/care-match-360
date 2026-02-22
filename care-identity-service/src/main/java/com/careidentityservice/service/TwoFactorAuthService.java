package com.careidentityservice.service;

import com.carecommon.constants.ErrorCodes;
import com.carecommon.exception.ResourceNotFoundException;
import com.carecommon.exception.ValidationException;
import com.careidentityservice.dto.TwoFactorSetupResponse;
import com.careidentityservice.model.TwoFactorAuth;
import com.careidentityservice.model.User;
import com.careidentityservice.repository.TwoFactorAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {

    private final TwoFactorAuthRepository twoFactorAuthRepository;
    //private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    @Value("${app.two-factor.issuer}")
    private String issuer;

    @Transactional
    public TwoFactorSetupResponse setupTwoFactor(User user) {
        // Generate new secret
//        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
//        String secret = key.getKey();

        //Fake:
        String secret = "";

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes(10);

        // Save or update 2FA settings
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
                .orElse(TwoFactorAuth.builder()
                        .user(user)
                        .build());

        twoFactorAuth.setSecret(secret);
        twoFactorAuth.setBackupCodes(backupCodes);
        twoFactorAuth.setIsEnabled(false); // Not enabled until verified

        twoFactorAuthRepository.save(twoFactorAuth);

        // Generate QR code URL
//        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
//                issuer,
//                user.getEmail(),
//                key
//        );

        //Fake:
        String qrCodeUrl = "";

        log.info("2FA setup initiated for user: {}", user.getEmail());

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(backupCodes)
                .build();
    }

    @Transactional
    public void enableTwoFactor(User user, String code) {
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("2FA setup not found. Please setup 2FA first."));

        if (!verifyCode(twoFactorAuth.getSecret(), code)) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "Invalid verification code");
        }

        twoFactorAuth.setIsEnabled(true);
        twoFactorAuthRepository.save(twoFactorAuth);

        log.info("2FA enabled for user: {}", user.getEmail());
    }

    @Transactional
    public void disableTwoFactor(User user) {
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("2FA not found"));

        twoFactorAuthRepository.delete(twoFactorAuth);
        log.info("2FA disabled for user: {}", user.getEmail());
    }

    public boolean isTwoFactorEnabled(User user) {
        return twoFactorAuthRepository.findByUser(user)
                .map(TwoFactorAuth::getIsEnabled)
                .orElse(false);
    }

    public boolean verifyTwoFactorCode(User user, String code) {
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("2FA not enabled"));

        if (!twoFactorAuth.getIsEnabled()) {
            throw new ValidationException(ErrorCodes.EMAIL_ALREADY_EXISTS, "2FA is not enabled");
        }

        // Check if it's a backup code
        if (twoFactorAuth.getBackupCodes() != null && twoFactorAuth.getBackupCodes().contains(code)) {
            // Remove used backup code
            twoFactorAuth.getBackupCodes().remove(code);
            twoFactorAuthRepository.save(twoFactorAuth);
            log.info("Backup code used for user: {}", user.getEmail());
            return true;
        }

        // Verify TOTP code
        return verifyCode(twoFactorAuth.getSecret(), code);
    }

    private boolean verifyCode(String secret, String code) {
        try {
            int codeInt = Integer.parseInt(code);
            //return googleAuthenticator.authorize(secret, codeInt);
            //Fake
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<String> generateBackupCodes(int count) {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < count; i++) {
            int code = 100000 + random.nextInt(900000); // 6-digit codes
            codes.add(String.valueOf(code));
        }

        return codes;
    }
}