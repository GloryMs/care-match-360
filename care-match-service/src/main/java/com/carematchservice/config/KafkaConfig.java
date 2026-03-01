package com.carematchservice.config;

import com.carecommon.kafkaEvents.ProfileCreatedEvent;
import com.carecommon.kafkaEvents.ProfileUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.match-calculated}")
    private String matchCalculatedTopic;

    @Value("${kafka.topics.offer-sent}")
    private String offerSentTopic;

    @Value("${kafka.topics.offer-accepted}")
    private String offerAcceptedTopic;

    @Value("${kafka.topics.offer-rejected}")
    private String offerRejectedTopic;

    @Value("${kafka.topics.care-request-submitted}")
    private String careRequestSubmittedTopic;

    @Value("${kafka.topics.care-request-declined}")
    private String careRequestDeclinedTopic;

    @Bean public NewTopic careRequestSubmittedTopic() {
        return TopicBuilder.name(careRequestSubmittedTopic).partitions(3).replicas(1).build();
    }
    @Bean public NewTopic careRequestDeclinedTopic() {
        return TopicBuilder.name(careRequestDeclinedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic matchCalculatedTopic() {
        return TopicBuilder.name(matchCalculatedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic offerSentTopic() {
        return TopicBuilder.name(offerSentTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic offerAcceptedTopic() {
        return TopicBuilder.name(offerAcceptedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic offerRejectedTopic() {
        return TopicBuilder.name(offerRejectedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            KafkaProperties kafkaProperties) {

        // Map __TypeId__ header values → local classes.
        // Covers: (1) old messages with full producer class name, (2) new messages with logical alias.
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.careprofileservice.kafka.ProfileCreatedEvent", ProfileCreatedEvent.class);
        idClassMapping.put("com.careprofileservice.kafka.ProfileUpdatedEvent", ProfileUpdatedEvent.class);
        idClassMapping.put("ProfileCreatedEvent", ProfileCreatedEvent.class);
        idClassMapping.put("ProfileUpdatedEvent", ProfileUpdatedEvent.class);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.addTrustedPackages("*");
        typeMapper.setIdClassMapping(idClassMapping);

        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.setTypeMapper(typeMapper);

        // ErrorHandlingDeserializer wraps JsonDeserializer so that when a record cannot
        // be deserialized, the exception is caught INSIDE the deserializer (before the
        // Kafka client wraps it in RecordDeserializationException and bypasses Spring's
        // error handler). The failed record is delivered with a null payload.
        ErrorHandlingDeserializer<Object> valueDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        DefaultKafkaConsumerFactory<String, Object> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        kafkaProperties.buildConsumerProperties(null),
                        new StringDeserializer(),
                        valueDeserializer
                );

        // Skip unrecoverable records immediately (no retries).
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Skipping unrecoverable record: topic={}, partition={}, offset={}, error={}",
                        record.topic(), record.partition(), record.offset(),
                        exception.getMessage()),
                new FixedBackOff(0L, 0L)
        );
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}