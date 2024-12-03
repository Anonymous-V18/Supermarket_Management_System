package com.anonymous.service.impl;

import com.anonymous.dto.request.StockOutDetailInsertRequest;
import com.anonymous.dto.request.StockOutDetailUpdateRequest;
import com.anonymous.entity.*;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IProductRepository;
import com.anonymous.repository.IStockOutDetailRepository;
import com.anonymous.service.IStockOutDetailService;
import com.anonymous.service.IWarehouseProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockOutDetailService implements IStockOutDetailService {

    IStockOutDetailRepository stockOutDetailRepository;
    IProductRepository productRepository;
    IWarehouseProductService warehouseProductService;

    @Override
    @Transactional
    public void insert(Set<StockOutDetailInsertRequest> stockOutDetailInsertRequests, StockOut stockOut) {
        Set<StockOutDetail> stockOutDetails = new HashSet<>();
        stockOutDetailInsertRequests.forEach(stockOutDetailInsertRequest -> {
            Product product = productRepository.findById(stockOutDetailInsertRequest.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));

            StockInDetail stockInDetail = product.getStockInDetails().stream()
                    .filter(i -> i.getStockIn().getWarehouse().getId().equals(stockOut.getWarehouse().getId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.STOCK_IN_DETAIL_NOT_EXIST));

            Double inputPriceOfProduct = stockInDetail.getInputPrice();
            Double salePriceOfProduct = stockInDetail.getSalePrice();
            Double totalPriceOfProduct = salePriceOfProduct * stockOutDetailInsertRequest.getQuantity();

            StockOutDetail stockOutDetail = StockOutDetail.builder()
                    .product(product)
                    .quantity(stockOutDetailInsertRequest.getQuantity())
                    .inputPrice(inputPriceOfProduct)
                    .salePrice(salePriceOfProduct)
                    .totalPrice(totalPriceOfProduct)
                    .stockOut(stockOut)
                    .build();

            stockOutDetails.add(stockOutDetail);
            warehouseProductService.update(product, stockOut.getWarehouse(), 0, stockOutDetailInsertRequest.getQuantity());
        });

        stockOutDetailRepository.saveAll(stockOutDetails);
    }

    @Override
    public void update(Set<StockOutDetail> oldStockOutDetails, Set<StockOutDetailUpdateRequest> stockOutDetailUpdateRequests) {
        StockOut stockOut = oldStockOutDetails.stream().findFirst()
                .map(StockOutDetail::getStockOut)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_OUT_NOT_EXIST));

        Warehouse warehouse = stockOut.getWarehouse();

        Set<StockOutDetail> stockOutDetails = new HashSet<>();

        stockOutDetailUpdateRequests.forEach(stockOutDetailUpdateRequest -> {
            StockOutDetail stockOutDetail = oldStockOutDetails.stream()
                    .filter(oldStockOutDetail -> oldStockOutDetail.getId().equals(stockOutDetailUpdateRequest.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.STOCK_OUT_DETAIL_NOT_EXIST));

            Product product = stockOutDetail.getProduct();

            Integer oldQuantitySold = stockOutDetail.getQuantity();
            Integer newQuantitySold = stockOutDetailUpdateRequest.getQuantity();

            StockInDetail stockInDetail = product.getStockInDetails().stream()
                    .filter(i -> i.getStockIn().getWarehouse().getId().equals(warehouse.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.STOCK_IN_DETAIL_NOT_EXIST));

            Double inputPriceOfProduct = stockInDetail.getInputPrice();
            Double salePriceOfProduct = stockInDetail.getSalePrice();
            Double totalPriceOfProduct = salePriceOfProduct * newQuantitySold;

            stockOutDetail.setInputPrice(inputPriceOfProduct);
            stockOutDetail.setSalePrice(salePriceOfProduct);
            stockOutDetail.setTotalPrice(totalPriceOfProduct);
            stockOutDetail.setQuantity(newQuantitySold);

            stockOutDetails.add(stockOutDetail);

            warehouseProductService.update(product, stockOut.getWarehouse(), oldQuantitySold, newQuantitySold);
        });

        stockOutDetailRepository.saveAll(stockOutDetails);
    }

    @Override
    public void delete(Set<StockOutDetail> oldStockOutDetails) {
        oldStockOutDetails.forEach(oldStockOutDetail -> {
            Integer oldQuantitySold = oldStockOutDetail.getQuantity();
            Integer newQuantitySold = 0;
            warehouseProductService.update(oldStockOutDetail.getProduct(), oldStockOutDetail.getStockOut().getWarehouse(),
                    oldQuantitySold, newQuantitySold);
        });
        stockOutDetailRepository.deleteAll(oldStockOutDetails);
    }

}
