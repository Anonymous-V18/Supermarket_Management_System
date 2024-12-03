package com.anonymous.service;

import com.anonymous.dto.request.StockOutDetailInsertRequest;
import com.anonymous.dto.request.StockOutDetailUpdateRequest;
import com.anonymous.entity.StockOut;
import com.anonymous.entity.StockOutDetail;

import java.util.Set;

public interface IStockOutDetailService {

    void insert(Set<StockOutDetailInsertRequest> stockOutDetailInsertRequests, StockOut stockOut);

    void update(Set<StockOutDetail> oldStockOutDetails, Set<StockOutDetailUpdateRequest> stockOutDetailUpdateRequests);

    void delete(Set<StockOutDetail> oldStockOutDetails);
}
