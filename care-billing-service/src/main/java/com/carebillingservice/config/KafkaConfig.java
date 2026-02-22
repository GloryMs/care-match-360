package com.carebillingservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.subscription-created}")
    private String subscriptionCreatedTopic;

    @Value("${kafka.topics.subscription-updated}")
    private String subscriptionUpdatedTopic;

    @Value("${kafka.topics.subscription-expired}")
    private String subscriptionExpiredTopic;

    @Value("${kafka.topics.payment-succeeded}")
    private String paymentSucceededTopic;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    @Bean
    public NewTopic subscriptionCreatedTopic() {
        return TopicBuilder.name(subscriptionCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic subscriptionUpdatedTopic() {
        return TopicBuilder.name(subscriptionUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic subscriptionExpiredTopic() {
        return TopicBuilder.name(subscriptionExpiredTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentSucceededTopic() {
        return TopicBuilder.name(paymentSucceededTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(paymentFailedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}