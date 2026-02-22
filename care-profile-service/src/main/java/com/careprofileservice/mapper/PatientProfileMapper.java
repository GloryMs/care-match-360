package com.careprofileservice.mapper;

import com.careprofileservice.dto.CreatePatientProfileRequest;
import com.careprofileservice.dto.PatientProfileResponse;
import com.careprofileservice.dto.UpdatePatientProfileRequest;
import com.careprofileservice.model.PatientProfile;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.*;

//TODO remove this mapper
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PatientProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "location", expression = "java(createPoint(request.getLatitude(), request.getLongitude()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PatientProfile toEntity(CreatePatientProfileRequest request);

    @Mapping(target = "latitude", expression = "java(getLatitude(profile.getLocation()))")
    @Mapping(target = "longitude", expression = "java(getLongitude(profile.getLocation()))")
    PatientProfileResponse toResponse(PatientProfile profile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "location", expression = "java(updatePoint(request.getLatitude(), request.getLongitude(), profile.getLocation()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdatePatientProfileRequest request, @MappingTarget PatientProfile profile);

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
