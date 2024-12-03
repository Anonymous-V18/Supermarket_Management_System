package com.anonymous.service;

import com.anonymous.dto.request.StockInDetailInsertRequest;
import com.anonymous.dto.request.StockInDetailUpdateRequest;
import com.anonymous.entity.StockIn;
import com.anonymous.entity.StockInDetail;

import java.util.Set;

public interface IStockInDetailService {

    void insert(Set<StockInDetailInsertRequest> stockInDetailInsertRequests, StockIn stockIn);

    void update(Set<StockInDetail> oldStockInDetails, Set<StockInDetailUpdateRequest> stockInDetailUpdateRequests);

    void delete(Set<StockInDetail> oldStockInDetails);
}
