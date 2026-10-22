package com.anonymous.service.impl;

import com.anonymous.entity.Product;
import com.anonymous.entity.Warehouse;
import com.anonymous.entity.WarehouseProduct;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IWarehouseProductRepository;
import com.anonymous.service.IWarehouseProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseProductServiceTest {

    @Mock
    IWarehouseProductRepository warehouseProductRepository;

    @InjectMocks
    WarehouseProductService warehouseProductService;

    Warehouse warehouse;
    Product milk;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId("wh-1");
        milk = new Product();
        milk.setId("prd-milk");
    }

    @Test
    void firstReceiptCreatesTheStockRow() {
        when(warehouseProductRepository.findForUpdate("wh-1", "prd-milk")).thenReturn(Optional.empty());

        warehouseProductService.applyChange(warehouse, Map.of(), Map.of(milk, 10), IWarehouseProductService.INCOMING);

        ArgumentCaptor<WarehouseProduct> saved = ArgumentCaptor.forClass(WarehouseProduct.class);
        verify(warehouseProductRepository).save(saved.capture());
        assertThat(saved.getValue().getAvailableQuantity()).isEqualTo(10);
        assertThat(saved.getValue().getProduct()).isSameAs(milk);
    }

    @Test
    void sellingMoreThanAvailableIsRejected() {
        when(warehouseProductRepository.findForUpdate("wh-1", "prd-milk")).thenReturn(Optional.of(stock(3)));

        assertThatThrownBy(() -> warehouseProductService.applyChange(warehouse, Map.of(), Map.of(milk, 5),
                IWarehouseProductService.OUTGOING))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STOCK_QUANTITY_INVALID);
        verify(warehouseProductRepository, never()).save(any());
    }

    @Test
    void editingADocumentOnlyMovesTheDifference() {
        WarehouseProduct row = stock(20);
        when(warehouseProductRepository.findForUpdate("wh-1", "prd-milk")).thenReturn(Optional.of(row));

        // An invoice line goes from 2 to 5 units: 3 more leave the warehouse
        warehouseProductService.applyChange(warehouse, Map.of(milk, 2), Map.of(milk, 5), IWarehouseProductService.OUTGOING);

        assertThat(row.getAvailableQuantity()).isEqualTo(17);
    }

    @Test
    void unchangedQuantitiesDoNotTouchTheDatabase() {
        warehouseProductService.applyChange(warehouse, Map.of(milk, 4), Map.of(milk, 4), IWarehouseProductService.OUTGOING);

        verify(warehouseProductRepository, never()).findForUpdate(any(), any());
    }

    @Test
    void cancellingAnInvoicePutsGoodsBack() {
        WarehouseProduct row = stock(7);
        when(warehouseProductRepository.findForUpdate("wh-1", "prd-milk")).thenReturn(Optional.of(row));

        warehouseProductService.applyChange(warehouse, Map.of(milk, 3), Map.of(), IWarehouseProductService.OUTGOING);

        assertThat(row.getAvailableQuantity()).isEqualTo(10);
    }

    private WarehouseProduct stock(int quantity) {
        return WarehouseProduct.builder().warehouse(warehouse).product(milk).availableQuantity(quantity).build();
    }

}
