package com.anonymous.service.impl;

import com.anonymous.converter.IRoleMapper;
import com.anonymous.dto.response.RoleResponse;
import com.anonymous.repository.IRoleRepository;
import com.anonymous.service.IRoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService implements IRoleService {

    IRoleRepository roleRepository;
    IRoleMapper roleMapper;

    @Override
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream().map(roleMapper::toDTO).toList();
    }

}
