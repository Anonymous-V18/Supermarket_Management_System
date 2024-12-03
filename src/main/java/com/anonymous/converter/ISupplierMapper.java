package com.anonymous.converter;

import com.anonymous.dto.request.SupplierInsertRequest;
import com.anonymous.dto.request.SupplierUpdateRequest;
import com.anonymous.dto.response.SupplierResponse;
import com.anonymous.dto.response.SupplierToSelectionResponse;
import com.anonymous.entity.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ISupplierMapper {

    SupplierResponse toDTO(Supplier supplier);

    SupplierToSelectionResponse toSelectionDTO(Supplier supplier);

    @Mapping(target = "stockIns", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "address", ignore = true)
    Supplier toEntity(SupplierInsertRequest supplierInsertRequest);

    Supplier toEntity(@MappingTarget Supplier supplier, SupplierUpdateRequest supplierUpdateRequest);

}
