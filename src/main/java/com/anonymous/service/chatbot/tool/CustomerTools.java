package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.*;
import com.anonymous.service.ICustomerService;
import com.anonymous.service.IInvoiceService;
import com.anonymous.service.ISalesReturnService;
import com.anonymous.utils.PageUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

public final class CustomerTools {

    private CustomerTools() {
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
    public static class CustomerLookupTool implements ChatTool {

        ICustomerService customerService;

        @Override
        public String name() {
            return "customer_lookup";
        }

        @Override
        public String description() {
            return "Tra cứu thông tin khách hàng theo tên, số điện thoại hoặc email (điểm tích lũy, địa chỉ, liên hệ)";
        }

        @Override
        public String permission() {
            return Permissions.SALES_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Có khách hàng tên Đặng Thanh Mai không?", "Tìm khách hàng 0822000082", "Thông tin khách hàng Mai");
        }

        @Override
        @PreAuthorize(Permissions.SALES_READ)
        public ToolAnswer execute(ToolArgs args) {
            String rawKw = args.keyword();
            String keyword = (rawKw == null || rawKw.isBlank() || "null".equalsIgnoreCase(rawKw.trim())) ? "" : rawKw.trim();
            int limit = args.limit() == null ? 50 : Math.clamp(args.limit(), 5, 100);
            PageResponse<CustomerResponse> page = customerService.search(keyword, PageUtil.newestFirst(0, limit));
            List<CustomerResponse> customers = page.getItems();
            if (customers.isEmpty()) {
                String msg = keyword.isBlank()
                        ? "Không tìm thấy khách hàng nào trong hệ thống."
                        : "Không tìm thấy khách hàng nào khớp với từ khóa **" + keyword + "**. Vui lòng kiểm tra lại tên hoặc số điện thoại.";
                return ToolAnswer.of(msg, "/home/customer", "Đến trang Khách hàng");
            }

            StringBuilder md = new StringBuilder();
            if (!keyword.isBlank()) {
                md.append("Tìm thấy **").append(page.getTotalItems()).append("** khách hàng khớp với từ khóa **").append(keyword).append("**:\n\n");
            } else {
                md.append("Danh sách khách hàng (tổng cộng **").append(page.getTotalItems()).append("** khách hàng):\n\n");
            }

            List<String> headers = List.of("Họ và tên", "SĐT", "Giới tính", "Điểm tích lũy", "Địa chỉ");
            List<List<String>> allRows = new ArrayList<>();
            for (CustomerResponse c : customers) {
                allRows.add(List.of(
                        c.getName(),
                        c.getPhoneNumber() != null ? c.getPhoneNumber() : "—",
                        c.getGender() != null ? c.getGender() : "—",
                        c.getAccumulatedPoints() != null ? number(c.getAccumulatedPoints()) : "0",
                        addressOf(c.getAddress())
                ));
            }

            if (page.getTotalItems() <= 5) {
                md.append(table(headers, allRows));
            } else {
                List<List<String>> topRows = allRows.subList(0, Math.min(5, allRows.size()));
                md.append(table(headers, topRows));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + page.getTotalItems() + " khách hàng (mở rộng)", table(headers, allRows)));
                md.append(downloadCsvLink("Tải danh sách khách hàng (.CSV)", "danh-sach-khach-hang.csv", headers, allRows));
            }
            return ToolAnswer.of(md.toString(), "/home/customer", "Đến trang Khách hàng");
        }
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class SalesReturnSearchTool implements ChatTool {

        ISalesReturnService salesReturnService;

        @Override
        public String name() {
            return "sales_return_search";
        }

        @Override
        public String description() {
            return "Tìm kiếm phiếu trả hàng theo tên khách hàng, số điện thoại, lý do hoặc nhân viên lập";
        }

        @Override
        public String permission() {
            return Permissions.SALES_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Có ai tên Mai từng trả hàng không?", "Tìm phiếu trả hàng của khách hàng Đặng Thanh Mai", "Danh sách phiếu trả hàng gần đây");
        }

        @Override
        @PreAuthorize(Permissions.SALES_READ)
        public ToolAnswer execute(ToolArgs args) {
            String rawKw = args.keyword();
            String keyword = (rawKw == null || rawKw.isBlank() || "null".equalsIgnoreCase(rawKw.trim())) ? "" : rawKw.trim();
            int limit = args.limit() == null ? 50 : Math.clamp(args.limit(), 5, 100);
            PageResponse<SalesReturnResponse> page = salesReturnService.search(keyword, null, PageUtil.newestFirst(0, limit));
            List<SalesReturnResponse> items = page.getItems();
            if (items.isEmpty()) {
                String msg = keyword.isBlank()
                        ? "Hiện chưa có phiếu trả hàng nào trong hệ thống."
                        : "Không tìm thấy phiếu trả hàng nào liên quan đến **" + keyword + "**.";
                return ToolAnswer.of(msg, "/home/sales-return", "Đến trang Quản lý Trả hàng");
            }

            StringBuilder md = new StringBuilder();
            if (!keyword.isBlank()) {
                md.append("Tìm thấy **").append(page.getTotalItems()).append("** phiếu trả hàng liên quan đến **").append(keyword).append("**:\n\n");
            } else {
                md.append("Danh sách phiếu trả hàng (tổng cộng **").append(page.getTotalItems()).append("** phiếu):\n\n");
            }

            List<String> headers = List.of("Mã phiếu", "Khách hàng", "Hóa đơn gốc", "Tiền hoàn", "Lý do", "Thời gian");
            List<List<String>> allRows = new ArrayList<>();
            for (SalesReturnResponse r : items) {
                String customerName = r.getCustomer() != null ? r.getCustomer().getName() : "Khách lẻ";
                String invoiceCode = r.getInvoiceCode() != null ? r.getInvoiceCode() : "—";
                allRows.add(List.of(
                        r.getCode(),
                        customerName,
                        invoiceCode,
                        money(r.getRefundAmount()),
                        r.getReason() != null ? r.getReason() : "—",
                        dateTime(r.getReturnDate() != null ? r.getReturnDate() : r.getCreatedDate())
                ));
            }

            if (page.getTotalItems() <= 5) {
                md.append(table(headers, allRows));
            } else {
                List<List<String>> topRows = allRows.subList(0, Math.min(5, allRows.size()));
                md.append(table(headers, topRows));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + page.getTotalItems() + " phiếu trả hàng (mở rộng)", table(headers, allRows)));
                md.append(downloadCsvLink("Tải danh sách phiếu trả hàng (.CSV)", "danh-sach-tra-hang.csv", headers, allRows));
            }
            return ToolAnswer.of(md.toString(), "/home/sales-return", "Đến trang Quản lý Trả hàng");
        }
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class InvoiceSearchTool implements ChatTool {

        IInvoiceService invoiceService;

        @Override
        public String name() {
            return "invoice_search";
        }

        @Override
        public String description() {
            return "Tìm kiếm danh sách hóa đơn theo tên khách hàng, số điện thoại hoặc nhân viên lập";
        }

        @Override
        public String permission() {
            return Permissions.SALES_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Hóa đơn của khách hàng Mai", "Tìm hóa đơn của khách hàng Đặng Thanh Mai");
        }

        @Override
        @PreAuthorize(Permissions.SALES_READ)
        public ToolAnswer execute(ToolArgs args) {
            String rawKw = args.keyword();
            String keyword = (rawKw == null || rawKw.isBlank() || "null".equalsIgnoreCase(rawKw.trim())) ? "" : rawKw.trim();
            int limit = args.limit() == null ? 50 : Math.clamp(args.limit(), 5, 100);
            PageResponse<InvoiceResponse> page = invoiceService.search(keyword, args.from(), args.to(), null, PageUtil.newestFirst(0, limit));
            List<InvoiceResponse> items = page.getItems();

            String periodText = (args.from() != null && args.to() != null)
                    ? (args.from().equals(args.to()) ? " vào ngày " + args.from() : " từ ngày " + args.from() + " đến " + args.to())
                    : "";

            if (items.isEmpty()) {
                String msg = keyword.isBlank()
                        ? "Không tìm thấy hóa đơn nào" + periodText + " trong hệ thống."
                        : "Không tìm thấy hóa đơn nào liên quan đến **" + keyword + "**" + periodText + ".";
                return ToolAnswer.of(msg, "/home/invoice", "Đến trang Quản lý Hóa đơn");
            }

            StringBuilder md = new StringBuilder();
            if (!keyword.isBlank()) {
                md.append("Tìm thấy **").append(page.getTotalItems()).append("** hóa đơn liên quan đến **").append(keyword).append("**").append(periodText).append(":\n\n");
            } else {
                md.append("Danh sách hóa đơn").append(periodText).append(" (tổng cộng **").append(page.getTotalItems()).append("** hóa đơn):\n\n");
            }

            List<String> headers = List.of("Mã HĐ", "Khách hàng", "Tổng tiền", "Trạng thái", "Thanh toán", "Thời gian");
            List<List<String>> allRows = new ArrayList<>();
            for (InvoiceResponse inv : items) {
                String customerName = inv.getCustomer() != null ? inv.getCustomer().getName() : "Khách lẻ";
                allRows.add(List.of(
                        inv.getCode(),
                        customerName,
                        money(inv.getTotalPrice()),
                        status(inv.getStatusInvoice()),
                        payment(inv.getPaymentMethod()),
                        dateTime(inv.getInvoiceCreateDate() != null ? inv.getInvoiceCreateDate() : inv.getCreatedDate())
                ));
            }

            if (page.getTotalItems() <= 5) {
                md.append(table(headers, allRows));
            } else {
                List<List<String>> topRows = allRows.subList(0, Math.min(5, allRows.size()));
                md.append(table(headers, topRows));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + page.getTotalItems() + " hóa đơn (mở rộng)", table(headers, allRows)));
                md.append(downloadCsvLink("Tải danh sách hóa đơn (.CSV)", "danh-sach-hoa-don.csv", headers, allRows));
            }
            return ToolAnswer.of(md.toString(), "/home/invoice", "Đến trang Quản lý Hóa đơn");
        }
    }

}
