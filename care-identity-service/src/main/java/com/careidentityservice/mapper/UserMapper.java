package com.careidentityservice.mapper;

import com.careidentityservice.dto.UserResponse;
import com.careidentityservice.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "twoFactorEnabled", ignore = true)
    UserResponse toUserResponse(User user);
}