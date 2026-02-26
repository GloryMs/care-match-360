package com.careprofileservice.kafka;

import com.carecommon.kafkaEvents.ProfileCreatedEvent;
import com.carecommon.kafkaEvents.ProfileUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.profile-created}")
    private String profileCreatedTopic;

    @Value("${kafka.topics.profile-updated}")
    private String profileUpdatedTopic;

    public void sendProfileCreatedEvent(ProfileCreatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(profileCreatedTopic, event.getProfileId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Profile created event sent successfully: profileId={}, topic={}",
                        event.getProfileId(), profileCreatedTopic);
            } else {
                log.error("Failed to send profile created event: profileId={}",
                        event.getProfileId(), ex);
            }
        });
    }

    public void sendProfileUpdatedEvent(ProfileUpdatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(profileUpdatedTopic, event.getProfileId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Profile updated event sent successfully: profileId={}, topic={}",
                        event.getProfileId(), profileUpdatedTopic);
            } else {
                log.error("Failed to send profile updated event: profileId={}",
                        event.getProfileId(), ex);
            }
        });
    }
}
