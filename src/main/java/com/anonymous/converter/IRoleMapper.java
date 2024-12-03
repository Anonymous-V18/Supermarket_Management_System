package com.anonymous.converter;

import com.anonymous.dto.response.RoleResponse;
import com.anonymous.entity.Role;
import org.mapstruct.Mapper;

@Mapper
public interface IRoleMapper {

    RoleResponse toDTO(Role role);

}
