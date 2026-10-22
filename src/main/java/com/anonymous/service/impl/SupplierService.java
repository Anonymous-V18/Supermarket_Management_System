package com.anonymous.service.impl;

import com.anonymous.converter.ISupplierMapper;
import com.anonymous.dto.request.SupplierInsertRequest;
import com.anonymous.dto.request.SupplierUpdateRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.SupplierResponse;
import com.anonymous.dto.response.SupplierToSelectionResponse;
import com.anonymous.entity.Address;
import com.anonymous.entity.Supplier;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.ISupplierRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.IAddressService;
import com.anonymous.service.ISupplierService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierService implements ISupplierService {

    ISupplierRepository supplierRepository;
    ISupplierMapper supplierMapper;
    IAddressService addressService;

    @Override
    @Transactional
    public SupplierResponse insert(SupplierInsertRequest supplierInsertRequest) {
        Supplier supplier = supplierMapper.toEntity(supplierInsertRequest);
        Address address = addressService.insert(supplierInsertRequest.getNewAddress());
        supplier.setAddress(address);
        supplier = supplierRepository.save(supplier);
        return supplierMapper.toDTO(supplier);
    }

    @Override
    public SupplierResponse update(String oldSupplierId, SupplierUpdateRequest supplierUpdateRequest) {
        Supplier supplier = supplierRepository.findByIdAndIsDeletedFalse(oldSupplierId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_EXIST));
        supplier = supplierMapper.toEntity(supplier, supplierUpdateRequest);

        Address address = addressService.update(supplierUpdateRequest.getCurrentAddress());
        supplier.setAddress(address);

        supplier = supplierRepository.save(supplier);

        return supplierMapper.toDTO(supplier);
    }

    @Override
    public List<SupplierResponse> getAll() {
        return supplierRepository.findAllByIsDeletedFalse().stream().map(supplierMapper::toDTO).toList();
    }

    @Override
    public List<SupplierToSelectionResponse> getAllToSelection() {
        return supplierRepository.findAllByIsDeletedFalse().stream().map(supplierMapper::toSelectionDTO).toList();
    }

    @Override
    public SupplierResponse getById(String supplierId) {
        Supplier supplier = supplierRepository.findByIdAndIsDeletedFalse(supplierId).orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_EXIST));
        return supplierMapper.toDTO(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> search(String keyword, Pageable pageable) {
        return PageResponse.of(supplierRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                SearchSpecs.keyword(keyword, "name", "phoneNumber", "email")
        ), pageable), supplierMapper::toDTO);
    }

}
