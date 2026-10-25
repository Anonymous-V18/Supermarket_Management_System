package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.DashboardSummaryResponse;
import com.anonymous.service.IDashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

// The home-page shift figures. IDashboardService already limits staff to their own invoices
// (admin sees the whole store), so the chatbot inherits that scoping.
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MySummaryTool implements ChatTool {

    IDashboardService dashboardService;

    @Override
    public String name() {
        return "my_shift_summary";
    }

    @Override
    public String description() {
        return "Số liệu bán hàng của ca làm việc (số hóa đơn, doanh thu, số sản phẩm đã bán) của chính người hỏi; Admin xem toàn cửa hàng";
    }

    @Override
    public String permission() {
        return Permissions.AUTHENTICATED;
    }

    @Override
    public List<String> examples() {
        return List.of("Ca này tôi bán được bao nhiêu?", "Hôm nay tôi đã lập bao nhiêu hóa đơn?");
    }

    @Override
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ToolAnswer execute(ToolArgs args) {
        DashboardSummaryResponse summary = dashboardService.getSummary(args.from(), null);
        String scope = "STORE".equals(summary.getScope()) ? "toàn cửa hàng" : "của bạn";
        String md = "**" + summary.getShiftName() + " ngày " + date(summary.getWorkDate()) + "** (" + scope + ", "
                + dateTime(summary.getShiftStart()) + " – " + dateTime(summary.getShiftEnd()) + ")\n\n"
                + table(List.of("Chỉ tiêu", "Giá trị"), List.of(
                List.of("Số hóa đơn (trừ đã hủy)", number(summary.getInvoiceCount())),
                List.of("Doanh thu", money(summary.getRevenue())),
                List.of("Số sản phẩm đã bán", number(summary.getProductsSold())),
                List.of("Hóa đơn hoàn thành / đang chờ / đã hủy", number(summary.getCompletedInvoiceCount()) + " / "
                        + number(summary.getPendingInvoiceCount()) + " / " + number(summary.getCancelledInvoiceCount()))));
        return ToolAnswer.of(md, "/home/dashboard", "Mở Trang chính");
    }

}
