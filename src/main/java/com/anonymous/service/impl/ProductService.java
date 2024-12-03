package com.anonymous.service.impl;

import com.anonymous.converter.IProductMapper;
import com.anonymous.dto.request.ProductRequest;
import com.anonymous.dto.response.ProductDetailResponse;
import com.anonymous.dto.response.ProductResponse;
import com.anonymous.entity.*;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.*;
import com.anonymous.service.IProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService implements IProductService {

    IProductRepository productRepository;
    IProductMapper productMapper;
    IBrandRepository brandRepository;
    IProductCategoryRepository productCategoryRepository;
    IUnitRepository unitRepository;
    ISupplierRepository supplierRepository;
    IStockInDetailRepository stockInDetailRepository;

    @Override
    public ProductResponse insert(ProductRequest productRequest) {
        Product product = productMapper.toEntity(productRequest);

        Brand brand = brandRepository.findById(productRequest.getBrandId())
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_EXIST));

        Supplier supplier = supplierRepository.findById(productRequest.getSupplierId())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_EXIST));

        ProductCategory productCategory = productCategoryRepository.findById(productRequest.getProductCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXIST));

        Unit unit = unitRepository.findById(productRequest.getUnitId())
                .orElseThrow(() -> new AppException(ErrorCode.UNIT_NOT_EXIST));

        product.setBrand(brand);
        product.setProductCategory(productCategory);
        product.setUnit(unit);
        product.setSupplier(supplier);
        product = productRepository.save(product);
        return productMapper.toDTO(product);
    }

    @Override
    public ProductResponse update(String oldProductId, ProductRequest productRequest) {
        Product product = productRepository.findById(oldProductId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));

        product = productMapper.toEntity(product, productRequest);

        if (!product.getBrand().getId().equals(productRequest.getBrandId())) {
            Brand brand = brandRepository.findById(productRequest.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_EXIST));
            product.setBrand(brand);
        }

        if (!product.getSupplier().getId().equals(productRequest.getSupplierId())) {
            Supplier supplier = supplierRepository.findById(productRequest.getSupplierId())
                    .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_EXIST));
            product.setSupplier(supplier);
        }

        if (!product.getProductCategory().getId().equals(productRequest.getProductCategoryId())) {
            ProductCategory productCategory = productCategoryRepository.findById(productRequest.getProductCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXIST));
            product.setProductCategory(productCategory);
        }

        if (!product.getUnit().getId().equals(productRequest.getUnitId())) {
            Unit unit = unitRepository.findById(productRequest.getUnitId())
                    .orElseThrow(() -> new AppException(ErrorCode.UNIT_NOT_EXIST));
            product.setUnit(unit);
        }

        product = productRepository.save(product);

        return productMapper.toDTO(product);
    }

    @Override
    public void delete(String[] productIds) {
        try {
            productRepository.deleteAllById(Arrays.stream(productIds).toList());
        } catch (Exception e) {
            throw new AppException(ErrorCode.CAN_NOT_DELETE_PRODUCT);
        }
    }

    @Override
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream().map(productMapper::toDTO).toList();
    }

    @Override
    public ProductDetailResponse getDetail(String productId, String warehouseId) {
        StockInDetail stockInDetail = stockInDetailRepository.getDetailProductInStockInDateNearest(warehouseId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));
        Product product = stockInDetail.getProduct();
        ProductDetailResponse productDetailResponse = productMapper.toCommonDetailDTO(product);
        productDetailResponse.setSalePrice(stockInDetail.getSalePrice());
        return productDetailResponse;
    }

    @Override
    public ProductResponse getDetail(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));
        return productMapper.toDTO(product);
    }
}
