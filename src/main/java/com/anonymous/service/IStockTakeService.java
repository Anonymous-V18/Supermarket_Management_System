package com.anonymous.service;

import com.anonymous.dto.request.StockTakeInsertRequest;
import com.anonymous.dto.request.StockTakeRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockTakeResponse;
import org.springframework.data.domain.Pageable;

public interface IStockTakeService {

    StockTakeResponse insert(StockTakeInsertRequest request);

    StockTakeResponse update(String id, StockTakeRequest request);

    void delete(String[] ids);

    PageResponse<StockTakeResponse> search(String keyword, String statusCode, Pageable pageable);

    StockTakeResponse getById(String id);

}
