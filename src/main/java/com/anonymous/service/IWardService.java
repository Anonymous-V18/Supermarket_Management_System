package com.anonymous.service;

import com.anonymous.dto.response.WardResponse;

import java.util.List;

public interface IWardService {

    List<WardResponse> getAll();

    List<WardResponse> getAllByDistrictId(String districtId);

}
