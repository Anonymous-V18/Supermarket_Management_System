package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.ExpiringLotResponse;
import com.anonymous.dto.response.InventoryRowResponse;
import com.anonymous.service.IReportService;
import com.anonymous.utils.VietnameseTextUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

// Stock reports (warehouse staff, management, accounting), read through IReportService
public final class StockTools {

    private static final String REPORT_LINK = "/home/report";
    private static final String REPORT_LABEL = "Đến trang Báo cáo tồn kho";
    private static final int MAX_ROWS = 10;

    private StockTools() {
    }

    // Accent- and case-insensitive match on name, code or barcode
    static boolean matches(String keyword, String... fields) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String wanted = VietnameseTextUtil.stripDiacritics(VietnameseTextUtil.normalize(keyword));
        for (String field : fields) {
            if (field != null && VietnameseTextUtil.stripDiacritics(VietnameseTextUtil.normalize(field)).contains(wanted)) {
                return true;
            }
        }
        return false;
    }

    static String moreRows(int total) {
        return total > MAX_ROWS ? "\n_Đang hiện " + MAX_ROWS + "/" + number(total) + " dòng, xem đủ trên màn Báo cáo._" : "";
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class InventoryTool implements ChatTool {

        IReportService reportService;

        @Override
        public String name() {
            return "inventory";
        }

        @Override
        public String description() {
            return "Tồn kho theo kho và sản phẩm, giá trị tồn, các mặt hàng dưới mức tồn tối thiểu";
        }

        @Override
        public String permission() {
            return Permissions.STOCK_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Những mặt hàng nào sắp hết hàng?", "Tồn kho sữa chua ở các kho", "Giá trị tồn kho hiện tại");
        }

        @Override
        @PreAuthorize(Permissions.STOCK_READ)
        public ToolAnswer execute(ToolArgs args) {
            boolean lowOnly = Boolean.TRUE.equals(args.lowStockOnly());
            List<InventoryRowResponse> rows = reportService.inventory(args.warehouseId(), lowOnly).stream()
                    .filter(row -> matches(args.keyword(), row.getProductName(), row.getProductCode(), row.getBarcode()))
                    .toList();
            String scope = args.warehouseName() != null ? "kho " + args.warehouseName() : "tất cả kho";
            String title = (lowOnly ? "**Mặt hàng dưới mức tồn tối thiểu**" : "**Tồn kho hiện tại**")
                    + (args.keyword() != null ? " khớp \"" + args.keyword() + "\"" : "") + " (" + scope + ")\n\n";
            if (rows.isEmpty()) {
                return ToolAnswer.of(title + (lowOnly ? "Không có mặt hàng nào dưới mức tồn tối thiểu."
                        : "Không tìm thấy dòng tồn kho nào."), REPORT_LINK, REPORT_LABEL);
            }

            if (Boolean.TRUE.equals(args.byWarehouse()) && args.warehouseId() == null) {
                Map<String, List<InventoryRowResponse>> byWh = rows.stream()
                        .collect(Collectors.groupingBy(r -> r.getWarehouseName() != null ? r.getWarehouseName() : "Khác", LinkedHashMap::new, Collectors.toList()));

                record WhSummary(String name, int productCount, int totalQty, BigDecimal totalValue) {}

                List<WhSummary> summaries = byWh.entrySet().stream()
                        .map(entry -> {
                            String name = entry.getKey();
                            List<InventoryRowResponse> whRows = entry.getValue();
                            int count = whRows.size();
                            int qty = whRows.stream().mapToInt(InventoryRowResponse::getQuantity).sum();
                            BigDecimal val = whRows.stream()
                                    .map(InventoryRowResponse::getStockValue)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            return new WhSummary(name, count, qty, val);
                        })
                        .sorted(Comparator.comparingInt(WhSummary::productCount).thenComparingInt(WhSummary::totalQty).reversed())
                        .toList();

                if (!summaries.isEmpty()) {
                    WhSummary top = summaries.getFirst();
                    StringBuilder md = new StringBuilder();
                    md.append("Kho đang có nhiều sản phẩm nhất là **").append(top.name())
                            .append("** với **").append(top.productCount()).append(" mặt hàng**")
                            .append(" (tổng số lượng tồn: **").append(number(top.totalQty())).append("** sản phẩm")
                            .append(", tổng giá trị: **").append(money(top.totalValue())).append("**).\n\n");

                    md.append("**Thống kê hàng tồn kho theo từng kho:**\n\n");
                    List<String> whHeaders = List.of("Kho", "Số mặt hàng", "Tổng số lượng tồn", "Tổng giá trị tồn");
                    List<List<String>> whTableRows = summaries.stream()
                            .map(s -> List.of(s.name(), number(s.productCount()), number(s.totalQty()), money(s.totalValue())))
                            .toList();
                    md.append(table(whHeaders, whTableRows));

                    List<String> prodHeaders = List.of("Kho", "Sản phẩm", "Mã", "Tồn", "Tối thiểu", "Giá trị tồn");
                    List<List<String>> prodTableRows = rows.stream()
                            .map(row -> List.of(row.getWarehouseName(), row.getProductName() + (row.isLowStock() ? " (Sắp hết)" : ""),
                                    row.getProductCode(), number(row.getQuantity()) + " " + cell(row.getUnitName()),
                                    number(row.getMinStock()), money(row.getStockValue())))
                            .toList();
                    List<List<String>> previewRows = prodTableRows.size() > 50 ? prodTableRows.subList(0, 50) : prodTableRows;
                    String collapseTitle = prodTableRows.size() > 50
                            ? "Nhấn để xem chi tiết mặt hàng (hiển thị 50/" + prodTableRows.size() + " mặt hàng, tải CSV để xem đầy đủ)"
                            : "Nhấn để xem chi tiết từng mặt hàng (" + rows.size() + " mặt hàng)";
                    md.append("\n").append(collapsible(collapseTitle, table(prodHeaders, previewRows)));
                    md.append(downloadCsvLink("Tải báo cáo tồn kho (.CSV)", "bao-cao-ton-kho.csv", prodHeaders, prodTableRows));

                    return ToolAnswer.of(md.toString(), REPORT_LINK, REPORT_LABEL);
                }
            }

            List<String> headers = List.of("Kho", "Sản phẩm", "Mã", "Tồn", "Tối thiểu", "Giá trị tồn");
            List<List<String>> allRows = rows.stream()
                    .map(row -> List.of(row.getWarehouseName(), row.getProductName() + (row.isLowStock() ? " (Sắp hết)" : ""),
                            row.getProductCode(), number(row.getQuantity()) + " " + cell(row.getUnitName()),
                            number(row.getMinStock()), money(row.getStockValue())))
                    .toList();

            StringBuilder md = new StringBuilder(title);
            if (rows.size() <= MAX_ROWS) {
                md.append(table(headers, allRows));
            } else {
                List<List<String>> topRows = allRows.subList(0, MAX_ROWS);
                md.append(table(headers, topRows));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ " + rows.size() + " mặt hàng tồn kho (mở rộng)", table(headers, allRows)));
            }

            md.append(downloadCsvLink("Tải báo cáo tồn kho (.CSV)", "bao-cao-ton-kho.csv", headers, allRows));
            return ToolAnswer.of(md.toString(), REPORT_LINK, REPORT_LABEL);
        }

    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class ExpiringLotsTool implements ChatTool {

        IReportService reportService;

        @Override
        public String name() {
            return "expiring_lots";
        }

        @Override
        public String description() {
            return "Lô hàng sắp hết hạn hoặc đã hết hạn còn tồn trong kho (7, 30, 90 ngày) (xuất báo cáo)";
        }

        @Override
        public String permission() {
            return Permissions.STOCK_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Hàng nào sắp hết hạn trong 7 ngày tới?", "Lô hàng hết hạn trong 30 ngày");
        }

        @Override
        @PreAuthorize(Permissions.STOCK_READ)
        public ToolAnswer execute(ToolArgs args) {
            int withinDays = args.withinDays() == null ? 30 : Math.clamp(args.withinDays(), 1, 366);
            List<ExpiringLotResponse> rows = reportService.expiringLots(args.warehouseId(), withinDays).stream()
                    .filter(row -> matches(args.keyword(), row.getProductName(), row.getProductCode()))
                    .toList();
            String scope = args.warehouseName() != null ? "kho " + args.warehouseName() : "tất cả kho";
            String title = "**Lô hàng hết hạn trong " + withinDays + " ngày tới** (" + scope + ")\n\n";
            if (rows.isEmpty()) {
                return ToolAnswer.of(title + "Không có lô hàng nào sắp hết hạn trong khoảng này.", REPORT_LINK, REPORT_LABEL);
            }

            List<String> headers = List.of("Kho", "Sản phẩm", "Lô", "Hạn dùng", "Còn", "SL tồn", "Phiếu nhập");
            List<List<String>> allRows = rows.stream()
                    .map(row -> List.of(row.getWarehouseName(), row.getProductName(), cell(row.getLotNumber()),
                            date(row.getExpiryDate()), row.isExpired() ? "Đã hết hạn" : row.getDaysLeft() + " ngày",
                            number(row.getQuantity()), cell(row.getStockInCode())))
                    .toList();

            StringBuilder md = new StringBuilder(title);
            if (rows.size() <= MAX_ROWS) {
                md.append(table(headers, allRows));
            } else {
                List<List<String>> topRows = allRows.subList(0, MAX_ROWS);
                md.append(table(headers, topRows));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ " + rows.size() + " lô hàng sắp hết hạn (mở rộng)", table(headers, allRows)));
            }

            md.append(downloadCsvLink("Tải danh sách hàng sắp hết hạn (.CSV)", "hang-sap-het-han.csv", headers, allRows));
            md.append("\n_Số lượng theo lô được ước tính theo nguyên tắc nhập trước xuất trước (giống màn Báo cáo)._");
            return ToolAnswer.of(md.toString(), REPORT_LINK, REPORT_LABEL);
        }

    }

}
