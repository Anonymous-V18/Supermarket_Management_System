package com.anonymous.service.impl;

import com.anonymous.converter.IBrandMapper;
import com.anonymous.dto.response.BrandResponse;
import com.anonymous.repository.IBrandRepository;
import com.anonymous.service.IBrandService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrandService implements IBrandService {

    IBrandRepository brandRepository;
    IBrandMapper brandMapper;

    @Override
    public List<BrandResponse> getAll() {
        return brandRepository.findAll().stream().map(brandMapper::toDTO).toList();
    }
}
