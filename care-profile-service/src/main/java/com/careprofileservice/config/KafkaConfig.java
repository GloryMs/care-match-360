package com.careprofileservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.profile-created}")
    private String profileCreatedTopic;

    @Value("${kafka.topics.profile-updated}")
    private String profileUpdatedTopic;

    @Value("${kafka.topics.account-verified}")
    private String accountVerifiedTopic;

    @Bean
    public NewTopic profileCreatedTopic() {
        return TopicBuilder.name(profileCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic profileUpdatedTopic() {
        return TopicBuilder.name(profileUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic accountVerifiedTopic() {
        return TopicBuilder.name(accountVerifiedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
