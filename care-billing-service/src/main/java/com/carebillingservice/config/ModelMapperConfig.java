package com.carebillingservice.config;

import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        
        // Optional: strict matching (recommended in most enterprise projects)
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // Enum → String converter (uses .name() explicitly)
        Converter<Enum<?>, String> enumToStringConverter = new AbstractConverter<>() {
            @Override
            protected String convert(Enum<?> source) {
                return source == null ? null : source.name();
            }
        };

        // Apply to all Enum → String mappings
        mapper.addConverter(enumToStringConverter);

        // If you have many DTOs with status, you can be more specific:
        // mapper.typeMap(Invoice.class, InvoiceResponse.class)
        //     .addMapping(Invoice::getStatus, InvoiceResponse::setStatus)
        //     .setConverter(enumToStringConverter);

        return mapper;
    }
}