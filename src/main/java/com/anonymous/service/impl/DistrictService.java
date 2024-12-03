package com.anonymous.service.impl;

import com.anonymous.converter.IDistrictMapper;
import com.anonymous.dto.response.DistrictResponse;
import com.anonymous.repository.IDistrictRepository;
import com.anonymous.service.IDistrictService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DistrictService implements IDistrictService {

    IDistrictRepository districtRepository;
    IDistrictMapper districtMapper;

    @Override
    public List<DistrictResponse> getAll() {
        return districtRepository.findAll().stream().map(districtMapper::toDTO).toList();
    }

    @Override
    public List<DistrictResponse> getAllByCityId(String cityId) {
        return districtRepository.findByCity_Id(cityId).stream().map(districtMapper::toDTO).toList();
    }
    
}
