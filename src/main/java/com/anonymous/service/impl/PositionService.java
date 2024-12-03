package com.anonymous.service.impl;

import com.anonymous.converter.IPositionMapper;
import com.anonymous.dto.response.PositionResponse;
import com.anonymous.repository.IPositionRepository;
import com.anonymous.service.IPositionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PositionService implements IPositionService {

    IPositionRepository positionRepository;
    IPositionMapper positionMapper;

    @Override
    public List<PositionResponse> getAll() {
        return positionRepository.findAll().stream().map(positionMapper::toDTO).toList();
    }
    
}
