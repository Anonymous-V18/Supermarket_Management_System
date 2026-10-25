package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.*;
import com.anonymous.service.*;
import com.anonymous.utils.PageUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

}
