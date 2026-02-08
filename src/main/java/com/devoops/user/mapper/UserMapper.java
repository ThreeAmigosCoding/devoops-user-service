package com.devoops.user.mapper;

import com.devoops.user.dto.request.RegisterRequest;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    User toEntity(RegisterRequest request);
}
