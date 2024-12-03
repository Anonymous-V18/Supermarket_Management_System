package com.anonymous.converter;

import com.anonymous.dto.request.UserInsertRequest;
import com.anonymous.dto.response.UserResponse;
import com.anonymous.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface IUserMapper {
    
    UserResponse toDTO(User user);

    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toEntity(UserInsertRequest request);

}
