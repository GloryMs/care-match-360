package com.carematchservice.kafka;

import com.carematchservice.kafka.events.MatchCalculatedEvent;
import com.carematchservice.kafka.events.OfferAcceptedEvent;
import com.carematchservice.kafka.events.OfferRejectedEvent;
import com.carematchservice.kafka.events.OfferSentEvent;
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
public class MatchingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.match-calculated}")
    private String matchCalculatedTopic;

    @Value("${kafka.topics.offer-sent}")
    private String offerSentTopic;

    @Value("${kafka.topics.offer-accepted}")
    private String offerAcceptedTopic;

    @Value("${kafka.topics.offer-rejected}")
    private String offerRejectedTopic;

    public void sendMatchCalculatedEvent(MatchCalculatedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(matchCalculatedTopic, event.getMatchId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Match calculated event sent: matchId={}, score={}",
                        event.getMatchId(), event.getScore());
            } else {
                log.error("Failed to send match calculated event: matchId={}",
                        event.getMatchId(), ex);
            }
        });
    }

    public void sendOfferSentEvent(OfferSentEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(offerSentTopic, event.getOfferId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Offer sent event sent: offerId={}", event.getOfferId());
            } else {
                log.error("Failed to send offer sent event: offerId={}", event.getOfferId(), ex);
            }
        });
    }

    public void sendOfferAcceptedEvent(OfferAcceptedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(offerAcceptedTopic, event.getOfferId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Offer accepted event sent: offerId={}", event.getOfferId());
            } else {
                log.error("Failed to send offer accepted event: offerId={}", event.getOfferId(), ex);
            }
        });
    }

    public void sendOfferRejectedEvent(OfferRejectedEvent event) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(offerRejectedTopic, event.getOfferId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Offer rejected event sent: offerId={}", event.getOfferId());
            } else {
                log.error("Failed to send offer rejected event: offerId={}", event.getOfferId(), ex);
            }
        });
    }
}
