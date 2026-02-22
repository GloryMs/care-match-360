package com.carematchservice.mapper;

import com.carematchservice.dto.MatchScoreResponse;
import com.carematchservice.model.MatchScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchScoreMapper {

    @Mapping(target = "providerName", ignore = true)
    @Mapping(target = "providerType", ignore = true)
    @Mapping(target = "providerAddress", ignore = true)
    MatchScoreResponse toResponse(MatchScore matchScore);
}
