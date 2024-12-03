package com.anonymous.converter;

import com.anonymous.dto.request.WarehouseInsertRequest;
import com.anonymous.dto.request.WarehouseUpdateRequest;
import com.anonymous.dto.response.WarehouseResponse;
import com.anonymous.dto.response.WarehouseToSelectionResponse;
import com.anonymous.entity.Warehouse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface IWarehouseMapper {

    WarehouseResponse toDTO(Warehouse warehouse);

    WarehouseToSelectionResponse toSelectionDTO(Warehouse warehouse);

    Warehouse toEntity(WarehouseInsertRequest warehouseInsertRequest);

    Warehouse toEntity(@MappingTarget Warehouse oldWarehouse, WarehouseUpdateRequest warehouseUpdateRequest);

}
