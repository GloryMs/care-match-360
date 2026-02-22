package com.carematchservice.mapper;

import com.carematchservice.dto.CreateOfferRequest;
import com.carematchservice.dto.OfferResponse;
import com.carematchservice.model.Offer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OfferMapper {

    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "matchId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    Offer toEntity(CreateOfferRequest request);

    @Mapping(target = "status", expression = "java(offer.getStatus().name())")
    @Mapping(target = "providerName", ignore = true)
    @Mapping(target = "patientName", ignore = true)
    @Mapping(target = "matchScore", ignore = true)
    OfferResponse toResponse(Offer offer);
}
