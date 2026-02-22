package com.careprofileservice.mapper;

import com.careprofileservice.dto.DocumentResponse;
import com.careprofileservice.model.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

//TODO remove this mapper
@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "profileType", expression = "java(document.getProfileType().name().toLowerCase())")
    @Mapping(target = "presignedUrl", ignore = true)
    //Actually noo need for this mapper class, so in order to do the to-do :) you have to use ModelMapper
    DocumentResponse toResponse(Document document);
}
