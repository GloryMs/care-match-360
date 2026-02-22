package com.carematchservice.mapper;

import com.carematchservice.dto.OfferHistoryResponse;
import com.carematchservice.model.OfferHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OfferHistoryMapper {

    OfferHistoryResponse toResponse(OfferHistory offerHistory);
}
