package com.anonymous.service.impl;

import com.anonymous.converter.IStatusProductMapper;
import com.anonymous.dto.response.StatusProductResponse;
import com.anonymous.repository.IStatusProductRepository;
import com.anonymous.service.IStatusProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatusProductService implements IStatusProductService {

    IStatusProductRepository statusProductRepository;
    IStatusProductMapper statusProductMapper;

    @Override
    public List<StatusProductResponse> getAll() {
        return statusProductRepository.findAll().stream().map(statusProductMapper::toDTO).toList();
    }
    
}
