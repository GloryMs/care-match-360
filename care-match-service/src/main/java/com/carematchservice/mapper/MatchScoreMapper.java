package com.carematchservice.mapper;

import com.carematchservice.dto.MatchScoreResponse;
import com.carematchservice.model.MatchScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface MatchScoreMapper {

    @Mapping(target = "providerName", ignore = true)
    @Mapping(target = "providerType", ignore = true)
    @Mapping(target = "providerAddress", ignore = true)
    @Mapping(target = "providerServiceTiers", ignore = true)
    @Mapping(target = "providerPremiumServices", ignore = true)
    @Mapping(target = "patientSummary", ignore = true)
    @Mapping(target = "providerSummary", ignore = true)
    MatchScoreResponse toResponse(MatchScore matchScore);

    default Map<String, Double> map(Map<String, Object> value) {
        if (value == null) return null;
        Map<String, Double> result = new HashMap<>();
        value.forEach((k, v) -> {
            if (v instanceof Number) {
                result.put(k, ((Number) v).doubleValue());
            }
        });
        return result;
    }
}
