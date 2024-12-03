package com.anonymous.service.impl;

import com.anonymous.dto.request.InvoiceDetailInsertRequest;
import com.anonymous.dto.request.InvoiceDetailUpdateRequest;
import com.anonymous.entity.*;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IInvoiceDetailRepository;
import com.anonymous.repository.IProductRepository;
import com.anonymous.service.IInvoiceDetailService;
import com.anonymous.service.IWarehouseProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceDetailService implements IInvoiceDetailService {

    IInvoiceDetailRepository invoiceDetailRepository;
    IProductRepository productRepository;
    IWarehouseProductService warehouseProductService;

    @Override
    public void insert(Set<InvoiceDetailInsertRequest> invoiceDetailInsertRequests, Invoice invoice) {
        Set<InvoiceDetail> invoiceDetails = new HashSet<>();

        invoiceDetailInsertRequests.forEach(invoiceDetailInsertRequest -> {
            Product product = productRepository.findById(invoiceDetailInsertRequest.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));


            StockInDetail stockInDetail = product.getStockInDetails().stream()
                    .filter(i -> i.getStockIn()
                            .getWarehouse().getId().equals(invoice.getWarehouse().getId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.STOCK_IN_DETAIL_NOT_EXIST));
            Double inputPriceOfProduct = stockInDetail.getInputPrice();
            Double salePriceOfProduct = stockInDetail.getSalePrice();

            Integer newQuantitySold = invoiceDetailInsertRequest.getQuantity();

            InvoiceDetail invoiceDetail = InvoiceDetail.builder()
                    .product(product)
                    .quantity(newQuantitySold)
                    .inputPrice(inputPriceOfProduct)
                    .salePrice(salePriceOfProduct)
                    .promotionalPrice(invoiceDetailInsertRequest.getPromotionalPrice())
                    .percentDiscount(invoiceDetailInsertRequest.getPercentDiscount())
                    .invoice(invoice)
                    .build();

            invoiceDetails.add(invoiceDetail);

            warehouseProductService.update(product, invoice.getWarehouse(), 0, newQuantitySold);
        });

        invoiceDetailRepository.saveAll(invoiceDetails);
    }

    @Override
    public void update(Set<InvoiceDetail> oldInvoiceDetails, Set<InvoiceDetailUpdateRequest> invoiceDetailUpdateRequests) {
        Invoice invoice = oldInvoiceDetails.stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_DETAIL_NOT_EXIST))
                .getInvoice();

        Warehouse warehouse = invoice.getWarehouse();

        Set<InvoiceDetail> invoiceDetails = new HashSet<>();

        invoiceDetailUpdateRequests.forEach(invoiceDetailRequest -> {
            InvoiceDetail invoiceDetail = oldInvoiceDetails.stream()
                    .filter(oldInvoiceDetail -> oldInvoiceDetail.getId().equals(invoiceDetailRequest.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.INVOICE_DETAIL_NOT_EXIST));
            Product product = invoiceDetail.getProduct();


            StockInDetail stockInDetail = product.getStockInDetails().stream()
                    .filter(i -> i.getStockIn().getWarehouse().getId().equals(warehouse.getId()))
                    .max(Comparator.comparing(i -> i.getStockIn().getCreatedDate()))
                    .orElseThrow(() -> new AppException(ErrorCode.STOCK_IN_DETAIL_NOT_EXIST));
            Double inputPriceOfProduct = stockInDetail.getInputPrice();
            Double salePriceOfProduct = stockInDetail.getSalePrice();

            Integer oldQuantitySold = invoiceDetail.getQuantity();
            Integer newQuantitySold = invoiceDetailRequest.getQuantity();

            invoiceDetail.setInputPrice(inputPriceOfProduct);
            invoiceDetail.setSalePrice(salePriceOfProduct);
            invoiceDetail.setQuantity(newQuantitySold);
            invoiceDetail.setPromotionalPrice(invoiceDetailRequest.getPromotionalPrice());
            invoiceDetail.setPercentDiscount(invoiceDetailRequest.getPercentDiscount());

            invoiceDetails.add(invoiceDetail);

            warehouseProductService.update(product, invoice.getWarehouse(), oldQuantitySold, newQuantitySold);
        });

        invoiceDetailRepository.saveAll(invoiceDetails);
    }

    @Override
    public void delete(Set<InvoiceDetail> oldInvoiceDetails) {
        oldInvoiceDetails.forEach(invoiceDetail -> {
                    Integer oldQuantitySold = invoiceDetail.getQuantity();
                    Integer newQuantitySold = 0;
                    warehouseProductService.update(invoiceDetail.getProduct(), invoiceDetail.getInvoice().getWarehouse(),
                            oldQuantitySold, newQuantitySold);
                }
        );
        invoiceDetailRepository.deleteAll(oldInvoiceDetails);
    }

}
