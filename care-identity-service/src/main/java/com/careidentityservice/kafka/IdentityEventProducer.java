package com.careidentityservice.kafka;

import com.carecommon.kafkaEvents.AccountVerifiedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.account-verified}")
    private String accountVerifiedTopic;

    public void publishAccountVerifiedEvent(AccountVerifiedEvent event) {
        try {
            kafkaTemplate.send(accountVerifiedTopic, event.getUserId().toString(), event);
            log.info("Published account verified event: userId={}, role={}", event.getUserId(), event.getRole());
        } catch (Exception e) {
            log.error("Failed to publish account verified event: userId={}", event.getUserId(), e);
        }
    }
}
