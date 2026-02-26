package com.careprofileservice.kafka;

import com.carecommon.kafkaEvents.AccountVerifiedEvent;
import com.careprofileservice.model.ProviderProfile;
import com.careprofileservice.service.PatientProfileService;
import com.careprofileservice.service.ProviderProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountVerifiedEventConsumer {

    private final PatientProfileService patientProfileService;
    private final ProviderProfileService providerProfileService;

    @KafkaListener(topics = "${kafka.topics.account-verified}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleAccountVerified(@Payload AccountVerifiedEvent event) {
        log.info("Received account verified event: userId={}, role={}", event.getUserId(), event.getRole());
        try {
            String role = event.getRole();
            if ("PATIENT".equals(role) || "RELATIVE".equals(role)) {
                patientProfileService.createBasicProfile(event.getUserId(), event.getEmail());
            } else if ("RESIDENTIAL_PROVIDER".equals(role)) {
                providerProfileService.createBasicProfile(event.getUserId(), event.getEmail(), ProviderProfile.ProviderType.RESIDENTIAL);
            } else if ("AMBULATORY_PROVIDER".equals(role)) {
                providerProfileService.createBasicProfile(event.getUserId(), event.getEmail(), ProviderProfile.ProviderType.AMBULATORY);
            } else {
                log.info("No profile creation needed for role: {}", role);
            }
        } catch (Exception e) {
            log.error("Failed to create profile for account verified event: userId={}", event.getUserId(), e);
        }
    }
}
