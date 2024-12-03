package com.anonymous.service;

import com.anonymous.dto.request.StockInInsertRequest;
import com.anonymous.dto.request.StockInUpdateRequest;
import com.anonymous.dto.response.StockInResponse;

import java.util.List;

public interface IStockInService {

    void insert(StockInInsertRequest stockInInsertRequest);

    void update(String oldStockInId, StockInUpdateRequest request);

    void delete(String[] stockInIds);

    List<StockInResponse> getAll();

    StockInResponse getById(String stockInId);
    
}
