package com.anonymous.service;

import com.anonymous.dto.request.StockInInsertRequest;
import com.anonymous.dto.request.StockInUpdateRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockInResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IStockInService {

    StockInResponse insert(StockInInsertRequest request);

    StockInResponse update(String id, StockInUpdateRequest request);

    void delete(String[] ids);

    List<StockInResponse> getAll();

    PageResponse<StockInResponse> search(String keyword, String statusCode, Pageable pageable);

    StockInResponse getById(String id);

}
