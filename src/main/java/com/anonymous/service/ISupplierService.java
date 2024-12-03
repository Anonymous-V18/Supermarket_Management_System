package com.anonymous.service;

import com.anonymous.dto.request.SupplierInsertRequest;
import com.anonymous.dto.request.SupplierUpdateRequest;
import com.anonymous.dto.response.SupplierResponse;
import com.anonymous.dto.response.SupplierToSelectionResponse;

import java.util.List;

public interface ISupplierService {

    SupplierResponse insert(SupplierInsertRequest supplierInsertRequest);

    SupplierResponse update(String oldSupplierId, SupplierUpdateRequest supplierUpdateRequest);

    List<SupplierResponse> getAll();

    List<SupplierToSelectionResponse> getAllToSelection();

    SupplierResponse getById(String supplierId);
}
