package com.anonymous.service.impl;

import com.anonymous.dto.request.StockInDetailInsertRequest;
import com.anonymous.dto.request.StockInDetailUpdateRequest;
import com.anonymous.entity.Product;
import com.anonymous.entity.StockIn;
import com.anonymous.entity.StockInDetail;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IProductRepository;
import com.anonymous.repository.IStockInDetailRepository;
import com.anonymous.service.IStockInDetailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockInDetailService implements IStockInDetailService {

    IStockInDetailRepository stockInDetailRepository;
    IProductRepository productRepository;

    @Override
    public void insert(Set<StockInDetailInsertRequest> stockInDetailInsertRequests, StockIn stockIn) {
        Set<StockInDetail> stockInDetails = new HashSet<>();
        stockInDetailInsertRequests.forEach(stockInDetailInsertRequest -> {
            Product product = productRepository.findById(stockInDetailInsertRequest.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));

            StockInDetail stockInDetail = StockInDetail.builder()
                    .product(product)
                    .quantity(stockInDetailInsertRequest.getQuantity())
                    .inputPrice(stockInDetailInsertRequest.getInputPrice())
                    .salePrice(stockInDetailInsertRequest.getSalePrice())
                    .stockIn(stockIn)
                    .build();

            stockInDetails.add(stockInDetail);
        });

        stockInDetailRepository.saveAll(stockInDetails);
    }

    @Override
    public void update(Set<StockInDetail> oldStockInDetails, Set<StockInDetailUpdateRequest> stockInDetailUpdateRequests) {
        Set<StockInDetail> newStockInDetails = new HashSet<>();
        
        stockInDetailUpdateRequests.forEach(stockInDetailUpdateRequest -> {
            StockInDetail oldStockInDetail = oldStockInDetails.stream()
                    .filter(i -> i.getId().equals(stockInDetailUpdateRequest.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.STOCK_IN_DETAIL_NOT_EXIST));

            oldStockInDetail.setQuantity(stockInDetailUpdateRequest.getQuantity());
            oldStockInDetail.setInputPrice(stockInDetailUpdateRequest.getInputPrice());
            oldStockInDetail.setSalePrice(stockInDetailUpdateRequest.getSalePrice());

            newStockInDetails.add(oldStockInDetail);
        });

        stockInDetailRepository.saveAll(newStockInDetails);
    }

    @Override
    public void delete(Set<StockInDetail> oldStockInDetails) {
        stockInDetailRepository.deleteAll(oldStockInDetails);
    }
}
