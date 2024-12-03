package com.anonymous.service.impl;

import com.anonymous.converter.IUnitMapper;
import com.anonymous.dto.response.UnitResponse;
import com.anonymous.repository.IUnitRepository;
import com.anonymous.service.IUnitService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UnitService implements IUnitService {

    IUnitRepository unitRepository;
    IUnitMapper unitMapper;

    @Override
    public List<UnitResponse> getAll() {
        return unitRepository.findAll().stream().map(unitMapper::toDTO).toList();
    }
}
