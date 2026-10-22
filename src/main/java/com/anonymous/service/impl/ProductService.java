package com.anonymous.service.impl;

import com.anonymous.converter.IProductMapper;
import com.anonymous.dto.request.ProductRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.ProductDetailResponse;
import com.anonymous.dto.response.ProductResponse;
import com.anonymous.entity.Brand;
import com.anonymous.entity.Product;
import com.anonymous.entity.Supplier;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IBrandRepository;
import com.anonymous.repository.IProductCategoryRepository;
import com.anonymous.repository.IProductRepository;
import com.anonymous.repository.ISupplierRepository;
import com.anonymous.repository.IUnitRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.IProductService;
import com.anonymous.service.IWarehouseProductService;
import com.anonymous.utils.MoneyUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    IWarehouseProductService warehouseProductService;
    ICodeGeneratorService codeGeneratorService;

    @Override
    @Transactional
    public ProductResponse insert(ProductRequest productRequest) {
        Product product = productMapper.toEntity(productRequest);
        product.setCode(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_PRODUCT));
        // Purchase price is only known once goods are received
        product.setCostPrice(BigDecimal.ZERO);
        applyRequest(product, productRequest);
        return productMapper.toDTO(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(String productId, ProductRequest productRequest) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));
        product = productMapper.toEntity(product, productRequest);
        applyRequest(product, productRequest);
        return productMapper.toDTO(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(String[] productIds) {
        // Soft delete: old invoices / stock documents keep showing the product, it just can't be used anymore
        List<Product> products = productRepository.findAllByIdInAndIsDeletedFalse(Arrays.asList(productIds));
        products.forEach(Product::markDeleted);
        productRepository.saveAll(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAllByIsDeletedFalse().stream().map(productMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String keyword, Pageable pageable) {
        return PageResponse.of(productRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                SearchSpecs.keyword(keyword, "code", "barcode", "name", "brand.name", "productCategory.name")
        ), pageable), productMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getDetail(String productId, String warehouseId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));
        return toSaleDetail(product, warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getByBarcode(String barcode, String warehouseId) {
        Product product = productRepository.findByBarcodeAndIsDeletedFalse(barcode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));
        return toSaleDetail(product, warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getDetail(String productId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));
        return productMapper.toDTO(product);
    }

    private ProductDetailResponse toSaleDetail(Product product, String warehouseId) {
        ProductDetailResponse response = productMapper.toCommonDetailDTO(product);
        response.setAvailableQuantity(warehouseProductService.getAvailableQuantity(warehouseId, product.getId()));
        return response;
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setBarcode(normalizeBarcode(product, request.getBarcode()));
        product.setSalePrice(MoneyUtil.round(request.getSalePrice()));
        product.setProductCategory(productCategoryRepository.findByIdAndIsDeletedFalse(request.getProductCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_EXIST)));
        product.setUnit(unitRepository.findByIdAndIsDeletedFalse(request.getUnitId())
                .orElseThrow(() -> new AppException(ErrorCode.UNIT_NOT_EXIST)));
        product.setBrand(optionalBrand(request.getBrandId()));
        product.setSupplier(optionalSupplier(request.getSupplierId()));
    }

    // Empty barcode is stored as NULL so several products may have none
    private String normalizeBarcode(Product product, String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }
        String value = barcode.trim();
        productRepository.findByBarcode(value)
                .filter(other -> !other.getId().equals(product.getId()))
                .ifPresent(other -> {
                    throw new AppException(ErrorCode.BARCODE_EXISTED);
                });
        return value;
    }

    private Brand optionalBrand(String brandId) {
        if (brandId == null || brandId.isBlank()) {
            return null;
        }
        return brandRepository.findByIdAndIsDeletedFalse(brandId)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_EXIST));
    }

    private Supplier optionalSupplier(String supplierId) {
        if (supplierId == null || supplierId.isBlank()) {
            return null;
        }
        return supplierRepository.findByIdAndIsDeletedFalse(supplierId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_EXIST));
    }

}
