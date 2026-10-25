package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.*;
import com.anonymous.entity.Employee;
import com.anonymous.entity.Promotion;
import com.anonymous.entity.ShiftChecklist;
import com.anonymous.entity.User;
import com.anonymous.repository.IInvoiceRepository;
import com.anonymous.repository.IPromotionRepository;
import com.anonymous.repository.IShiftChecklistRepository;
import com.anonymous.service.IEmployeeService;
import com.anonymous.service.IShiftChecklistService;
import com.anonymous.utils.PageUtil;
import com.anonymous.utils.ShiftUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

public final class OperationsTools {

    private OperationsTools() {
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class EmployeeLookupTool implements ChatTool {

        IEmployeeService employeeService;

        @Override
        public String name() {
            return "employee_lookup";
        }

        @Override
        public String description() {
            return "Tra cứu thông tin nhân viên theo tên, mã NV hoặc số điện thoại (chức vụ, liên hệ)";
        }

        @Override
        public String permission() {
            return Permissions.ADMIN;
        }

        @Override
        public List<String> examples() {
            return List.of("Tìm nhân viên Nguyễn Văn A", "Thông tin nhân viên NV00001", "Danh sách nhân viên");
        }

        @Override
        @PreAuthorize(Permissions.ADMIN)
        public ToolAnswer execute(ToolArgs args) {
            String keyword = args.keyword() == null ? "" : args.keyword().trim();
            int limit = args.limit() == null ? 50 : Math.clamp(args.limit(), 5, 100);
            PageResponse<EmployeeResponse> page = employeeService.search(keyword, PageUtil.newestFirst(0, limit));
            List<EmployeeResponse> employees = page.getItems();
            if (employees.isEmpty()) {
                String msg = keyword.isBlank()
                        ? "Không tìm thấy nhân viên nào trong hệ thống."
                        : "Không tìm thấy nhân viên nào khớp với từ khóa **" + keyword + "**. Vui lòng kiểm tra lại.";
                return ToolAnswer.of(msg, "/home/employee", "Đến trang Quản lý Nhân viên");
            }

            StringBuilder md = new StringBuilder();
            if (!keyword.isBlank()) {
                md.append("Tìm thấy **").append(page.getTotalItems()).append("** nhân viên khớp với từ khóa **").append(keyword).append("**:\n\n");
            } else {
                md.append("Danh sách nhân viên (tổng cộng **").append(page.getTotalItems()).append("** nhân viên):\n\n");
            }

            List<String> headers = List.of("Mã NV", "Họ và tên", "Chức vụ", "SĐT", "Email");
            List<List<String>> allRows = new ArrayList<>();
            for (EmployeeResponse e : employees) {
                String pos = e.getPosition() != null ? e.getPosition().getName() : "—";
                allRows.add(List.of(
                        e.getCode() != null ? e.getCode() : "—",
                        e.getName(),
                        pos,
                        e.getPhoneNumber() != null ? e.getPhoneNumber() : "—",
                        e.getEmail() != null ? e.getEmail() : "—"
                ));
            }

            if (page.getTotalItems() <= 5) {
                md.append(table(headers, allRows));
            } else {
                List<List<String>> topRows = allRows.subList(0, Math.min(5, allRows.size()));
                md.append(table(headers, topRows));
                md.append("\n").append(collapsible("Nhấn để xem toàn bộ danh sách " + page.getTotalItems() + " nhân viên (mở rộng)", table(headers, allRows)));
                md.append(downloadCsvLink("Tải danh sách nhân viên (.CSV)", "danh-sach-nhan-vien.csv", headers, allRows));
            }
            return ToolAnswer.of(md.toString(), "/home/employee", "Đến trang Quản lý Nhân viên");
        }
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class PromotionLookupTool implements ChatTool {

        IPromotionRepository promotionRepository;

        @Override
        public String name() {
            return "promotion_lookup";
        }

        @Override
        public String description() {
            return "Tra cứu các chương trình khuyến mãi, voucher giảm giá đang áp dụng hoặc sắp tới";
        }

        @Override
        public String permission() {
            return Permissions.SALES_READ;
        }

        @Override
        public List<String> examples() {
            return List.of("Có chương trình khuyến mãi nào đang chạy?", "Các voucher giảm giá hiện tại", "Khuyến mãi hôm nay");
        }

        @Override
        @PreAuthorize(Permissions.SALES_READ)
        public ToolAnswer execute(ToolArgs args) {
            List<Promotion> promotions = promotionRepository.findAllByIsDeletedFalse();
            if (promotions.isEmpty()) {
                return ToolAnswer.of("Hiện tại không có chương trình khuyến mãi nào đang chạy.", "/home/product", "Mở Sản phẩm");
            }

            StringBuilder md = new StringBuilder("Các chương trình khuyến mãi hiện hành:\n\n");
            List<List<String>> rows = new ArrayList<>();
            for (Promotion p : promotions) {
                rows.add(List.of(
                        p.getName(),
                        p.getVoucherCode() != null ? p.getVoucherCode() : "—",
                        dateTime(p.getStartDate()) + " - " + dateTime(p.getEndDate()),
                        p.getLimitVoucher() != null ? number(p.getLimitVoucher()) : "Không giới hạn",
                        p.getDescription() != null ? p.getDescription() : "—"
                ));
            }
            md.append(table(List.of("Tên khuyến mãi", "Mã voucher", "Thời gian áp dụng", "Số lượng", "Mô tả"), rows));
            return ToolAnswer.of(md.toString(), "/home/product", "Mở Sản phẩm");
        }
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class ShiftChecklistTool implements ChatTool {

        IShiftChecklistService shiftChecklistService;

        @Override
        public String name() {
            return "shift_checklist";
        }

        @Override
        public String description() {
            return "Kiểm tra tình trạng checklist công việc ca làm việc đầu ca / cuối ca";
        }

        @Override
        public String permission() {
            return Permissions.AUTHENTICATED;
        }

        @Override
        public List<String> examples() {
            return List.of("Checklist ca sáng hôm nay", "Kiểm tra checklist đầu ca", "Tình trạng checklist ca hiện tại");
        }

        @Override
        @PreAuthorize(Permissions.AUTHENTICATED)
        public ToolAnswer execute(ToolArgs args) {
            LocalDate date = args.from() != null ? args.from() : LocalDate.now(ShiftUtil.VIETNAM_ZONE);
            ShiftUtil.ShiftInfo shift = ShiftUtil.resolve(date, null);
            ShiftChecklistResponse checklist = shiftChecklistService.getChecklist(shift.date(), shift.code());

            StringBuilder md = new StringBuilder("**Checklist công việc: " + shift.name() + " (" + date(shift.date()) + ")**\n\n");
            int percent = checklist.getCompletedPercent() != null ? checklist.getCompletedPercent() : 0;
            md.append("- Tiến độ hoàn thành: **").append(percent).append("%**");
            if (Boolean.TRUE.equals(checklist.getIsCompleted())) {
                md.append(" (Đã hoàn thành toàn bộ)\n\n");
            } else {
                md.append(" (Chưa hoàn thành)\n\n");
            }

            if (checklist.getItems() != null && !checklist.getItems().isEmpty()) {
                md.append("| Trạng thái | Nhiệm vụ | Thời điểm |\n");
                md.append("| --- | --- | --- |\n");
                for (ChecklistItemResponse item : checklist.getItems()) {
                    boolean isChecked = Boolean.TRUE.equals(item.getChecked());
                    String statusIcon = isChecked ? "Đã xong" : "Chưa làm";
                    String timing = "START".equalsIgnoreCase(item.getGroupCode()) ? "Đầu ca" : "Cuối ca";
                    md.append("| ").append(statusIcon).append(" | ").append(item.getLabel()).append(" | ").append(timing).append(" |\n");
                }
            }

            return ToolAnswer.of(md.toString(), "/home/dashboard", "Mở Checklist ca");
        }
    }

    @Component
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class ShiftStaffTool implements ChatTool {

        IShiftChecklistRepository shiftChecklistRepository;
        IInvoiceRepository invoiceRepository;

        @Override
        public String name() {
            return "shift_staff";
        }

        @Override
        public String description() {
            return "Tra cứu danh sách nhân viên đang trực ca hiện tại hoặc ca làm việc cụ thể";
        }

        @Override
        public String permission() {
            return Permissions.AUTHENTICATED;
        }

        @Override
        public List<String> examples() {
            return List.of(
                    "Hiện tại có những ai đang trực?",
                    "Danh sách nhân viên trực ca",
                    "Ai đang trong ca trực hôm nay?"
            );
        }

        @Override
        @PreAuthorize(Permissions.AUTHENTICATED)
        public ToolAnswer execute(ToolArgs args) {
            LocalDate date = args.from() != null ? args.from() : LocalDate.now(ShiftUtil.VIETNAM_ZONE);
            ShiftUtil.ShiftInfo shift = ShiftUtil.resolve(date, null);

            List<ShiftChecklist> checklists = shiftChecklistRepository.findByWorkDateAndShiftCode(shift.date(), shift.code());
            List<String> activeCreators = invoiceRepository.findDistinctCreatedByBetween(shift.startInstant(), shift.endInstant());

            Map<String, StaffStatus> staffMap = new LinkedHashMap<>();

            for (ShiftChecklist sc : checklists) {
                User u = sc.getUser();
                if (u == null) continue;
                String username = u.getUsername();
                Employee emp = u.getEmployee();
                String name = emp != null && emp.getName() != null ? emp.getName() : username;
                String roleOrPos = emp != null && emp.getPosition() != null ? emp.getPosition().getName() : "Nhân viên";
                String status = Boolean.TRUE.equals(sc.getIsCompleted())
                        ? "Đã xong checklist ca"
                        : "Đang trực (Checklist: " + (sc.getCheckedItems() != null ? sc.getCheckedItems().size() : 0) + " mục)";
                staffMap.put(username, new StaffStatus(name, username, roleOrPos, status));
            }

            for (String username : activeCreators) {
                if (username != null && !username.isBlank() && !staffMap.containsKey(username)) {
                    staffMap.put(username, new StaffStatus(username, username, "Bán hàng", "Có phát sinh hóa đơn trong ca"));
                }
            }

            StringBuilder md = new StringBuilder();
            md.append("**Nhân sự trong ca: ").append(shift.name()).append("** (Ngày ").append(date(shift.date()))
                    .append(" | ").append(timeWindowOf(shift.code())).append(")\n\n");

            if (staffMap.isEmpty()) {
                md.append("Hiện tại chưa ghi nhận nhân viên nào hoàn thành checklist ca hoặc phát sinh giao dịch trong ").append(shift.name().toLowerCase()).append(" hôm nay.");
            } else {
                md.append("Hiện có **").append(staffMap.size()).append(" nhân sự** đang trực ca / có hoạt động trong ca:\n\n");
                List<String> headers = List.of("Họ và tên", "Tài khoản", "Vị trí / Vai trò", "Trạng thái ca trực");
                List<List<String>> rows = new ArrayList<>();
                for (StaffStatus s : staffMap.values()) {
                    rows.add(List.of(s.name(), s.username(), s.roleOrPos(), s.status()));
                }
                md.append(table(headers, rows));
            }

            return ToolAnswer.of(md.toString(), "/home/dashboard", "Mở Trang Checklist ca");
        }

        private record StaffStatus(String name, String username, String roleOrPos, String status) {}

        private static String timeWindowOf(String code) {
            return switch (code != null ? code.toUpperCase() : "") {
                case "MORNING" -> "06:00 - 14:00";
                case "AFTERNOON" -> "14:00 - 22:00";
                case "NIGHT" -> "22:00 - 06:00";
                default -> "Trong ca làm việc";
            };
        }
    }

}
