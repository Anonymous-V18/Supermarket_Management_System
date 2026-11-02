package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.*;
import com.anonymous.service.*;
import com.anonymous.utils.PageUtil;
import com.anonymous.utils.ShiftUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

// Looks up one business document by its code (HD000001, TH000001, PN..., PX..., CK..., KK...), with the
// same permission as that document's own screen
public final class DocumentLookupTools {

    private DocumentLookupTools() {
    }

    static String codeOf(ToolArgs args) {
        return args.keyword() == null ? null : args.keyword().trim().toUpperCase(Locale.ROOT);
    }

    // Search by keyword, then keep the exact code only
    static <T> Optional<T> exact(List<T> candidates, Function<T, String> codeOf, String code) {
        return candidates.stream().filter(item -> code.equalsIgnoreCase(codeOf.apply(item))).findFirst();
    }

    static String employeeOf(EmployeeResponse employee) {
        return employee == null ? "—" : employee.getName();
    }

    static String warehouseOf(WarehouseResponse warehouse) {
        return warehouse == null ? "—" : warehouse.getName();
    }

    static String productOf(ProductResponse product) {
        return product == null ? "—" : product.getName() + " (" + product.getCode() + ")";
    }

    static ToolAnswer notFound(String code, String link, String label) {
        return ToolAnswer.of("Không tìm thấy chứng từ **" + code + "**. Kiểm tra lại mã (VD `HD000001`).", link, label);
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class InvoiceLookupTool implements ChatTool {

        IInvoiceService invoiceService;

        @Override
        public String name() {
            return "invoice_lookup";
        }

        @Override
        public String description() {
            return "Chi tiết một hóa đơn bán hàng theo mã HD (trạng thái, tổng tiền, thanh toán, các dòng hàng)";
        }

        @Override
        public String permission() {
            return Permissions.SALES_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Hóa đơn HD000001 gồm những gì?", "Tra cứu hóa đơn HD000005");
        }

        @Override
        @PreAuthorize(Permissions.SALES_READ)
        public ToolAnswer execute(ToolArgs args) {
            String code = codeOf(args);
            Optional<InvoiceResponse> found = code == null ? Optional.empty()
                    : exact(invoiceService.search(code, null, PageUtil.newestFirst(0, 5)).getItems(), InvoiceResponse::getCode, code);
            if (found.isEmpty()) {
                return notFound(code, "/home/invoice", "Mở Hóa đơn");
            }
            InvoiceResponse invoice = found.get();
            StringBuilder md = new StringBuilder("**Hóa đơn ").append(invoice.getCode()).append("**\n\n");
            md.append(table(List.of("Thông tin", "Giá trị"), List.of(
                    List.of("Trạng thái", status(invoice.getStatusInvoice())),
                    List.of("Thời gian lập", dateTime(invoice.getInvoiceCreateDate() != null ? invoice.getInvoiceCreateDate() : invoice.getCreatedDate())),
                    List.of("Nhân viên", employeeOf(invoice.getEmployee())),
                    List.of("Khách hàng", invoice.getCustomer() != null ? invoice.getCustomer().getName() : "Khách lẻ"),
                    List.of("Kho", warehouseOf(invoice.getWarehouse())),
                    List.of("Giảm giá hóa đơn", money(invoice.getDiscount())),
                    List.of("Tổng tiền", money(invoice.getTotalPrice())),
                    List.of("Thanh toán", payment(invoice.getPaymentMethod())),
                    List.of("Khách đưa / Tiền thối", money(invoice.getAmountPaid()) + " / " + money(invoice.getChangeAmount())))));
            md.append("\n**Dòng hàng**\n\n");
            md.append(table(List.of("Sản phẩm", "SL", "Đơn giá", "Giảm %", "Thành tiền"), invoice.getInvoiceDetails().stream()
                    .sorted(Comparator.comparing(line -> productOf(line.getProduct())))
                    .map(line -> List.of(productOf(line.getProduct()), number(line.getQuantity()), money(line.getSalePrice()),
                            number(line.getDiscountPercent()), money(line.getLineTotal())))
                    .toList()));
            return ToolAnswer.of(md.toString(), "/home/invoice", "Mở Hóa đơn");
        }

    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class SalesReturnLookupTool implements ChatTool {

        ISalesReturnService salesReturnService;

        @Override
        public String name() {
            return "sales_return_lookup";
        }

        @Override
        public String description() {
            return "Chi tiết một phiếu trả hàng theo mã TH (hóa đơn gốc, lý do, tiền hoàn, các dòng trả)";
        }

        @Override
        public String permission() {
            return Permissions.SALES_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Phiếu trả hàng TH000001 hoàn bao nhiêu tiền?");
        }

        @Override
        @PreAuthorize(Permissions.SALES_READ)
        public ToolAnswer execute(ToolArgs args) {
            String code = codeOf(args);
            Optional<SalesReturnResponse> found = code == null ? Optional.empty()
                    : exact(salesReturnService.search(code, null, PageUtil.newestFirst(0, 5)).getItems(), SalesReturnResponse::getCode, code);
            if (found.isEmpty()) {
                return notFound(code, "/home/sales-return", "Mở Trả hàng");
            }
            SalesReturnResponse salesReturn = found.get();
            StringBuilder md = new StringBuilder("**Phiếu trả hàng ").append(salesReturn.getCode()).append("**\n\n");
            md.append(table(List.of("Thông tin", "Giá trị"), List.of(
                    List.of("Trạng thái", status(salesReturn.getStatusInvoice())),
                    List.of("Hóa đơn gốc", cell(salesReturn.getInvoiceCode())),
                    List.of("Ngày trả", dateTime(salesReturn.getReturnDate() != null ? salesReturn.getReturnDate() : salesReturn.getCreatedDate())),
                    List.of("Lý do", cell(salesReturn.getReason())),
                    List.of("Nhân viên", employeeOf(salesReturn.getEmployee())),
                    List.of("Kho nhận lại", warehouseOf(salesReturn.getWarehouse())),
                    List.of("Tiền hoàn", money(salesReturn.getRefundAmount())),
                    List.of("Hình thức hoàn", payment(salesReturn.getRefundMethod())))));
            md.append("\n**Dòng trả**\n\n");
            md.append(table(List.of("Sản phẩm", "SL", "Đơn giá hoàn", "Thành tiền"), salesReturn.getSalesReturnDetails().stream()
                    .map(line -> List.of(productOf(line.getProduct()), number(line.getQuantity()), money(line.getUnitRefund()),
                            money(line.getLineTotal())))
                    .toList()));
            return ToolAnswer.of(md.toString(), "/home/sales-return", "Mở Trả hàng");
        }

    }

    // PN (stock in), PX (stock out), CK (transfer), KK (stock take)
    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class StockDocumentLookupTool implements ChatTool {

        IStockInService stockInService;
        IStockOutService stockOutService;
        IStockTransferService stockTransferService;
        IStockTakeService stockTakeService;

        @Override
        public String name() {
            return "stock_document_lookup";
        }

        @Override
        public String description() {
            return "Chi tiết một chứng từ kho theo mã: phiếu nhập PN, phiếu xuất PX, phiếu chuyển kho CK, phiếu kiểm kê KK";
        }

        @Override
        public String permission() {
            return Permissions.STOCK_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Phiếu nhập PN000001 nhập những gì?", "Phiếu chuyển kho CK000001 đã nhận chưa?");
        }

        @Override
        @PreAuthorize(Permissions.STOCK_READ)
        public ToolAnswer execute(ToolArgs args) {
            String code = codeOf(args);
            if (code == null || code.length() < 3) {
                return notFound(code, "/home/stockin", "Mở Nhập kho");
            }
            return switch (code.substring(0, 2)) {
                case "PN" -> stockIn(code);
                case "PX" -> stockOut(code);
                case "CK" -> transfer(code);
                case "KK" -> stockTake(code);
                default -> notFound(code, "/home/stockin", "Mở Nhập kho");
            };
        }

        private ToolAnswer stockIn(String code) {
            Optional<StockInResponse> found = exact(stockInService.search(code, null, PageUtil.newestFirst(0, 5)).getItems(), StockInResponse::getCode, code);
            if (found.isEmpty()) {
                return notFound(code, "/home/stockin", "Mở Nhập kho");
            }
            StockInResponse doc = found.get();
            String md = "**Phiếu nhập kho " + doc.getCode() + "**\n\n" + table(List.of("Thông tin", "Giá trị"), List.of(
                    List.of("Trạng thái", status(doc.getStatusInvoice())),
                    List.of("Ngày nhập", dateTime(doc.getStockInDate() != null ? doc.getStockInDate() : doc.getCreatedDate())),
                    List.of("Nhà cung cấp", doc.getSupplier() != null ? doc.getSupplier().getName() : "—"),
                    List.of("Kho", warehouseOf(doc.getWarehouse())),
                    List.of("Nhân viên", employeeOf(doc.getEmployee())),
                    List.of("Tổng tiền", money(doc.getTotalPrice()))))
                    + "\n**Dòng hàng**\n\n" + table(List.of("Sản phẩm", "SL", "Giá nhập", "Lô", "Hạn dùng"), doc.getStockInDetails().stream()
                    .map(line -> List.of(productOf(line.getProduct()), number(line.getQuantity()), money(line.getInputPrice()),
                            cell(line.getLotNumber()), date(line.getExpiryDate())))
                    .toList());
            return ToolAnswer.of(md, "/home/stockin", "Mở Nhập kho");
        }

        private ToolAnswer stockOut(String code) {
            Optional<StockOutResponse> found = exact(stockOutService.search(code, null, PageUtil.newestFirst(0, 5)).getItems(), StockOutResponse::getCode, code);
            if (found.isEmpty()) {
                return notFound(code, "/home/stockout", "Mở Xuất kho");
            }
            StockOutResponse doc = found.get();
            String md = "**Phiếu xuất kho " + doc.getCode() + "**\n\n" + table(List.of("Thông tin", "Giá trị"), List.of(
                    List.of("Trạng thái", status(doc.getStatusInvoice())),
                    List.of("Ngày xuất", dateTime(doc.getStockOutDate() != null ? doc.getStockOutDate() : doc.getCreatedDate())),
                    List.of("Lý do", cell(doc.getReason())),
                    List.of("Kho", warehouseOf(doc.getWarehouse())),
                    List.of("Nhân viên", employeeOf(doc.getEmployee())),
                    List.of("Giá trị (giá vốn)", money(doc.getTotalPrice()))))
                    + "\n**Dòng hàng**\n\n" + table(List.of("Sản phẩm", "SL", "Giá vốn", "Thành tiền"), doc.getStockOutDetails().stream()
                    .map(line -> List.of(productOf(line.getProduct()), number(line.getQuantity()), money(line.getInputPrice()),
                            money(line.getTotalPrice())))
                    .toList());
            return ToolAnswer.of(md, "/home/stockout", "Mở Xuất kho");
        }

        private ToolAnswer transfer(String code) {
            Optional<StockTransferResponse> found = exact(stockTransferService.search(code, null, PageUtil.newestFirst(0, 5)).getItems(), StockTransferResponse::getCode, code);
            if (found.isEmpty()) {
                return notFound(code, "/home/stock-transfer", "Mở Chuyển kho");
            }
            StockTransferResponse doc = found.get();
            String md = "**Phiếu chuyển kho " + doc.getCode() + "**\n\n" + table(List.of("Thông tin", "Giá trị"), List.of(
                    List.of("Trạng thái", status(doc.getStatusInvoice())),
                    List.of("Ngày chuyển", dateTime(doc.getTransferDate() != null ? doc.getTransferDate() : doc.getCreatedDate())),
                    List.of("Từ kho", warehouseOf(doc.getFromWarehouse())),
                    List.of("Đến kho", warehouseOf(doc.getToWarehouse())),
                    List.of("Nhân viên", employeeOf(doc.getEmployee())),
                    List.of("Ghi chú", cell(doc.getNote()))))
                    + "\n**Dòng hàng**\n\n" + table(List.of("Sản phẩm", "SL"), doc.getStockTransferDetails().stream()
                    .map(line -> List.of(productOf(line.getProduct()), number(line.getQuantity())))
                    .toList());
            return ToolAnswer.of(md, "/home/stock-transfer", "Mở Chuyển kho");
        }

        private ToolAnswer stockTake(String code) {
            Optional<StockTakeResponse> found = exact(stockTakeService.search(code, null, PageUtil.newestFirst(0, 5)).getItems(), StockTakeResponse::getCode, code);
            if (found.isEmpty()) {
                return notFound(code, "/home/stock-take", "Mở Kiểm kê");
            }
            StockTakeResponse doc = found.get();
            String md = "**Phiếu kiểm kê " + doc.getCode() + "**\n\n" + table(List.of("Thông tin", "Giá trị"), List.of(
                    List.of("Trạng thái", status(doc.getStatusInvoice())),
                    List.of("Ngày kiểm", dateTime(doc.getStockTakeDate() != null ? doc.getStockTakeDate() : doc.getCreatedDate())),
                    List.of("Kho", warehouseOf(doc.getWarehouse())),
                    List.of("Nhân viên", employeeOf(doc.getEmployee())),
                    List.of("Tổng chênh lệch", number(doc.getTotalDifference())),
                    List.of("Ghi chú", cell(doc.getNote()))))
                    + "\n**Dòng kiểm**\n\n" + table(List.of("Sản phẩm", "Hệ thống", "Thực đếm", "Chênh lệch"), doc.getStockTakeDetails().stream()
                    .map(line -> List.of(productOf(line.getProduct()), number(line.getSystemQuantity()),
                            number(line.getCountedQuantity()), number(line.getDifference())))
                    .toList());
            return ToolAnswer.of(md, "/home/stock-take", "Mở Kiểm kê");
        }

    }

    static boolean inDateRange(Instant docInstant, Instant fallbackInstant, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        Instant effective = docInstant != null ? docInstant : fallbackInstant;
        if (effective == null) {
            return false;
        }
        LocalDate d = effective.atZone(ShiftUtil.VIETNAM_ZONE).toLocalDate();
        if (from != null && d.isBefore(from)) {
            return false;
        }
        if (to != null && d.isAfter(to)) {
            return false;
        }
        return true;
    }

    static String periodText(ToolArgs args) {
        if (args.from() != null && args.to() != null) {
            return " " + range(args.from(), args.to());
        }
        return "";
    }

    static String cleanKeyword(ToolArgs args) {
        String raw = args.keyword();
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw.trim())) {
            return "";
        }
        return raw.trim();
    }

    static boolean isCompleted(StatusInvoiceResponse status) {
        return status != null && "COMPLETED".equalsIgnoreCase(status.getCode());
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class StockInSearchTool implements ChatTool {

        IStockInService stockInService;

        @Override
        public String name() {
            return "stock_in_search";
        }

        @Override
        public String description() {
            return "Thống kê, đếm số lượng và tra cứu danh sách phiếu nhập kho (PN) theo ngày/tháng, kho hoặc nhà cung cấp";
        }

        @Override
        public String permission() {
            return Permissions.STOCK_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Danh sách phiếu nhập kho gần đây", "Thống kê nhập kho theo nhà cung cấp");
        }

        @Override
        @PreAuthorize(Permissions.STOCK_READ)
        public ToolAnswer execute(ToolArgs args) {
            String kw = cleanKeyword(args);
            List<StockInResponse> all = stockInService.search(kw, null, PageUtil.newestFirstUnbounded(2000)).getItems();
            List<StockInResponse> items = all.stream()
                    .filter(doc -> inDateRange(doc.getStockInDate(), doc.getCreatedDate(), args.from(), args.to()))
                    .filter(doc -> args.warehouseId() == null || (doc.getWarehouse() != null && args.warehouseId().equals(doc.getWarehouse().getId())))
                    .toList();

            String scope = ReportTools.scopeOf(args);
            String title = "**Thống kê phiếu nhập kho" + periodText(args) + "** (" + scope + ")\n\n";
            if (items.isEmpty()) {
                return ToolAnswer.of(title + "Không có phiếu nhập kho nào phát sinh" + periodText(args)
                        + (!kw.isEmpty() ? " khớp từ khóa **" + kw + "**" : "") + ".", "/home/stockin", "Đến trang Quản lý Nhập kho");
            }

            long completed = items.stream().filter(d -> isCompleted(d.getStatusInvoice())).count();
            BigDecimal totalValue = items.stream()
                    .map(StockInResponse::getTotalPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long totalQty = items.stream()
                    .map(StockInResponse::getTotalProduct)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum();

            StringBuilder md = new StringBuilder(title);
            md.append(kpiCards(List.of(
                    new KpiCard("Tổng phiếu nhập kho", number(items.size()) + " phiếu", number(completed) + " phiếu đã hoàn thành", "primary"),
                    new KpiCard("Tổng giá trị nhập", money(totalValue), "Tổng tiền trên phiếu nhập", "success"),
                    new KpiCard("Tổng SL hàng nhập", number(totalQty) + " SP", "Tổng số lượng sản phẩm nhập", "primary")
            ))).append("\n");

            md.append("Có tổng cộng **").append(number(items.size())).append(" phiếu nhập kho**")
                    .append(periodText(args)).append(" (").append(number(completed)).append(" đã hoàn thành, tổng tiền: **")
                    .append(money(totalValue)).append("**).\n\n");

            List<String> headers = List.of("Mã phiếu", "Nhà cung cấp", "Kho nhập", "Nhân viên", "Tổng tiền", "Trạng thái", "Thời gian");
            List<List<String>> rows = items.stream().map(d -> List.of(
                    cell(d.getCode()),
                    d.getSupplier() != null ? cell(d.getSupplier().getName()) : "—",
                    warehouseOf(d.getWarehouse()),
                    employeeOf(d.getEmployee()),
                    money(d.getTotalPrice()),
                    status(d.getStatusInvoice()),
                    dateTime(d.getStockInDate() != null ? d.getStockInDate() : d.getCreatedDate())
            )).toList();

            if (rows.size() <= 5) {
                md.append(table(headers, rows));
            } else {
                md.append(table(headers, rows.subList(0, 5)));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + rows.size() + " phiếu nhập kho (mở rộng)", table(headers, rows)));
            }
            md.append(downloadCsvLink("Tải danh sách phiếu nhập kho (.CSV)", "danh-sach-phieu-nhap-kho.csv", headers, rows));
            return ToolAnswer.of(md.toString(), "/home/stockin", "Đến trang Quản lý Nhập kho");
        }
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class StockOutSearchTool implements ChatTool {

        IStockOutService stockOutService;

        @Override
        public String name() {
            return "stock_out_search";
        }

        @Override
        public String description() {
            return "Thống kê, đếm số lượng và tra cứu danh sách phiếu xuất kho (PX) theo ngày/tháng, kho hoặc lý do";
        }

        @Override
        public String permission() {
            return Permissions.STOCK_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Danh sách phiếu xuất kho gần đây", "Thống kê phiếu xuất kho tháng này");
        }

        @Override
        @PreAuthorize(Permissions.STOCK_READ)
        public ToolAnswer execute(ToolArgs args) {
            String kw = cleanKeyword(args);
            List<StockOutResponse> all = stockOutService.search(kw, null, PageUtil.newestFirstUnbounded(2000)).getItems();
            List<StockOutResponse> items = all.stream()
                    .filter(doc -> inDateRange(doc.getStockOutDate(), doc.getCreatedDate(), args.from(), args.to()))
                    .filter(doc -> args.warehouseId() == null || (doc.getWarehouse() != null && args.warehouseId().equals(doc.getWarehouse().getId())))
                    .toList();

            String scope = ReportTools.scopeOf(args);
            String title = "**Thống kê phiếu xuất kho" + periodText(args) + "** (" + scope + ")\n\n";
            if (items.isEmpty()) {
                return ToolAnswer.of(title + "Không có phiếu xuất kho nào phát sinh" + periodText(args)
                        + (!kw.isEmpty() ? " khớp từ khóa **" + kw + "**" : "") + ".", "/home/stockout", "Đến trang Quản lý Xuất kho");
            }

            long completed = items.stream().filter(d -> isCompleted(d.getStatusInvoice())).count();
            BigDecimal totalValue = items.stream()
                    .map(StockOutResponse::getTotalPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long totalQty = items.stream()
                    .map(StockOutResponse::getTotalProduct)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum();

            StringBuilder md = new StringBuilder(title);
            md.append(kpiCards(List.of(
                    new KpiCard("Tổng phiếu xuất kho", number(items.size()) + " phiếu", number(completed) + " phiếu đã hoàn thành", "primary"),
                    new KpiCard("Tổng giá trị xuất", money(totalValue), "Tính theo giá vốn", "warning"),
                    new KpiCard("Tổng SL hàng xuất", number(totalQty) + " SP", "Tổng số lượng sản phẩm xuất", "primary")
            ))).append("\n");

            md.append("Có tổng cộng **").append(number(items.size())).append(" phiếu xuất kho**")
                    .append(periodText(args)).append(" (").append(number(completed)).append(" đã hoàn thành, tổng giá trị: **")
                    .append(money(totalValue)).append("**).\n\n");

            List<String> headers = List.of("Mã phiếu", "Kho xuất", "Lý do", "Nhân viên", "Giá trị xuất", "Trạng thái", "Thời gian");
            List<List<String>> rows = items.stream().map(d -> List.of(
                    cell(d.getCode()),
                    warehouseOf(d.getWarehouse()),
                    cell(d.getReason()),
                    employeeOf(d.getEmployee()),
                    money(d.getTotalPrice()),
                    status(d.getStatusInvoice()),
                    dateTime(d.getStockOutDate() != null ? d.getStockOutDate() : d.getCreatedDate())
            )).toList();

            if (rows.size() <= 5) {
                md.append(table(headers, rows));
            } else {
                md.append(table(headers, rows.subList(0, 5)));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + rows.size() + " phiếu xuất kho (mở rộng)", table(headers, rows)));
            }
            md.append(downloadCsvLink("Tải danh sách phiếu xuất kho (.CSV)", "danh-sach-phieu-xuat-kho.csv", headers, rows));
            return ToolAnswer.of(md.toString(), "/home/stockout", "Đến trang Quản lý Xuất kho");
        }
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class StockTransferSearchTool implements ChatTool {

        IStockTransferService stockTransferService;

        @Override
        public String name() {
            return "stock_transfer_search";
        }

        @Override
        public String description() {
            return "Thống kê, đếm số lượng và tra cứu danh sách phiếu chuyển kho (CK) theo ngày/tháng hoặc kho";
        }

        @Override
        public String permission() {
            return Permissions.STOCK_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Có bao nhiêu phiếu chuyển kho trong tháng 10?", "Danh sách phiếu chuyển kho gần đây");
        }

        @Override
        @PreAuthorize(Permissions.STOCK_READ)
        public ToolAnswer execute(ToolArgs args) {
            String kw = cleanKeyword(args);
            List<StockTransferResponse> all = stockTransferService.search(kw, null, PageUtil.newestFirstUnbounded(2000)).getItems();
            List<StockTransferResponse> items = all.stream()
                    .filter(doc -> inDateRange(doc.getTransferDate(), doc.getCreatedDate(), args.from(), args.to()))
                    .filter(doc -> args.warehouseId() == null
                            || (doc.getFromWarehouse() != null && args.warehouseId().equals(doc.getFromWarehouse().getId()))
                            || (doc.getToWarehouse() != null && args.warehouseId().equals(doc.getToWarehouse().getId())))
                    .toList();

            String scope = ReportTools.scopeOf(args);
            String title = "**Thống kê phiếu chuyển kho" + periodText(args) + "** (" + scope + ")\n\n";
            if (items.isEmpty()) {
                return ToolAnswer.of(title + "Không có phiếu chuyển kho nào phát sinh" + periodText(args) + ".",
                        "/home/stock-transfer", "Đến trang Quản lý Chuyển kho");
            }

            long completed = items.stream().filter(d -> isCompleted(d.getStatusInvoice())).count();
            long totalQty = items.stream()
                    .map(StockTransferResponse::getTotalProduct)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum();

            StringBuilder md = new StringBuilder(title);
            md.append(kpiCards(List.of(
                    new KpiCard("Tổng phiếu chuyển kho", number(items.size()) + " phiếu", number(completed) + " phiếu đã hoàn thành", "primary"),
                    new KpiCard("Tổng SL điều chuyển", number(totalQty) + " SP", "Tổng số lượng hàng luân chuyển", "success")
            ))).append("\n");

            md.append("Có tổng cộng **").append(number(items.size())).append(" phiếu chuyển kho**")
                    .append(periodText(args)).append(" (").append(number(completed)).append(" đã hoàn thành).\n\n");

            List<String> headers = List.of("Mã phiếu", "Từ kho", "Đến kho", "SL SP", "Nhân viên", "Trạng thái", "Thời gian");
            List<List<String>> rows = items.stream().map(d -> List.of(
                    cell(d.getCode()),
                    warehouseOf(d.getFromWarehouse()),
                    warehouseOf(d.getToWarehouse()),
                    number(d.getTotalProduct()),
                    employeeOf(d.getEmployee()),
                    status(d.getStatusInvoice()),
                    dateTime(d.getTransferDate() != null ? d.getTransferDate() : d.getCreatedDate())
            )).toList();

            if (rows.size() <= 5) {
                md.append(table(headers, rows));
            } else {
                md.append(table(headers, rows.subList(0, 5)));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + rows.size() + " phiếu chuyển kho (mở rộng)", table(headers, rows)));
            }
            md.append(downloadCsvLink("Tải danh sách phiếu chuyển kho (.CSV)", "danh-sach-phieu-chuyen-kho.csv", headers, rows));
            return ToolAnswer.of(md.toString(), "/home/stock-transfer", "Đến trang Quản lý Chuyển kho");
        }
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class StockTakeSearchTool implements ChatTool {

        IStockTakeService stockTakeService;

        @Override
        public String name() {
            return "stock_take_search";
        }

        @Override
        public String description() {
            return "Thống kê, đếm số lượng và tra cứu danh sách phiếu kiểm kê kho (KK) theo ngày/tháng hoặc kho";
        }

        @Override
        public String permission() {
            return Permissions.STOCK_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Có bao nhiêu phiếu kiểm kê trong tháng 10?", "Danh sách phiếu kiểm kê kho gần đây");
        }

        @Override
        @PreAuthorize(Permissions.STOCK_READ)
        public ToolAnswer execute(ToolArgs args) {
            String kw = cleanKeyword(args);
            List<StockTakeResponse> all = stockTakeService.search(kw, null, PageUtil.newestFirstUnbounded(2000)).getItems();
            List<StockTakeResponse> items = all.stream()
                    .filter(doc -> inDateRange(doc.getStockTakeDate(), doc.getCreatedDate(), args.from(), args.to()))
                    .filter(doc -> args.warehouseId() == null || (doc.getWarehouse() != null && args.warehouseId().equals(doc.getWarehouse().getId())))
                    .toList();

            String scope = ReportTools.scopeOf(args);
            String title = "**Thống kê phiếu kiểm kê kho" + periodText(args) + "** (" + scope + ")\n\n";
            if (items.isEmpty()) {
                return ToolAnswer.of(title + "Không có phiếu kiểm kê kho nào phát sinh" + periodText(args) + ".",
                        "/home/stock-take", "Đến trang Quản lý Kiểm kê");
            }

            long completed = items.stream().filter(d -> isCompleted(d.getStatusInvoice())).count();
            long totalDiff = items.stream()
                    .map(StockTakeResponse::getTotalDifference)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum();

            StringBuilder md = new StringBuilder(title);
            md.append(kpiCards(List.of(
                    new KpiCard("Tổng phiếu kiểm kê", number(items.size()) + " phiếu", number(completed) + " phiếu đã hoàn thành", "primary"),
                    new KpiCard("Tổng chênh lệch", number(totalDiff) + " SP", "Chênh lệch thực đếm so với hệ thống", totalDiff == 0 ? "success" : "warning")
            ))).append("\n");

            md.append("Có tổng cộng **").append(number(items.size())).append(" phiếu kiểm kê kho**")
                    .append(periodText(args)).append(" (").append(number(completed)).append(" đã hoàn thành).\n\n");

            List<String> headers = List.of("Mã phiếu", "Kho kiểm kê", "Chênh lệch", "Ghi chú", "Nhân viên", "Trạng thái", "Thời gian");
            List<List<String>> rows = items.stream().map(d -> List.of(
                    cell(d.getCode()),
                    warehouseOf(d.getWarehouse()),
                    number(d.getTotalDifference()),
                    cell(d.getNote()),
                    employeeOf(d.getEmployee()),
                    status(d.getStatusInvoice()),
                    dateTime(d.getStockTakeDate() != null ? d.getStockTakeDate() : d.getCreatedDate())
            )).toList();

            if (rows.size() <= 5) {
                md.append(table(headers, rows));
            } else {
                md.append(table(headers, rows.subList(0, 5)));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + rows.size() + " phiếu kiểm kê kho (mở rộng)", table(headers, rows)));
            }
            md.append(downloadCsvLink("Tải danh sách phiếu kiểm kê (.CSV)", "danh-sach-phieu-kiem-ke.csv", headers, rows));
            return ToolAnswer.of(md.toString(), "/home/stock-take", "Đến trang Quản lý Kiểm kê");
        }
    }

}
