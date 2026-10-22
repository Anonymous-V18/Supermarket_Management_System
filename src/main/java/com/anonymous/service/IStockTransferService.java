package com.anonymous.service;

import com.anonymous.dto.request.StockTransferInsertRequest;
import com.anonymous.dto.request.StockTransferRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.StockTransferResponse;
import org.springframework.data.domain.Pageable;

public interface IStockTransferService {

    StockTransferResponse insert(StockTransferInsertRequest request);

    StockTransferResponse update(String id, StockTransferRequest request);

    void delete(String[] ids);

    PageResponse<StockTransferResponse> search(String keyword, String statusCode, Pageable pageable);

    StockTransferResponse getById(String id);

}
