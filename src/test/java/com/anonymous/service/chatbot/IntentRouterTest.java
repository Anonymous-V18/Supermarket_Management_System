package com.anonymous.service.chatbot;

import com.anonymous.constant.Permissions;
import com.anonymous.service.IWarehouseService;
import com.anonymous.service.chatbot.IntentRouter.Kind;
import com.anonymous.service.chatbot.IntentRouter.Route;
import com.anonymous.service.chatbot.tool.ChatTool;
import com.anonymous.service.chatbot.tool.ToolAnswer;
import com.anonymous.service.chatbot.tool.ToolArgs;
import com.anonymous.service.chatbot.tool.ToolRegistry;
import com.anonymous.utils.ShiftUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntentRouterTest {

    private record StubTool(String name, String permission) implements ChatTool {

        @Override
        public String description() {
            return name;
        }

        @Override
        public List<String> examples() {
            return List.of(name);
        }

        @Override
        public ToolAnswer execute(ToolArgs args) {
            return ToolAnswer.of(name, null, null);
        }

    }

    private static final Set<String> SALESMAN = Set.of("SALESMAN");
    private static final Set<String> ACCOUNTING = Set.of("ACCOUNTING_STAFF");
    private static final Set<String> STOREKEEPER = Set.of("STOREKEEPER");

    private final IntentRouter router;

    IntentRouterTest() {
        ToolRegistry registry = new ToolRegistry(List.of(
                new StubTool("revenue", Permissions.REPORT),
                new StubTool("top_products", Permissions.REPORT),
                new StubTool("sales_by_employee", Permissions.REPORT),
                new StubTool("inventory", Permissions.STOCK_READ),
                new StubTool("expiring_lots", Permissions.STOCK_READ),
                new StubTool("product_lookup", Permissions.AUTHENTICATED),
                new StubTool("invoice_lookup", Permissions.SALES_READ),
                new StubTool("sales_return_lookup", Permissions.SALES_READ),
                new StubTool("stock_document_lookup", Permissions.STOCK_READ),
                new StubTool("my_shift_summary", Permissions.AUTHENTICATED),
                new StubTool("invoice_stats", Permissions.SALES_READ),
                new StubTool("data_overview", Permissions.AUTHENTICATED),
                new StubTool("customer_lookup", Permissions.SALES_READ),
                new StubTool("sales_return_search", Permissions.SALES_READ),
                new StubTool("invoice_search", Permissions.SALES_READ),
                new StubTool("supplier_lookup", Permissions.STOCK_READ),
                new StubTool("employee_lookup", Permissions.ADMIN),
                new StubTool("warehouse_lookup", Permissions.STOCK_READ),
                new StubTool("promotion_lookup", Permissions.SALES_READ),
                new StubTool("shift_staff", Permissions.AUTHENTICATED),
                new StubTool("shift_checklist", Permissions.AUTHENTICATED),
                new StubTool("current_user", Permissions.AUTHENTICATED)));
        IWarehouseService warehouseService = mock(IWarehouseService.class);
        when(warehouseService.getAllToSelection()).thenReturn(List.of());
        router = new IntentRouter(registry, new WarehouseResolver(warehouseService));
    }

    private Route route(String question, Set<String> roles) {
        return router.route(question, roles);
    }

    @Test
    void dataQuestionOutsideTheRolesIsDenied() {
        Route route = route("Doanh thu tháng này là bao nhiêu?", SALESMAN);
        assertThat(route.kind()).isEqualTo(Kind.DENIED);
        assertThat(route.deniedTool()).isEqualTo("revenue");
        assertThat(route("Phiếu nhập PN000101 nhập những gì?", SALESMAN).kind()).isEqualTo(Kind.DENIED);
        assertThat(route("Tồn kho hiện tại", Set.of("EMPLOYEE")).kind()).isEqualTo(Kind.DENIED);
    }

    @Test
    void revenueGetsTheMonthRange() {
        Route route = route("Doanh thu tháng này", ACCOUNTING);
        LocalDate today = LocalDate.now(ShiftUtil.VIETNAM_ZONE);
        assertThat(route.kind()).isEqualTo(Kind.TOOL);
        assertThat(route.tool().name()).isEqualTo("revenue");
        assertThat(route.args().from()).isEqualTo(today.withDayOfMonth(1));
        assertThat(route.args().to()).isEqualTo(today);
        assertThat(route("Lợi nhuận gộp tuần trước", ACCOUNTING).tool().name()).isEqualTo("revenue");
    }

    @Test
    void documentCodesArePaddedAndRoutedByPrefix() {
        Route invoice = route("Hóa đơn HD101 gồm những gì?", SALESMAN);
        assertThat(invoice.tool().name()).isEqualTo("invoice_lookup");
        assertThat(invoice.args().keyword()).isEqualTo("HD000101");
        assertThat(route("phieu chuyen kho ck000003 da nhan chua", STOREKEEPER).tool().name())
                .isEqualTo("stock_document_lookup");
    }

    @Test
    void wordsAreMatchedWhole() {
        // "hết hàng" must not be read as "hết hạn"
        Route lowStock = route("Những mặt hàng nào sắp hết hàng?", STOREKEEPER);
        assertThat(lowStock.tool().name()).isEqualTo("inventory");
        assertThat(lowStock.args().lowStockOnly()).isTrue();
        assertThat(lowStock.args().keyword()).isNull();
        assertThat(route("Hàng nào sắp hết hạn trong 7 ngày tới?", STOREKEEPER).args().withinDays()).isEqualTo(7);
        // "tối đa" is not "tôi"
        assertThat(route("Giảm giá hóa đơn tối đa bao nhiêu?", Set.of("ADMIN")).kind()).isEqualTo(Kind.DOCS);
    }

    @Test
    void howToQuestionsGoToTheDocuments() {
        assertThat(route("Cách tính tiền hoàn khi khách trả hàng?", SALESMAN).kind()).isEqualTo(Kind.DOCS);
        assertThat(route("Doanh thu thuần được tính thế nào?", ACCOUNTING).kind()).isEqualTo(Kind.DOCS);
        assertThat(route("Ca sáng bắt đầu lúc mấy giờ?", Set.of("EMPLOYEE")).kind()).isEqualTo(Kind.DOCS);
    }

    @Test
    void productNameIsWhatRemainsOfTheQuestion() {
        Route route = route("Giá sữa chua Vinamilk bao nhiêu?", SALESMAN);
        assertThat(route.tool().name()).isEqualTo("product_lookup");
        assertThat(route.args().keyword()).isEqualTo("sữa chua vinamilk");
    }

    @Test
    void cashierAskingStockGetsTheProductLookup() {
        Route route = route("Tồn kho sữa chua", SALESMAN);
        assertThat(route.kind()).isEqualTo(Kind.TOOL);
        assertThat(route.tool().name()).isEqualTo("product_lookup");
    }

    @Test
    void ownSalesAndGreetings() {
        assertThat(route("Ca này tôi bán được bao nhiêu?", SALESMAN).tool().name()).isEqualTo("my_shift_summary");
        assertThat(route("xin chào", SALESMAN).kind()).isEqualTo(Kind.SMALLTALK);
    }

    @Test
    void followUpKeepsThePreviousTool() {
        Route route = router.route("còn tháng trước?", ACCOUNTING, "revenue");
        assertThat(route.kind()).isEqualTo(Kind.TOOL);
        assertThat(route.tool().name()).isEqualTo("revenue");
        assertThat(route.args().from()).isNotNull();
    }

    @Test
    void orderCountOnADateIsAnInvoiceCount() {
        Route route = route("Ngày 22/10/2026 có bao nhiêu đơn hàng ?", Set.of("ADMIN"));
        assertThat(route.kind()).isEqualTo(Kind.TOOL);
        assertThat(route.tool().name()).isEqualTo("invoice_stats");
        assertThat(route.args().from()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(route.args().to()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(route("hom nay co may hoa don", SALESMAN).tool().name()).isEqualTo("invoice_stats");
        assertThat(route("Hôm nay có bao nhiêu đơn hàng?", STOREKEEPER).kind()).isEqualTo(Kind.DENIED);
        // Own invoices stay with the shift summary, how-to questions with the documents
        assertThat(route("Hôm nay tôi lập được bao nhiêu hóa đơn?", SALESMAN).tool().name()).isEqualTo("my_shift_summary");
        assertThat(route("Cách hủy đơn hàng như thế nào?", SALESMAN).kind()).isEqualTo(Kind.DOCS);
    }

    @Test
    void statisticsWithoutASubjectGetTheOverview() {
        assertThat(route("Thống kê dữ liệu giống tôi", Set.of("ADMIN")).tool().name()).isEqualTo("data_overview");
        assertThat(route("Thống kê dữ liệu giúp tôi", STOREKEEPER).tool().name()).isEqualTo("data_overview");
        Route week = route("Tổng quan tuần này", ACCOUNTING);
        assertThat(week.tool().name()).isEqualTo("data_overview");
        assertThat(week.args().from()).isNotNull();
        // A named report still wins
        assertThat(route("Thống kê doanh thu tháng này", ACCOUNTING).tool().name()).isEqualTo("revenue");
    }

    @Test
    void customerAndPartnerAndOperationsTools() {
        Route returnRoute = route("Có ai tên Mai từng trả hàng không ?", SALESMAN);
        assertThat(returnRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(returnRoute.tool().name()).isEqualTo("sales_return_search");
        assertThat(returnRoute.args().keyword()).isEqualTo("Mai");

        Route custRoute = route("Có khách hàng tên Đặng Thanh Mai không ?", SALESMAN);
        assertThat(custRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(custRoute.tool().name()).isEqualTo("customer_lookup");
        assertThat(custRoute.args().keyword()).isEqualTo("Đặng Thanh Mai");

        Route suppRoute = route("Tìm nhà cung cấp Vinamilk", STOREKEEPER);
        assertThat(suppRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(suppRoute.tool().name()).isEqualTo("supplier_lookup");
        assertThat(suppRoute.args().keyword()).isEqualTo("Vinamilk");

        Route empRoute = route("Tìm nhân viên Nguyễn Văn A", Set.of("ADMIN"));
        assertThat(empRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(empRoute.tool().name()).isEqualTo("employee_lookup");
        assertThat(empRoute.args().keyword()).isEqualTo("Nguyễn Văn A");

        Route whRoute = route("Có những kho nào?", STOREKEEPER);
        assertThat(whRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(whRoute.tool().name()).isEqualTo("warehouse_lookup");

        Route promoRoute = route("Có chương trình khuyến mãi nào không?", SALESMAN);
        assertThat(promoRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(promoRoute.tool().name()).isEqualTo("promotion_lookup");

        Route checkRoute = route("Checklist ca sáng hôm nay", SALESMAN);
        assertThat(checkRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(checkRoute.tool().name()).isEqualTo("shift_checklist");
    }

    @Test
    void multiTurnConversationContextAndPronounResolution() {
        ConversationContext context = ConversationContext.builder()
                .previousIntent("customer_lookup")
                .lastPersonName("Đặng Thanh Mai")
                .build();

        // Pronoun question resolves to last person name
        Route route1 = router.route("Khách hàng đó có trả hàng lần nào chưa ?", SALESMAN, context);
        assertThat(route1.kind()).isEqualTo(Kind.TOOL);
        assertThat(route1.tool().name()).isEqualTo("sales_return_search");
        assertThat(route1.args().keyword()).isEqualTo("Đặng Thanh Mai");

        // Question without explicit person name inherits last person name
        Route route2 = router.route("Có trả hàng lần nào không?", SALESMAN, context);
        assertThat(route2.kind()).isEqualTo(Kind.TOOL);
        assertThat(route2.tool().name()).isEqualTo("sales_return_search");
        assertThat(route2.args().keyword()).isEqualTo("Đặng Thanh Mai");

        // Elliptical follow-up inherits previous intent
        ConversationContext returnContext = ConversationContext.builder()
                .previousIntent("sales_return_search")
                .lastPersonName("Đặng Thanh Mai")
                .build();
        Route route3 = router.route("Còn Nguyễn Văn B thì sao?", SALESMAN, returnContext);
        assertThat(route3.kind()).isEqualTo(Kind.TOOL);
        assertThat(route3.tool().name()).isEqualTo("sales_return_search");
        assertThat(route3.args().keyword()).isEqualTo("Nguyễn Văn B");
    }

    @Test
    void productSalesVolumeAndChartRouting() {
        // "Vẽ biểu đồ thống kê lượng mặt hàng được bán ra vào ngày 21/04/2026"
        Route chartRoute = route("Vẽ biểu đồ thống kê lượng mặt hàng được bán ra vào ngày 21/04/2026", ACCOUNTING);
        assertThat(chartRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(chartRoute.tool().name()).isEqualTo("top_products");
        assertThat(chartRoute.args().chart()).isTrue();
        assertThat(chartRoute.args().from()).isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(chartRoute.args().to()).isEqualTo(LocalDate.of(2026, 4, 21));

        // "Vẽ biểu đồ doanh thu tuần này"
        Route revChart = route("Vẽ biểu đồ doanh thu tuần này", ACCOUNTING);
        assertThat(revChart.kind()).isEqualTo(Kind.TOOL);
        assertThat(revChart.tool().name()).isEqualTo("revenue");
        assertThat(revChart.args().chart()).isTrue();
    }

    @Test
    void dateBasedInvoiceQueriesDoNotConfuseWithPersonContext() {
        ConversationContext contextWithLastPerson = ConversationContext.builder()
                .previousIntent("customer_lookup")
                .lastPersonName("Tra số liệu thật")
                .build();

        // "Ngày 22/10/2026 có hóa đơn nào ?"
        Route routeDate = router.route("Ngày 22/10/2026 có hóa đơn nào ?", SALESMAN, contextWithLastPerson);
        assertThat(routeDate.kind()).isEqualTo(Kind.TOOL);
        assertThat(routeDate.tool().name()).isEqualTo("invoice_stats");
        assertThat(routeDate.args().from()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(routeDate.args().to()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(routeDate.args().keyword()).isNull();

        // "Hôm nay có hóa đơn nào không ?"
        Route routeToday = router.route("Hôm nay có hóa đơn nào không ?", SALESMAN, contextWithLastPerson);
        assertThat(routeToday.kind()).isEqualTo(Kind.TOOL);
        assertThat(routeToday.tool().name()).isEqualTo("invoice_stats");
        assertThat(routeToday.args().from()).isEqualTo(LocalDate.now());
        assertThat(routeToday.args().to()).isEqualTo(LocalDate.now());

        // Explicit person pronoun DOES route to invoice_search with person keyword
        Route routePronoun = router.route("Khách hàng đó có hóa đơn nào không?", SALESMAN, contextWithLastPerson);
        assertThat(routePronoun.kind()).isEqualTo(Kind.TOOL);
        assertThat(routePronoun.tool().name()).isEqualTo("invoice_search");
        assertThat(routePronoun.args().keyword()).isEqualTo("Tra số liệu thật");
    }

    @Test
    void shiftStaffAndPermissionQuestionsRouting() {
        // "Hiện tại có những ai đang trực" -> shift_staff
        Route shiftRoute = route("Hiện tại có những ai đang trực", SALESMAN);
        assertThat(shiftRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(shiftRoute.tool().name()).isEqualTo("shift_staff");

        // "Ai đang trực ca" -> shift_staff
        Route dutyRoute = route("Ai đang trực ca", SALESMAN);
        assertThat(dutyRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(dutyRoute.tool().name()).isEqualTo("shift_staff");

        // "Nhân viên có những quyền nào ?" -> DOCS (not employee_lookup with keyword "có những quyền")
        Route permRoute = route("Nhân viên có những quyền nào ?", SALESMAN);
        assertThat(permRoute.kind()).isEqualTo(Kind.DOCS);

        // "Các vai trò trong hệ thống" -> DOCS
        Route roleRoute = route("Các vai trò trong hệ thống", SALESMAN);
        assertThat(roleRoute.kind()).isEqualTo(Kind.DOCS);

        // "Tìm nhân viên Nguyễn Văn A" -> employee_lookup
        Route empRoute = route("Tìm nhân viên Nguyễn Văn A", Set.of("ADMIN"));
        assertThat(empRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(empRoute.tool().name()).isEqualTo("employee_lookup");
        assertThat(empRoute.args().keyword()).isEqualTo("Nguyễn Văn A");
    }

    @Test
    void compoundEmployeeLookupAndFollowUp() {
        // Compound question: "Nhân viên tên Thảo và có họ Đỗ"
        Route compoundRoute = route("Nhân viên tên Thảo và có họ Đỗ", Set.of("ADMIN"));
        assertThat(compoundRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(compoundRoute.tool().name()).isEqualTo("employee_lookup");
        assertThat(compoundRoute.args().keyword()).isEqualTo("Đỗ Thảo");

        // Compound question variation: "Nhân viên họ Đỗ và tên Thảo"
        Route compoundRoute2 = route("Nhân viên họ Đỗ và tên Thảo", Set.of("ADMIN"));
        assertThat(compoundRoute2.kind()).isEqualTo(Kind.TOOL);
        assertThat(compoundRoute2.tool().name()).isEqualTo("employee_lookup");
        assertThat(compoundRoute2.args().keyword()).isEqualTo("Đỗ Thảo");

        // Turn 1: "Có nhân viên tên Thảo không ?" -> keyword "Thảo"
        Route turn1 = route("Có nhân viên tên Thảo không ?", Set.of("ADMIN"));
        assertThat(turn1.kind()).isEqualTo(Kind.TOOL);
        assertThat(turn1.tool().name()).isEqualTo("employee_lookup");
        assertThat(turn1.args().keyword()).isEqualTo("Thảo");

        // Turn 2 follow-up: "Họ Đỗ" with context of previous employee search
        ConversationContext context = ConversationContext.builder()
                .previousIntent("employee_lookup")
                .lastPersonName("Thảo")
                .build();
        Route turn2 = router.route("Họ Đỗ", Set.of("ADMIN"), context);
        assertThat(turn2.kind()).isEqualTo(Kind.TOOL);
        assertThat(turn2.tool().name()).isEqualTo("employee_lookup");
        assertThat(turn2.args().keyword()).isEqualTo("Đỗ Thảo");
    }

    @Test
    void revenueByWarehouseAndInvoiceWithDateAndPerson() {
        // "vẽ biểu đồ doanh thu ngày 21/10/2026 theo kho ghi rõ tên từng kho"
        Route whChart = route("vẽ biểu đồ doanh thu ngày 21/10/2026 theo kho ghi rõ tên từng kho", ACCOUNTING);
        assertThat(whChart.kind()).isEqualTo(Kind.TOOL);
        assertThat(whChart.tool().name()).isEqualTo("revenue");
        assertThat(whChart.args().chart()).isTrue();
        assertThat(whChart.args().byWarehouse()).isTrue();
        assertThat(whChart.args().from()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(whChart.args().to()).isEqualTo(LocalDate.of(2026, 10, 21));

        // "vẽ biểu đồ doanh thu ngày 22/10/2026 phân riêng từng kho"
        Route whChart2 = route("vẽ biểu đồ doanh thu ngày 22/10/2026 phân riêng từng kho", ACCOUNTING);
        assertThat(whChart2.kind()).isEqualTo(Kind.TOOL);
        assertThat(whChart2.tool().name()).isEqualTo("revenue");
        assertThat(whChart2.args().chart()).isTrue();
        assertThat(whChart2.args().byWarehouse()).isTrue();

        // Generalized variations without hardcoded strings:
        // "so sánh doanh thu giữa các kho ngày 21/10/2026"
        Route whCompare = route("so sánh doanh thu giữa các kho ngày 21/10/2026", ACCOUNTING);
        assertThat(whCompare.args().byWarehouse()).isTrue();

        // "doanh thu ngày 21/10/2026 ở mỗi chi nhánh"
        Route whBranch = route("doanh thu ngày 21/10/2026 ở mỗi chi nhánh", ACCOUNTING);
        assertThat(whBranch.args().byWarehouse()).isTrue();

        // "kho nào bán nhiều nhất ngày 21/10/2026"
        Route whWhich = route("kho nào bán nhiều nhất ngày 21/10/2026", ACCOUNTING);
        assertThat(whWhich.args().byWarehouse()).isTrue();

        // "rồi ngày 22/10/2026 chị Thảo có đơn hàng nào không ?"
        Route invRoute = route("rồi ngày 22/10/2026 chị Thảo có đơn hàng nào không ?", SALESMAN);
        assertThat(invRoute.kind()).isEqualTo(Kind.TOOL);
        assertThat(invRoute.tool().name()).isEqualTo("invoice_search");
        assertThat(invRoute.args().keyword()).isEqualTo("Thảo");
        assertThat(invRoute.args().from()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(invRoute.args().to()).isEqualTo(LocalDate.of(2026, 10, 22));
    }

    @Test
    void currentUserQuestionsRouteToCurrentUser() {
        Route r1 = route("Hiện tại ai đang đăng nhập vậy", SALESMAN);
        assertThat(r1.kind()).isEqualTo(Kind.TOOL);
        assertThat(r1.tool().name()).isEqualTo("current_user");

        Route r2 = route("Tôi là ai", SALESMAN);
        assertThat(r2.kind()).isEqualTo(Kind.TOOL);
        assertThat(r2.tool().name()).isEqualTo("current_user");

        Route r3 = route("Thông tin tài khoản của tôi", SALESMAN);
        assertThat(r3.kind()).isEqualTo(Kind.TOOL);
        assertThat(r3.tool().name()).isEqualTo("current_user");
    }

    @Test
    void warehouseInventoryComparisonRoutesToInventoryByWarehouse() {
        Route route = route("Kho nào đang có nhiều sản phẩm nhất", STOREKEEPER);
        assertThat(route.kind()).isEqualTo(Kind.TOOL);
        assertThat(route.tool().name()).isEqualTo("inventory");
        assertThat(route.args().byWarehouse()).isTrue();

        Route route2 = route("Kho nào có nhiều hàng tồn nhất", STOREKEEPER);
        assertThat(route2.kind()).isEqualTo(Kind.TOOL);
        assertThat(route2.tool().name()).isEqualTo("inventory");
        assertThat(route2.args().byWarehouse()).isTrue();
    }

}
