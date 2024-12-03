package com.anonymous.service;

import com.anonymous.dto.request.StockOutInsertRequest;
import com.anonymous.dto.request.StockOutUpdateRequest;
import com.anonymous.dto.response.StockOutResponse;

import java.util.List;

public interface IStockOutService {

    void insert(StockOutInsertRequest stockOutInsertRequest);

    void update(String oldStockOutId, StockOutUpdateRequest stockOutUpdateRequest);

    void delete(String[] stockOutIds);

    List<StockOutResponse> getAll();

    StockOutResponse getById(String stockOutId);

}
