package com.anonymous.service;

import com.anonymous.dto.response.RoleResponse;

import java.util.List;

public interface IRoleService {

    List<RoleResponse> findAll();

}
