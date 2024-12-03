package com.anonymous.service;

import com.anonymous.dto.response.DistrictResponse;

import java.util.List;

public interface IDistrictService {

    List<DistrictResponse> getAll();

    List<DistrictResponse> getAllByCityId(String cityId);
    
}
