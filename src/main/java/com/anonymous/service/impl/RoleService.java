package com.anonymous.service.impl;

import com.anonymous.client.IdentityClient;
import com.anonymous.dto.response.RoleResponse;
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

    IdentityClient identityClient;

    @Override
    public List<RoleResponse> findAll() {
        return identityClient.getRoles();
    }

}
