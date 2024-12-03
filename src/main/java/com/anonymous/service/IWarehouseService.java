package com.anonymous.service;

import com.anonymous.dto.request.WarehouseInsertRequest;
import com.anonymous.dto.request.WarehouseUpdateRequest;
import com.anonymous.dto.response.WarehouseResponse;
import com.anonymous.dto.response.WarehouseToSelectionResponse;

import java.util.List;

public interface IWarehouseService {

    WarehouseResponse insert(WarehouseInsertRequest warehouseInsertRequest);

    WarehouseResponse update(String oldWarehouseId, WarehouseUpdateRequest warehouseUpdateRequest);

    List<WarehouseResponse> getAll();

    List<WarehouseToSelectionResponse> getAllToSelection();

    WarehouseResponse getById(String warehouseId);
}
