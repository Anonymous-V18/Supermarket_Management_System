package com.anonymous.service.impl;

import com.anonymous.converter.ICityMapper;
import com.anonymous.dto.response.CityResponse;
import com.anonymous.repository.ICityRepository;
import com.anonymous.service.ICityService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CityService implements ICityService {

    ICityRepository cityRepository;
    ICityMapper cityMapper;

    @Override
    public List<CityResponse> getAll() {
        return cityRepository.findAll().stream().map(cityMapper::toDTO).toList();
    }
    
}
