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
        return "Tổng số lượng sản phẩm, danh sách mặt hàng đang kinh doanh, hoặc tra giá bán, mã vạch và tồn kho của sản phẩm cụ thể";
    }

    @Override
    public String permission() {
        return Permissions.AUTHENTICATED;
    }

    @Override
    public List<String> examples() {
        return List.of("Hiện tại có bao nhiêu sản phẩm?", "Giá sữa chua Vinamilk bao nhiêu?", "Sản phẩm SP00001 còn bao nhiêu hàng?");
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ToolAnswer execute(ToolArgs args) {
        if (args.keyword() == null || args.keyword().isBlank()) {
            List<ProductResponse> allProducts = productService.getAll();
            if (allProducts.isEmpty()) {
                return ToolAnswer.of("Hiện tại hệ thống chưa có sản phẩm nào được khai báo.", "/home/product", "Mở Sản phẩm");
            }
            long categoryCount = allProducts.stream()
                    .map(p -> p.getProductCategory() != null ? p.getProductCategory().getName() : null)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .count();
            long brandCount = allProducts.stream()
                    .map(p -> p.getBrand() != null ? p.getBrand().getName() : null)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .count();

            StringBuilder md = new StringBuilder();
            md.append("**Thống kê danh mục sản phẩm hiện có**\n\n");
            md.append(kpiCards(List.of(
                    new KpiCard("Tổng số sản phẩm", number(allProducts.size()) + " SP", "Đang kinh doanh trên hệ thống", "primary"),
                    new KpiCard("Nhóm hàng", number(categoryCount) + " nhóm", "Phân loại danh mục hàng hóa", "success"),
                    new KpiCard("Thương hiệu", number(brandCount) + " hãng", "Nhà sản xuất / nhãn hàng", "primary")
            ))).append("\n");

            md.append("Hiện tại hệ thống đang kinh doanh tổng cộng **").append(number(allProducts.size()))
                    .append(" sản phẩm (mặt hàng)** thuộc **").append(number(categoryCount)).append(" nhóm hàng**.\n\n");

            List<String> headers = List.of("#", "Sản phẩm", "Mã SP", "Mã vạch", "Nhóm hàng", "Đơn vị", "Giá bán");
            int[] idx = {0};
            List<List<String>> allRows = allProducts.stream()
                    .map(p -> List.of(
                            String.valueOf(++idx[0]),
                            cell(p.getName()),
                            cell(p.getCode()),
                            cell(p.getBarcode()),
                            p.getProductCategory() != null ? cell(p.getProductCategory().getName()) : "—",
                            p.getUnit() != null ? cell(p.getUnit().getName()) : "—",
                            money(p.getSalePrice())
                    ))
                    .toList();

            int previewSize = 10;
            if (allRows.size() <= previewSize) {
                md.append(table(headers, allRows));
            } else {
                md.append(table(headers, allRows.subList(0, previewSize)));
                md.append("\n").append(collapsible(
                        "Nhấn để xem toàn bộ danh sách " + allRows.size() + " sản phẩm (mở rộng)",
                        table(headers, allRows)
                ));
            }
            md.append(downloadCsvLink("Tải danh sách toàn bộ sản phẩm (.CSV)", "danh-sach-san-pham.csv", headers, allRows));
            return ToolAnswer.of(md.toString(), "/home/product", "Đến trang Quản lý Sản phẩm");
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
