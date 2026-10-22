package com.anonymous.service;

import com.anonymous.dto.request.SupplierInsertRequest;
import com.anonymous.dto.request.SupplierUpdateRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.SupplierResponse;
import com.anonymous.dto.response.SupplierToSelectionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ISupplierService {

    SupplierResponse insert(SupplierInsertRequest supplierInsertRequest);

    SupplierResponse update(String oldSupplierId, SupplierUpdateRequest supplierUpdateRequest);

    List<SupplierResponse> getAll();

    List<SupplierToSelectionResponse> getAllToSelection();

    SupplierResponse getById(String supplierId);

    PageResponse<SupplierResponse> search(String keyword, Pageable pageable);

}
