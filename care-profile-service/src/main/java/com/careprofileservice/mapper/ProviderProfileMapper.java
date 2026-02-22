package com.careprofileservice.mapper;

import com.careprofileservice.dto.CreateProviderProfileRequest;
import com.careprofileservice.dto.ProviderProfileResponse;
import com.careprofileservice.dto.UpdateProviderProfileRequest;
import com.careprofileservice.model.ProviderProfile;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.*;

//TODO remove this mapper
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProviderProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "providerType", expression = "java(mapProviderType(request.getProviderType()))")
    @Mapping(target = "location", expression = "java(createPoint(request.getLatitude(), request.getLongitude()))")
    @Mapping(target = "introVideoUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "isVisible", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProviderProfile toEntity(CreateProviderProfileRequest request);

    @Mapping(target = "providerType", expression = "java(profile.getProviderType().name().toLowerCase())")
    @Mapping(target = "latitude", expression = "java(getLatitude(profile.getLocation()))")
    @Mapping(target = "longitude", expression = "java(getLongitude(profile.getLocation()))")
    ProviderProfileResponse toResponse(ProviderProfile profile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "providerType", expression = "java(mapProviderTypeIfNotNull(request.getProviderType()))")
    @Mapping(target = "location", expression = "java(updatePoint(request.getLatitude(), request.getLongitude(), profile.getLocation()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateProviderProfileRequest request, @MappingTarget ProviderProfile profile);

    default ProviderProfile.ProviderType mapProviderType(String providerType) {
        return providerType != null ?
                ProviderProfile.ProviderType.valueOf(providerType.toUpperCase()) : null;
    }

    default ProviderProfile.ProviderType mapProviderTypeIfNotNull(String providerType) {
        return providerType != null ? mapProviderType(providerType) : null;
    }

    default Point createPoint(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    default Point updatePoint(Double latitude, Double longitude, Point existingPoint) {
        if (latitude == null && longitude == null) {
            return existingPoint;
        }
        if (latitude == null || longitude == null) {
            return existingPoint;
        }
        return createPoint(latitude, longitude);
    }

    default Double getLatitude(Point point) {
        return point != null ? point.getY() : null;
    }

    default Double getLongitude(Point point) {
        return point != null ? point.getX() : null;
    }
}
