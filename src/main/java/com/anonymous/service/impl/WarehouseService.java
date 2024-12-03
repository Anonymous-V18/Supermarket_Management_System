package com.anonymous.service.impl;

import com.anonymous.converter.IWarehouseMapper;
import com.anonymous.dto.request.WarehouseInsertRequest;
import com.anonymous.dto.request.WarehouseUpdateRequest;
import com.anonymous.dto.response.WarehouseResponse;
import com.anonymous.dto.response.WarehouseToSelectionResponse;
import com.anonymous.entity.Address;
import com.anonymous.entity.Warehouse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IWarehouseRepository;
import com.anonymous.service.IAddressService;
import com.anonymous.service.IWarehouseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WarehouseService implements IWarehouseService {

    IWarehouseRepository warehouseRepository;
    IWarehouseMapper warehouseMapper;
    IAddressService addressService;

    @Override
    @Transactional
    public WarehouseResponse insert(WarehouseInsertRequest warehouseInsertRequest) {
        Warehouse warehouse = warehouseMapper.toEntity(warehouseInsertRequest);
        Address address = addressService.insert(warehouseInsertRequest.getNewAddress());
        warehouse.setAddress(address);
        warehouse = warehouseRepository.save(warehouse);
        return warehouseMapper.toDTO(warehouse);
    }

    @Override
    public WarehouseResponse update(String oldWarehouseId, WarehouseUpdateRequest warehouseUpdateRequest) {
        Warehouse warehouse = warehouseRepository.findById(oldWarehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));

        warehouse = warehouseMapper.toEntity(warehouse, warehouseUpdateRequest);

        Address address = addressService.update(warehouseUpdateRequest.getCurrentAddress());
        warehouse.setAddress(address);

        warehouse = warehouseRepository.save(warehouse);
        return warehouseMapper.toDTO(warehouse);
    }

    @Override
    public List<WarehouseResponse> getAll() {
        return warehouseRepository.findAll().stream().map(warehouseMapper::toDTO).toList();
    }

    @Override
    public List<WarehouseToSelectionResponse> getAllToSelection() {
        return warehouseRepository.findAll().stream().map(warehouseMapper::toSelectionDTO).toList();
    }

    @Override
    public WarehouseResponse getById(String warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_EXIST));
        return warehouseMapper.toDTO(warehouse);
    }
    
}
