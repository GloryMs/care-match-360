package com.carenotificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaProperties kafkaProperties) {

        // Wrap with ErrorHandlingDeserializer so bad payloads are skipped, not retried
        ErrorHandlingDeserializer<Object> valueDeserializer =
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>());

        DefaultKafkaConsumerFactory<String, Object> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        kafkaProperties.buildConsumerProperties(null),
                        new StringDeserializer(),
                        valueDeserializer
                );

        // No retries — skip unrecoverable records immediately (prevents duplicate processing)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, ex) -> log.error(
                        "Skipping unrecoverable record: topic={}, partition={}, offset={}, error={}",
                        record.topic(), record.partition(), record.offset(), ex.getMessage()),
                new FixedBackOff(0L, 0L)   // 0 backoff, 0 retries
        );
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.setRecordMessageConverter(new StringJsonMessageConverter());
        // AckMode.RECORD = commit offset immediately after each record is processed
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }
}