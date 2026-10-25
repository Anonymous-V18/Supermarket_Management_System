package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.ProductResponse;
import com.anonymous.dto.response.WarehouseToSelectionResponse;
import com.anonymous.exception.AppException;
import com.anonymous.service.IProductService;
import com.anonymous.service.IWarehouseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

// Price and stock per warehouse of a product, by name / code / barcode. Every signed-in user may look
// this up (same rule as the product screens and the sales counter).
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductLookupTool implements ChatTool {

    private static final int MAX_PRODUCTS = 5;

    IProductService productService;
    IWarehouseService warehouseService;

    @Override
    public String name() {
        return "product_lookup";
    }

    @Override
    public String description() {
        return "Giá bán, mã vạch và số lượng còn của một sản phẩm ở từng kho (tìm theo tên, mã hoặc mã vạch)";
    }

    @Override
    public String permission() {
        return Permissions.AUTHENTICATED;
    }

    @Override
    public List<String> examples() {
        return List.of("Giá sữa chua Vinamilk bao nhiêu?", "Sản phẩm SP00001 còn bao nhiêu hàng?");
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ToolAnswer execute(ToolArgs args) {
        if (args.keyword() == null || args.keyword().isBlank()) {
            return ToolAnswer.notMatched("Bạn muốn tra sản phẩm nào? Hãy nhập tên, mã sản phẩm (VD `SP00001`) hoặc mã vạch.",
                    "/home/product", "Mở Sản phẩm");
        }
        List<ProductResponse> products = productService.search(args.keyword().trim(), PageRequest.of(0, MAX_PRODUCTS)).getItems();
        if (products.isEmpty()) {
            return ToolAnswer.notMatched("Không tìm thấy sản phẩm nào khớp \"" + args.keyword() + "\".", "/home/product", "Mở Sản phẩm");
        }
        List<WarehouseToSelectionResponse> warehouses = warehouseService.getAllToSelection().stream()
                .filter(warehouse -> args.warehouseId() == null || args.warehouseId().equals(warehouse.getId()))
                .toList();

        List<String> headers = new ArrayList<>(List.of("Sản phẩm", "Mã", "Mã vạch", "Giá bán"));
        warehouses.forEach(warehouse -> headers.add("Tồn " + warehouse.getName()));
        List<List<String>> rows = new ArrayList<>();
        for (ProductResponse product : products) {
            List<String> row = new ArrayList<>(List.of(product.getName(), product.getCode(), cell(product.getBarcode()),
                    money(product.getSalePrice())));
            String unit = product.getUnit() != null ? " " + product.getUnit().getName() : "";
            for (WarehouseToSelectionResponse warehouse : warehouses) {
                row.add(stockOf(product.getId(), warehouse.getId()) + unit);
            }
            rows.add(row);
        }
        String md = "**Kết quả tra cứu \"" + args.keyword() + "\"**\n\n" + table(headers, rows)
                + (products.size() == MAX_PRODUCTS ? "\n_Chỉ hiện " + MAX_PRODUCTS + " sản phẩm đầu tiên, hãy gõ tên cụ thể hơn._" : "");
        return ToolAnswer.of(md, "/home/product", "Đến trang Quản lý Sản phẩm");
    }

    private String stockOf(String productId, String warehouseId) {
        try {
            Integer quantity = productService.getDetail(productId, warehouseId).getAvailableQuantity();
            return number(quantity == null ? 0 : quantity);
        } catch (AppException e) {
            return "—";
        }
    }

}
