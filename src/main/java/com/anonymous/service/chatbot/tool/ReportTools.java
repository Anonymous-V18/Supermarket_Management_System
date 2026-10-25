package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.EmployeeSalesResponse;
import com.anonymous.dto.response.ProductSalesResponse;
import com.anonymous.dto.response.RevenueByDayResponse;
import com.anonymous.dto.response.WarehouseRevenueResponse;
import com.anonymous.service.IReportService;
import com.anonymous.utils.ShiftUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

// Sales reports (management and accounting), read through IReportService: same figures as the Report screen
public final class ReportTools {

    private static final String REPORT_LINK = "/home/report";
    private static final String REPORT_LABEL = "Đến trang Báo cáo chi tiết";

    private ReportTools() {
    }

    // Same default as the Report screen: the last 30 days up to today (Vietnam time)
    static LocalDate[] rangeOf(ToolArgs args) {
        LocalDate today = LocalDate.now(ShiftUtil.VIETNAM_ZONE);
        LocalDate to = args.to() != null ? args.to() : (args.from() != null ? args.from() : today);
        LocalDate from = args.from() != null ? args.from() : to.minusDays(29);
        return new LocalDate[]{from, to};
    }

    static String scopeOf(ToolArgs args) {
        return args.warehouseName() != null ? "kho " + args.warehouseName() : "tất cả kho";
    }

    private static BigDecimal sum(List<RevenueByDayResponse> days, Function<RevenueByDayResponse, BigDecimal> field) {
        return days.stream().map(field).map(value -> value == null ? BigDecimal.ZERO : value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class RevenueTool implements ChatTool {

        IReportService reportService;

        @Override
        public String name() {
            return "revenue";
        }

        @Override
        public String description() {
            return "Doanh thu, hoàn trả, doanh thu thuần, giá vốn, lợi nhuận gộp theo khoảng ngày và kho (vẽ biểu đồ, xuất báo cáo)";
        }

        @Override
        public String permission() {
            return Permissions.REPORT;
        }

        @Override
        public List<String> examples() {
            return List.of("Doanh thu hôm nay là bao nhiêu?", "Doanh thu tháng này", "Vẽ biểu đồ doanh thu tuần này");
        }

        @Override
        @PreAuthorize(Permissions.REPORT)
        public ToolAnswer execute(ToolArgs args) {
            LocalDate[] range = rangeOf(args);

            if (Boolean.TRUE.equals(args.byWarehouse())) {
                List<WarehouseRevenueResponse> warehouses = reportService.revenueByWarehouse(range[0], range[1]);
                StringBuilder mdWh = new StringBuilder();
                mdWh.append("**Doanh thu ").append(range(range[0], range[1])).append(" theo từng kho**\n\n");
                if (warehouses.isEmpty()) {
                    mdWh.append("Không có phát sinh doanh thu nào tại các kho trong khoảng thời gian này.");
                    return ToolAnswer.of(mdWh.toString(), REPORT_LINK, REPORT_LABEL);
                }

                List<String> headers = List.of("Kho", "Số HĐ", "Doanh thu", "Hoàn trả", "Doanh thu thuần", "Lợi nhuận gộp");
                List<List<String>> rows = warehouses.stream().map(w -> List.of(
                        w.getWarehouseName(),
                        number(w.getInvoiceCount()),
                        money(w.getRevenue()),
                        money(w.getRefunds()),
                        money(w.getNetRevenue()),
                        money(w.getGrossProfit())
                )).toList();
                mdWh.append(table(headers, rows));

                BigDecimal maxRevenue = warehouses.stream()
                        .map(w -> w.getRevenue() != null ? w.getRevenue() : BigDecimal.ZERO)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ONE);

                if (maxRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    List<ChartBar> bars = warehouses.stream().map(w -> {
                        BigDecimal val = w.getNetRevenue() != null && w.getNetRevenue().compareTo(BigDecimal.ZERO) > 0
                                ? w.getNetRevenue() : (w.getRevenue() != null ? w.getRevenue() : BigDecimal.ZERO);
                        int pct = val.multiply(BigDecimal.valueOf(100)).divide(maxRevenue, RoundingMode.HALF_UP).intValue();
                        String detail = money(w.getNetRevenue()) + " (" + w.getInvoiceCount() + " HĐ)";
                        if (w.getRefunds() != null && w.getRefunds().compareTo(BigDecimal.ZERO) > 0) {
                            detail += " [Hoàn trả: " + money(w.getRefunds()) + "]";
                        }
                        return new ChartBar(w.getWarehouseName(), val.longValue(), detail, Math.max(pct, 5));
                    }).toList();
                    mdWh.append("\n").append(chart("Biểu đồ doanh thu theo kho " + range(range[0], range[1]), bars)).append("\n");
                }

                mdWh.append(downloadCsvLink("Tải báo cáo doanh thu theo kho (.CSV)", "doanh-thu-theo-kho.csv", headers, rows));
                mdWh.append("\n_Số liệu tính trên các hóa đơn và phiếu trả hàng đã hoàn thành tại từng kho._");
                return ToolAnswer.of(mdWh.toString(), REPORT_LINK, REPORT_LABEL);
            }

            List<RevenueByDayResponse> days = reportService.revenueByDay(range[0], range[1], args.warehouseId());
            long invoices = days.stream().mapToLong(RevenueByDayResponse::getInvoiceCount).sum();
            StringBuilder md = new StringBuilder();
            md.append("**Doanh thu ").append(range(range[0], range[1])).append("** (").append(scopeOf(args)).append(")\n\n");
            md.append(table(List.of("Chỉ tiêu", "Giá trị"), List.of(
                    List.of("Số hóa đơn hoàn thành", number(invoices)),
                    List.of("Doanh thu", money(sum(days, RevenueByDayResponse::getRevenue))),
                    List.of("Hoàn trả", money(sum(days, RevenueByDayResponse::getRefunds))),
                    List.of("Doanh thu thuần", money(sum(days, RevenueByDayResponse::getNetRevenue))),
                    List.of("Giá vốn", money(sum(days, RevenueByDayResponse::getCost))),
                    List.of("Lợi nhuận gộp", money(sum(days, RevenueByDayResponse::getGrossProfit))))));

            List<RevenueByDayResponse> activeDays = days.stream()
                    .filter(day -> day.getInvoiceCount() > 0 || (day.getRefunds() != null && day.getRefunds().signum() != 0))
                    .toList();

            if (!activeDays.isEmpty() && (Boolean.TRUE.equals(args.chart()) || activeDays.size() > 1)) {
                BigDecimal maxBasis = activeDays.stream()
                        .map(d -> {
                            BigDecimal net = d.getNetRevenue() != null ? d.getNetRevenue() : BigDecimal.ZERO;
                            BigDecimal rev = d.getRevenue() != null ? d.getRevenue() : BigDecimal.ZERO;
                            return net.compareTo(BigDecimal.ZERO) > 0 ? net : rev;
                        })
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ONE);
                if (maxBasis.compareTo(BigDecimal.ZERO) > 0) {
                    List<ChartBar> bars = activeDays.stream().limit(10).map(d -> {
                        BigDecimal net = d.getNetRevenue() != null ? d.getNetRevenue() : BigDecimal.ZERO;
                        BigDecimal displayVal = net.compareTo(BigDecimal.ZERO) > 0 ? net : (d.getRevenue() != null ? d.getRevenue() : BigDecimal.ZERO);
                        int pct = displayVal.multiply(BigDecimal.valueOf(100)).divide(maxBasis, RoundingMode.HALF_UP).intValue();
                        String detail = money(net) + " (" + d.getInvoiceCount() + " HĐ)";
                        if (d.getRefunds() != null && d.getRefunds().compareTo(BigDecimal.ZERO) > 0) {
                            detail += " [Hoàn trả: " + money(d.getRefunds()) + "]";
                        }
                        return new ChartBar(date(d.getDate()), displayVal.longValue(), detail, Math.max(pct, 5));
                    }).toList();
                    md.append("\n").append(chart("Biểu đồ doanh thu thuần " + range(range[0], range[1]), bars)).append("\n");
                }
            }

            if (!range[0].equals(range[1]) && !activeDays.isEmpty() && activeDays.size() <= 31) {
                md.append("\n**Theo ngày** (chỉ ngày có phát sinh)\n\n");
                md.append(table(List.of("Ngày", "Hóa đơn", "Doanh thu thuần", "Lợi nhuận gộp"), activeDays.stream()
                        .map(day -> List.of(date(day.getDate()), number(day.getInvoiceCount()),
                                money(day.getNetRevenue()), money(day.getGrossProfit())))
                        .toList()));
            }

            List<String> csvHeaders = List.of("Ngày", "Số hóa đơn", "Doanh thu", "Hoàn trả", "Doanh thu thuần", "Giá vốn", "Lợi nhuận gộp");
            List<List<String>> csvRows = days.stream().map(d -> List.of(
                    date(d.getDate()), number(d.getInvoiceCount()), money(d.getRevenue()), money(d.getRefunds()),
                    money(d.getNetRevenue()), money(d.getCost()), money(d.getGrossProfit())
            )).toList();
            md.append(downloadCsvLink("Tải báo cáo doanh thu (.CSV)", "bao-cao-doanh-thu.csv", csvHeaders, csvRows));

            md.append("\n_Tính trên hóa đơn đã hoàn thành, trừ phiếu trả hàng đã hoàn thành (giống màn Báo cáo)._");
            return ToolAnswer.of(md.toString(), REPORT_LINK, REPORT_LABEL);
        }

    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class TopProductsTool implements ChatTool {

        IReportService reportService;

        @Override
        public String name() {
            return "top_products";
        }

        @Override
        public String description() {
            return "Sản phẩm bán ra, số lượng bán, doanh thu và lợi nhuận gộp theo ngày/khoảng ngày và kho (vẽ biểu đồ, báo cáo chi tiết)";
        }

        @Override
        public String permission() {
            return Permissions.REPORT;
        }

        @Override
        public List<String> examples() {
            return List.of("Top 5 sản phẩm bán chạy tháng này", "Thống kê lượng mặt hàng bán ra hôm nay", "Vẽ biểu đồ lượng mặt hàng bán ra");
        }

        @Override
        @PreAuthorize(Permissions.REPORT)
        public ToolAnswer execute(ToolArgs args) {
            LocalDate[] range = rangeOf(args);
            int limit = args.limit() == null ? 50 : Math.clamp(args.limit(), 5, 100);
            List<ProductSalesResponse> rows = reportService.topProducts(range[0], range[1], args.warehouseId(), limit);
            StringBuilder md = new StringBuilder();

            if (rows.isEmpty()) {
                md.append("**Thống kê lượng mặt hàng bán ra ").append(range(range[0], range[1])).append("** (").append(scopeOf(args)).append(")\n\n");
                md.append("Không có mặt hàng nào được bán ra trong khoảng thời gian này (0 hóa đơn hoàn thành).");
                if (Boolean.TRUE.equals(args.chart())) {
                    md.append("\n\n_Chưa có số liệu phát sinh để vẽ biểu đồ._");
                }
                return ToolAnswer.of(md.toString(), REPORT_LINK, REPORT_LABEL);
            }

            long totalQty = rows.stream().mapToLong(ProductSalesResponse::getQuantity).sum();
            BigDecimal totalRevenue = rows.stream().map(ProductSalesResponse::getRevenue).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalProfit = rows.stream().map(ProductSalesResponse::getGrossProfit).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

            md.append("**Thống kê lượng mặt hàng bán ra ").append(range(range[0], range[1])).append("** (").append(scopeOf(args)).append(")\n\n");
            md.append("Tổng cộng: **").append(number(totalQty)).append("** sản phẩm bán ra thuộc **").append(rows.size())
                    .append("** mặt hàng, tổng doanh thu: **").append(money(totalRevenue))
                    .append("** (lợi nhuận gộp: **").append(money(totalProfit)).append("**).\n\n");

            long maxQty = rows.stream().mapToLong(ProductSalesResponse::getQuantity).max().orElse(1);
            List<ChartBar> bars = rows.stream().limit(10).map(r -> {
                int pct = (int) Math.round(((double) r.getQuantity() / maxQty) * 100.0);
                return new ChartBar(r.getProductName(), r.getQuantity(), number(r.getQuantity()) + " cái (" + money(r.getRevenue()) + ")", Math.max(pct, 5));
            }).toList();
            md.append(chart("Biểu đồ lượng bán theo sản phẩm " + range(range[0], range[1]), bars)).append("\n\n");

            List<String> headers = List.of("#", "Sản phẩm", "Mã SP", "SL bán", "Doanh thu", "Lợi nhuận gộp");
            int[] rank = {0};
            List<List<String>> allRows = new ArrayList<>(rows.stream()
                    .map(row -> List.of(String.valueOf(++rank[0]), row.getProductName(), row.getProductCode(),
                            number(row.getQuantity()), money(row.getRevenue()), money(row.getGrossProfit())))
                    .toList());

            if (rows.size() <= 5) {
                md.append(table(headers, allRows));
            } else {
                List<List<String>> topRows = allRows.subList(0, 5);
                md.append(table(headers, topRows));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + rows.size() + " mặt hàng đã bán (mở rộng)", table(headers, allRows)));
            }

            md.append(downloadCsvLink("Tải báo cáo chi tiết mặt hàng bán ra (.CSV)", "bao-cao-mat-hang-ban-ra.csv", headers, allRows));
            return ToolAnswer.of(md.toString(), REPORT_LINK, REPORT_LABEL);
        }

    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class SalesByEmployeeTool implements ChatTool {

        IReportService reportService;

        @Override
        public String name() {
            return "sales_by_employee";
        }

        @Override
        public String description() {
            return "Doanh số theo nhân viên bán hàng (số hóa đơn, doanh thu) theo khoảng ngày và kho (vẽ biểu đồ, xuất báo cáo)";
        }

        @Override
        public String permission() {
            return Permissions.REPORT;
        }

        @Override
        public List<String> examples() {
            return List.of("Doanh số theo nhân viên tháng này", "Vẽ biểu đồ nhân viên nào bán được nhiều nhất");
        }

        @Override
        @PreAuthorize(Permissions.REPORT)
        public ToolAnswer execute(ToolArgs args) {
            LocalDate[] range = rangeOf(args);
            List<EmployeeSalesResponse> rows = reportService.salesByEmployee(range[0], range[1], args.warehouseId());
            StringBuilder md = new StringBuilder();
            md.append("**Doanh số theo nhân viên ").append(range(range[0], range[1])).append("** (").append(scopeOf(args)).append(")\n\n");

            if (rows.isEmpty()) {
                md.append("Không có hóa đơn hoàn thành nào trong khoảng này.");
                if (Boolean.TRUE.equals(args.chart())) {
                    md.append("\n\n_Chưa có số liệu phát sinh để vẽ biểu đồ._");
                }
                return ToolAnswer.of(md.toString(), REPORT_LINK, REPORT_LABEL);
            }

            if (Boolean.TRUE.equals(args.chart()) || rows.size() > 1) {
                BigDecimal maxRev = rows.stream().map(EmployeeSalesResponse::getRevenue).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
                if (maxRev.compareTo(BigDecimal.ZERO) > 0) {
                    List<ChartBar> bars = rows.stream().limit(10).map(e -> {
                        BigDecimal rev = e.getRevenue() != null ? e.getRevenue() : BigDecimal.ZERO;
                        int pct = rev.multiply(BigDecimal.valueOf(100)).divide(maxRev, RoundingMode.HALF_UP).intValue();
                        return new ChartBar(e.getEmployeeName(), rev.longValue(), money(rev) + " (" + e.getInvoiceCount() + " HĐ)", Math.max(pct, 5));
                    }).toList();
                    md.append(chart("Biểu đồ doanh số nhân viên " + range(range[0], range[1]), bars)).append("\n\n");
                }
            }

            List<String> headers = List.of("Nhân viên", "Mã", "Hóa đơn", "Doanh thu");
            List<List<String>> allRows = rows.stream()
                    .map(row -> List.of(row.getEmployeeName(), row.getEmployeeCode(), number(row.getInvoiceCount()),
                            money(row.getRevenue())))
                    .toList();

            if (rows.size() <= 5) {
                md.append(table(headers, allRows));
            } else {
                List<List<String>> topRows = allRows.subList(0, 5);
                md.append(table(headers, topRows));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + rows.size() + " nhân viên (mở rộng)", table(headers, allRows)));
            }

            md.append(downloadCsvLink("Tải báo cáo doanh số nhân viên (.CSV)", "doanh-so-nhan-vien.csv", headers, allRows));
            return ToolAnswer.of(md.toString(), REPORT_LINK, REPORT_LABEL);
        }

    }

}
