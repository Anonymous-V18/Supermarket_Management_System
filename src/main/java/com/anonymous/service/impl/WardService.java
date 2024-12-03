package com.anonymous.service.impl;

import com.anonymous.converter.IWardMapper;
import com.anonymous.dto.response.WardResponse;
import com.anonymous.repository.IWardRepository;
import com.anonymous.service.IWardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WardService implements IWardService {

    IWardRepository wardRepository;
    IWardMapper wardMapper;

    @Override
    public List<WardResponse> getAll() {
        return wardRepository.findAll().stream().map(wardMapper::toDTO).toList();
    }

    @Override
    public List<WardResponse> getAllByDistrictId(String districtId) {
        return wardRepository.findByDistrict_Id(districtId).stream().map(wardMapper::toDTO).toList();
    }
    
}
