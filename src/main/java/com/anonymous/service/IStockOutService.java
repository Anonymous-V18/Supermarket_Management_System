package com.anonymous.service;

import com.anonymous.dto.request.StockOutInsertRequest;
import com.anonymous.dto.request.StockOutUpdateRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockOutResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IStockOutService {

    StockOutResponse insert(StockOutInsertRequest request);

    StockOutResponse update(String id, StockOutUpdateRequest request);

    void delete(String[] ids);

    List<StockOutResponse> getAll();

    PageResponse<StockOutResponse> search(String keyword, String statusCode, Pageable pageable);

    StockOutResponse getById(String id);

}
