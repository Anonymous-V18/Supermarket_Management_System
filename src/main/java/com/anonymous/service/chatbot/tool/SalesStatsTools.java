package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.ExpiringLotResponse;
import com.anonymous.dto.response.InvoiceResponse;
import com.anonymous.dto.response.InvoiceStatusCountResponse;
import com.anonymous.dto.response.RevenueByDayResponse;
import com.anonymous.service.IInvoiceService;
import com.anonymous.service.IReportService;
import com.anonymous.utils.ShiftUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

// Counts over a period: invoices by status, and an overview that shows only the figures the asker's roles may see
public final class SalesStatsTools {

    private static final List<String> STATUS_ORDER = List.of("COMPLETED", "PENDING", "CANCELLED");
    private static final String STATUS_CANCELLED = "CANCELLED";

    private SalesStatsTools() {
    }

    // A count is about one day unless a period was asked: no period means today (Vietnam time)
    static LocalDate[] dayRangeOf(ToolArgs args) {
        LocalDate today = LocalDate.now(ShiftUtil.VIETNAM_ZONE);
        LocalDate from = args.from() != null ? args.from() : today;
        LocalDate to = args.to() != null ? args.to() : from;
        return new LocalDate[]{from, to};
    }

    static String futureNote(LocalDate[] range) {
        return range[1].isAfter(LocalDate.now(ShiftUtil.VIETNAM_ZONE))
                ? "\n_Khoảng này có ngày chưa tới nên chưa có số liệu cho những ngày đó._" : "";
    }

    static List<InvoiceStatusCountResponse> ordered(List<InvoiceStatusCountResponse> rows) {
        return rows.stream()
                .sorted(Comparator.comparingInt((InvoiceStatusCountResponse row) -> {
                    int at = STATUS_ORDER.indexOf(row.getStatusCode());
                    return at < 0 ? STATUS_ORDER.size() : at;
                }).thenComparing(InvoiceStatusCountResponse::getStatusCode))
                .toList();
    }

    static long countOf(List<InvoiceStatusCountResponse> rows, boolean includeCancelled) {
        return rows.stream()
                .filter(row -> includeCancelled || !STATUS_CANCELLED.equals(row.getStatusCode()))
                .mapToLong(InvoiceStatusCountResponse::getInvoiceCount).sum();
    }

    static String breakdown(List<InvoiceStatusCountResponse> rows) {
        return ordered(rows).stream()
                .map(row -> number(row.getInvoiceCount()) + " " + cell(row.getStatusName()).toLowerCase())
                .collect(Collectors.joining(", "));
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class InvoiceStatsTool implements ChatTool {

        IInvoiceService invoiceService;

        @Override
        public String name() {
            return "invoice_stats";
        }

        @Override
        public String description() {
            return "Số hóa đơn (đơn hàng) bán ra theo ngày / khoảng ngày và kho, chia theo trạng thái";
        }

        @Override
        public String permission() {
            return Permissions.SALES_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Hôm nay có bao nhiêu đơn hàng?", "Số hóa đơn tuần này");
        }

        @Override
        @PreAuthorize(Permissions.SALES_READ)
        public ToolAnswer execute(ToolArgs args) {
            LocalDate[] range = dayRangeOf(args);
            List<InvoiceStatusCountResponse> rows = ordered(invoiceService.countByStatus(range[0], range[1], args.warehouseId()));
            long all = countOf(rows, true);
            StringBuilder md = new StringBuilder();
            md.append("**Hóa đơn bán hàng ").append(range(range[0], range[1])).append("** (")
                    .append(ReportTools.scopeOf(args)).append(")\n\n");
            if (all == 0) {
                md.append("Không có hóa đơn nào được lập trong ").append(range(range[0], range[1])).append(".");
                if (Boolean.TRUE.equals(args.chart())) {
                    md.append("\n\n_Chưa có hóa đơn phát sinh để vẽ biểu đồ._");
                }
            } else {
                md.append("Có **").append(number(all)).append(" hóa đơn** (").append(breakdown(rows)).append(").\n\n");

                if (Boolean.TRUE.equals(args.chart()) || rows.size() > 1) {
                    long maxCount = rows.stream().mapToLong(InvoiceStatusCountResponse::getInvoiceCount).max().orElse(1);
                    List<ChartBar> bars = rows.stream().map(r -> {
                        int pct = (int) Math.round(((double) r.getInvoiceCount() / maxCount) * 100.0);
                        return new ChartBar(r.getStatusName(), r.getInvoiceCount(), number(r.getInvoiceCount()) + " HĐ (" + money(r.getTotal()) + ")", Math.max(pct, 5));
                    }).toList();
                    md.append(chart("Biểu đồ trạng thái hóa đơn " + range(range[0], range[1]), bars)).append("\n\n");
                }

                List<List<String>> table = new ArrayList<>(rows.stream()
                        .map(row -> List.of(cell(row.getStatusName()), number(row.getInvoiceCount()), money(row.getTotal())))
                        .toList());
                table.add(List.of("**Tổng**", "**" + number(all) + "**",
                        "**" + money(rows.stream().map(InvoiceStatusCountResponse::getTotal)
                                .map(value -> value == null ? BigDecimal.ZERO : value)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)) + "**"));
                md.append(table(List.of("Trạng thái", "Số hóa đơn", "Tổng tiền trên hóa đơn"), table));

                List<InvoiceResponse> invoices = invoiceService.findByDateRange(range[0], range[1], args.warehouseId(), 50);
                if (!invoices.isEmpty()) {
                    md.append("\n**Danh sách hóa đơn:**\n\n");
                    List<String> invHeaders = List.of("Mã HĐ", "Khách hàng", "Tổng tiền", "Trạng thái", "Thanh toán", "Thời gian");
                    List<List<String>> invRows = invoices.stream().map(inv -> List.of(
                            inv.getCode(),
                            inv.getCustomer() != null ? inv.getCustomer().getName() : "Khách lẻ",
                            money(inv.getTotalPrice()),
                            status(inv.getStatusInvoice()),
                            payment(inv.getPaymentMethod()),
                            dateTime(inv.getInvoiceCreateDate() != null ? inv.getInvoiceCreateDate() : inv.getCreatedDate())
                    )).toList();

                    if (invoices.size() <= 5) {
                        md.append(table(invHeaders, invRows));
                    } else {
                        List<List<String>> topInv = invRows.subList(0, 5);
                        md.append(table(invHeaders, topInv));
                        md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + invoices.size() + " hóa đơn (mở rộng)", table(invHeaders, invRows)));
                    }
                    md.append(downloadCsvLink("Tải danh sách hóa đơn (.CSV)", "danh-sach-hoa-don.csv", invHeaders, invRows));
                }

                List<String> headers = List.of("Trạng thái", "Số hóa đơn", "Tổng tiền");
                List<List<String>> csvRows = rows.stream()
                        .map(row -> List.of(cell(row.getStatusName()), number(row.getInvoiceCount()), money(row.getTotal())))
                        .toList();
                md.append(downloadCsvLink("Tải thống kê hóa đơn (.CSV)", "thong-ke-hoa-don.csv", headers, csvRows));
            }
            md.append(futureNote(range));
            md.append("\n_\"Đơn hàng\" là hóa đơn bán hàng, tính theo ngày lập hóa đơn; tổng tiền chưa trừ hàng trả lại._");
            return ToolAnswer.of(md.toString(), "/home/invoice", "Đến trang Quản lý Hóa đơn");
        }

    }

    // "Thống kê giúp tôi": one table with every figure the asker may see, each read from the service behind its screen
    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class OverviewTool implements ChatTool {

        private static final int EXPIRY_DAYS = 30;

        IInvoiceService invoiceService;
        IReportService reportService;

        @Override
        public String name() {
            return "data_overview";
        }

        @Override
        public String description() {
            return "Thống kê tổng quan theo ngày / khoảng ngày: hóa đơn, doanh thu, tồn kho thấp, hàng sắp hết hạn (theo quyền)";
        }

        @Override
        public String permission() {
            return Permissions.AUTHENTICATED;
        }

        @Override
        public List<String> examples() {
            return List.of("Thống kê hôm nay", "Tổng quan tuần này");
        }

        @Override
        @PreAuthorize(Permissions.AUTHENTICATED)
        public ToolAnswer execute(ToolArgs args) {
            Set<String> roles = currentRoles();
            LocalDate[] range = dayRangeOf(args);
            List<List<String>> rows = new ArrayList<>();
            String link = null;
            String label = null;
            if (ToolAccess.allows(Permissions.SALES_READ, roles)) {
                List<InvoiceStatusCountResponse> invoices = invoiceService.countByStatus(range[0], range[1], args.warehouseId());
                long all = countOf(invoices, true);
                rows.add(List.of("Hóa đơn đã lập", number(all) + (all > 0 ? " (" + breakdown(invoices) + ")" : "")));
                link = "/home/invoice";
                label = "Mở Hóa đơn";
            }
            if (ToolAccess.allows(Permissions.REPORT, roles)) {
                List<RevenueByDayResponse> days = reportService.revenueByDay(range[0], range[1], args.warehouseId());
                rows.add(List.of("Doanh thu thuần (hóa đơn hoàn thành trừ trả hàng)", money(sum(days, true))));
                rows.add(List.of("Lợi nhuận gộp", money(sum(days, false))));
                link = "/home/report";
                label = "Mở Báo cáo";
            }
            if (ToolAccess.allows(Permissions.STOCK_READ, roles)) {
                int lowStock = reportService.inventory(args.warehouseId(), true).size();
                List<ExpiringLotResponse> lots = reportService.expiringLots(args.warehouseId(), EXPIRY_DAYS);
                long expired = lots.stream().filter(ExpiringLotResponse::isExpired).count();
                rows.add(List.of("Mặt hàng dưới mức tồn tối thiểu (hiện tại)", number(lowStock)));
                rows.add(List.of("Lô hết hạn trong " + EXPIRY_DAYS + " ngày tới (hiện tại)", number(lots.size())
                        + (expired > 0 ? " (gồm " + number(expired) + " lô đã hết hạn)" : "")));
                if (link == null) {
                    link = "/home/report";
                    label = "Mở Báo cáo tồn kho";
                }
            }
            String title = "**Thống kê " + range(range[0], range[1]) + "** (" + ReportTools.scopeOf(args) + ")\n\n";
            if (rows.isEmpty()) {
                return ToolAnswer.of(title + "Vai trò của bạn chưa được xem số liệu tổng hợp nào. Bạn có thể hỏi về sản phẩm, "
                        + "giá bán hoặc số liệu ca làm việc của mình.", null, null);
            }
            String md = title + table(List.of("Chỉ tiêu", "Giá trị"), rows) + futureNote(range)
                    + "\n_Hỏi chi tiết hơn, ví dụ: \"doanh thu tuần này\", \"hàng sắp hết hạn trong 7 ngày\"._";
            return ToolAnswer.of(md, link, label);
        }

        private static BigDecimal sum(List<RevenueByDayResponse> days, boolean netRevenue) {
            return days.stream()
                    .map(day -> netRevenue ? day.getNetRevenue() : day.getGrossProfit())
                    .map(value -> value == null ? BigDecimal.ZERO : value)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private static Set<String> currentRoles() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return Set.of();
            }
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring("ROLE_".length()))
                    .collect(Collectors.toSet());
        }

    }

}
