package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.AddressResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.SupplierResponse;
import com.anonymous.dto.response.WarehouseResponse;
import com.anonymous.service.ISupplierService;
import com.anonymous.service.IWarehouseService;
import com.anonymous.utils.PageUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

public final class PartnerTools {

    private PartnerTools() {
    }

    static String addressOf(AddressResponse address) {
        if (address == null) return "—";
        StringBuilder sb = new StringBuilder();
        if (address.getStreet() != null && !address.getStreet().isBlank()) sb.append(address.getStreet()).append(", ");
        if (address.getWard() != null && address.getWard().getName() != null) sb.append(address.getWard().getName()).append(", ");
        if (address.getDistrict() != null && address.getDistrict().getName() != null) sb.append(address.getDistrict().getName());
        String res = sb.toString().trim();
        if (res.endsWith(",")) res = res.substring(0, res.length() - 1);
        return res.isBlank() ? "—" : res;
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class SupplierLookupTool implements ChatTool {

        ISupplierService supplierService;

        @Override
        public String name() {
            return "supplier_lookup";
        }

        @Override
        public String description() {
            return "Tra cứu thông tin nhà cung cấp theo tên, số điện thoại hoặc email (địa chỉ, liên hệ)";
        }

        @Override
        public String permission() {
            return Permissions.STOCK_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Tìm nhà cung cấp Vinamilk", "Thông tin nhà cung cấp Unilever", "Danh sách nhà cung cấp");
        }

        @Override
        @PreAuthorize(Permissions.STOCK_READ)
        public ToolAnswer execute(ToolArgs args) {
            String keyword = args.keyword() == null ? "" : args.keyword().trim();
            int limit = args.limit() == null ? 50 : Math.clamp(args.limit(), 5, 100);
            PageResponse<SupplierResponse> page = supplierService.search(keyword, PageUtil.newestFirst(0, limit));
            List<SupplierResponse> suppliers = page.getItems();
            if (suppliers.isEmpty()) {
                String msg = keyword.isBlank()
                        ? "Không tìm thấy nhà cung cấp nào trong hệ thống."
                        : "Không tìm thấy nhà cung cấp nào khớp với từ khóa **" + keyword + "**. Vui lòng kiểm tra lại.";
                return ToolAnswer.of(msg, "/home/supplier", "Đến trang Quản lý Nhà cung cấp");
            }

            StringBuilder md = new StringBuilder();
            if (!keyword.isBlank()) {
                md.append("Tìm thấy **").append(page.getTotalItems()).append("** nhà cung cấp khớp với từ khóa **").append(keyword).append("**:\n\n");
            } else {
                md.append("Danh sách nhà cung cấp (tổng cộng **").append(page.getTotalItems()).append("** nhà cung cấp):\n\n");
            }

            List<String> headers = List.of("Tên nhà cung cấp", "SĐT", "Email", "Địa chỉ");
            List<List<String>> allRows = new ArrayList<>();
            for (SupplierResponse s : suppliers) {
                allRows.add(List.of(
                        s.getName(),
                        s.getPhoneNumber() != null ? s.getPhoneNumber() : "—",
                        s.getEmail() != null ? s.getEmail() : "—",
                        addressOf(s.getAddress())
                ));
            }

            if (page.getTotalItems() <= 5) {
                md.append(table(headers, allRows));
            } else {
                List<List<String>> topRows = allRows.subList(0, Math.min(5, allRows.size()));
                md.append(table(headers, topRows));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + page.getTotalItems() + " nhà cung cấp (mở rộng)", table(headers, allRows)));
                md.append(downloadCsvLink("Tải danh sách nhà cung cấp (.CSV)", "danh-sach-nha-cung-cap.csv", headers, allRows));
            }
            return ToolAnswer.of(md.toString(), "/home/supplier", "Đến trang Quản lý Nhà cung cấp");
        }
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class WarehouseLookupTool implements ChatTool {

        IWarehouseService warehouseService;

        @Override
        public String name() {
            return "warehouse_lookup";
        }

        @Override
        public String description() {
            return "Danh sách và thông tin các kho hàng của hệ thống siêu thị";
        }

        @Override
        public String permission() {
            return Permissions.STOCK_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Có những kho hàng nào?", "Danh sách các kho", "Địa chỉ kho A");
        }

        @Override
        @PreAuthorize(Permissions.STOCK_READ)
        public ToolAnswer execute(ToolArgs args) {
            String keyword = args.keyword() == null ? "" : args.keyword().trim();
            PageResponse<WarehouseResponse> page = warehouseService.search(keyword, PageUtil.newestFirst(0, 10));
            List<WarehouseResponse> warehouses = page.getItems();
            if (warehouses.isEmpty()) {
                warehouses = warehouseService.getAll();
            }
            if (warehouses.isEmpty()) {
                return ToolAnswer.of("Không tìm thấy thông tin kho hàng nào.", "/home/warehouse", "Mở Kho hàng");
            }

            StringBuilder md = new StringBuilder("Danh sách kho hàng trong hệ thống:\n\n");
            List<List<String>> rows = new ArrayList<>();
            for (WarehouseResponse w : warehouses) {
                rows.add(List.of(
                        w.getName(),
                        w.getPhoneNumber() != null ? w.getPhoneNumber() : "—",
                        w.getEmail() != null ? w.getEmail() : "—",
                        addressOf(w.getAddress())
                ));
            }
            md.append(table(List.of("Tên kho", "SĐT", "Email", "Địa chỉ"), rows));
            return ToolAnswer.of(md.toString(), "/home/warehouse", "Mở Kho hàng");
        }
    }

}
