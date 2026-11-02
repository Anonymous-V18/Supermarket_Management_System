package com.anonymous.service.chatbot;

import com.anonymous.entity.ChatMessage;
import com.anonymous.service.chatbot.tool.ChatFormat;
import com.anonymous.service.chatbot.tool.ChatFormat.KpiCard;
import com.anonymous.utils.ShiftUtil;
import com.anonymous.utils.TtlCache;
import com.anonymous.utils.VietnameseTextUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.anonymous.service.chatbot.tool.ChatFormat.*;

/**
 * Zero-Hardcode AI Text-to-SQL Agent (1-Call Architecture optimized for Free API Keys).
 * Converts arbitrary Vietnamese natural-language questions directly into safe, read-only MySQL SELECT queries
 * with strict Role-Based Access Control (RBAC) table whitelisting, automatic self-correction, and rich UI formatting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicSqlAgent {

    private static final int MAX_ROWS = 1000;
    private static final int PREVIEW_ROWS = 10;

    private static final Pattern FORBIDDEN_SQL = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|replace|grant|revoke|call|exec|execute|into\\s+outfile|load_file|sleep|benchmark)\\b"
    );
    private static final Pattern TABLE_REF = Pattern.compile(
            "(?i)\\b(?:from|join)\\s+`?([a-zA-Z0-9_]+)`?"
    );
    private static final Pattern SENSITIVE_COL = Pattern.compile(
            "(?i)\\b(password|invalidated_token)\\b"
    );

    private static final Set<String> ALL_BUSINESS_TABLES = Set.of(
            "product", "product_category", "brand", "unit", "supplier",
            "warehouse", "warehouse_product", "status_product",
            "customer", "employee", "position", "user", "role", "user_role",
            "address", "ward", "district", "city",
            "promotion", "status_invoice",
            "invoice", "invoice_detail",
            "sales_return", "sales_return_detail",
            "stock_in", "stock_in_detail",
            "stock_out", "stock_out_detail",
            "stock_transfer", "stock_transfer_detail",
            "stock_take", "stock_take_detail",
            "shift_checklist", "shift_checklist_item", "checklist_item",
            "audit_log"
    );

    private static final Set<String> ACCOUNTING_TABLES = Set.of(
            "product", "product_category", "brand", "unit", "supplier",
            "warehouse", "warehouse_product", "status_product",
            "customer", "employee", "position",
            "address", "ward", "district", "city",
            "promotion", "status_invoice",
            "invoice", "invoice_detail",
            "sales_return", "sales_return_detail",
            "stock_in", "stock_in_detail",
            "stock_out", "stock_out_detail",
            "stock_transfer", "stock_transfer_detail",
            "stock_take", "stock_take_detail",
            "shift_checklist", "shift_checklist_item", "checklist_item"
    );

    private static final Set<String> STOREKEEPER_TABLES = Set.of(
            "product", "product_category", "brand", "unit", "supplier",
            "warehouse", "warehouse_product", "status_product",
            "status_invoice", "address", "ward", "district", "city",
            "stock_in", "stock_in_detail",
            "stock_out", "stock_out_detail",
            "stock_transfer", "stock_transfer_detail",
            "stock_take", "stock_take_detail",
            "shift_checklist", "shift_checklist_item", "checklist_item"
    );

    private static final Set<String> SALESMAN_TABLES = Set.of(
            "product", "product_category", "brand", "unit",
            "warehouse", "warehouse_product", "status_product",
            "customer", "promotion", "status_invoice",
            "address", "ward", "district", "city",
            "invoice", "invoice_detail",
            "sales_return", "sales_return_detail",
            "shift_checklist", "shift_checklist_item", "checklist_item"
    );

    private static final Set<String> BASIC_TABLES = Set.of(
            "product", "product_category", "brand", "unit",
            "warehouse", "warehouse_product", "status_product",
            "shift_checklist", "shift_checklist_item", "checklist_item"
    );

    private static final String SCHEMA_PROMPT = """
            Bạn là AI Data Analyst & Text-to-SQL Agent cho Hệ thống Quản lý Siêu thị (MySQL 8).
            Hôm nay (giờ Việt Nam): %s.
            Nhiệm vụ: Phân tích câu hỏi tiếng Việt của người dùng và trả về DUY NHẤT 1 JSON object (không giải thích ngoài JSON):
            {
              "mode": "sql" | "docs" | "smalltalk" | "denied",
              "title": "Tiêu đề báo cáo ngắn gọn bằng tiếng Việt",
              "sql": "Câu lệnh SELECT MySQL chuẩn xác (hoặc null nếu mode != sql)",
              "summaryPrefix": "Câu mở đầu ngắn gọn trả lời người dùng",
              "link": "Đường dẫn màn hình liên quan (ví dụ /home/product, /home/invoice, /home/stockin, /home/stockout, /home/stock-transfer, /home/stock-take, /home/sales-return, /home/customer, /home/employee, /home/supplier, /home/warehouse, /home/report)",
              "linkLabel": "Nhãn nút điều hướng (ví dụ: Đến trang Quản lý Xuất kho)"
            }

            QUY TẮC CHỌN MODE:
            1. "docs": Khi người dùng hỏi về quy trình nghiệp vụ, hướng dẫn sử dụng, chính sách, định nghĩa, cách tính, làm thế nào, quy định, quyền hạn lý thuyết.
            2. "smalltalk": Khi chào hỏi (xin chào, hi, hello, cảm ơn, bạn làm được gì).
            3. "sql": MỌI câu hỏi tra cứu số liệu, đếm số lượng, liệt kê danh sách, thống kê, tìm kiếm, so sánh, trung bình, tổng tiền, tồn kho, chứng từ, nhân sự, khách hàng...

            CẤU TRÚC DATABASE MYSQL (CHỈ DÙNG ĐÚNG TÊN BẢNG & CỘT DƯỚI ĐÂY):
            - product (id, code, barcode, name, sale_price, cost_price, min_stock, vat, warranty, product_category_id, unit_id, supplier_id, brand_id, promotion_id, is_deleted)
            - product_category (id, code, name, is_deleted)
            - brand (id, code, name, is_deleted)
            - unit (id, code, name, is_deleted)
            - warehouse (id, name, phone_number, email, more_info, establish_date, address_id, is_deleted)
            - warehouse_product (id, warehouse_id, product_id, available_quantity, status_product_id) -- BẢNG TỒN KHO HIỆN TẠI (KHÔNG có cột is_deleted)
            - supplier (id, name, email, phone_number, more_info, contact_date, address_id, is_deleted)
            - customer (id, name, gender, dob, phone_number, email, accumulated_points, address_id, is_deleted)
            - employee (id, code, name, gender, dob, phone_number, email, user_id, position_id, address_id, is_deleted)
            - position (id, name, is_deleted)
            - promotion (id, name, voucher_code, start_date, end_date, limit_voucher, description, is_deleted)
            - status_invoice (id, code, name) -- code gồm: 'COMPLETED' (Đã hoàn thành), 'PENDING' (Chờ xử lý), 'CANCELLED' (Đã hủy) (KHÔNG có is_deleted)
            - invoice (id, code, invoice_create_date, created_date, discount, total_product, total_price, payment_method, amount_paid, change_amount, status_invoice_id, customer_id, employee_id, warehouse_id, is_deleted) -- Hóa đơn bán hàng (mã HD...)
            - invoice_detail (id, invoice_id, product_id, quantity, input_price, sale_price, discount_percent, line_total) -- Chi tiết hóa đơn (KHÔNG có is_deleted)
            - sales_return (id, code, return_date, created_date, reason, total_product, refund_amount, refund_method, invoice_id, customer_id, warehouse_id, employee_id, status_invoice_id, is_deleted) -- Phiếu trả hàng (mã TH...)
            - sales_return_detail (id, sales_return_id, product_id, quantity, unit_refund, line_total) -- (KHÔNG có is_deleted)
            - stock_in (id, code, stock_in_date, created_date, total_product, total_price, status_invoice_id, supplier_id, warehouse_id, employee_id, is_deleted) -- Phiếu nhập kho (mã PN...)
            - stock_in_detail (id, stock_in_id, product_id, quantity, input_price, sale_price, lot_number, expiry_date) -- (KHÔNG có is_deleted)
            - stock_out (id, code, stock_out_date, created_date, total_product, total_price, reason, status_invoice_id, customer_id, employee_id, warehouse_id, is_deleted) -- Phiếu xuất kho (mã PX...)
            - stock_out_detail (id, stock_out_id, product_id, quantity, input_price, sale_price, total_price) -- (KHÔNG có is_deleted)
            - stock_transfer (id, code, transfer_date, created_date, note, total_product, from_warehouse_id, to_warehouse_id, employee_id, status_invoice_id, is_deleted) -- Phiếu chuyển kho (mã CK...)
            - stock_transfer_detail (id, stock_transfer_id, product_id, quantity) -- (KHÔNG có is_deleted)
            - stock_take (id, code, stock_take_date, created_date, note, total_difference, warehouse_id, employee_id, status_invoice_id, is_deleted) -- Phiếu kiểm kê kho (mã KK...)
            - stock_take_detail (id, stock_take_id, product_id, system_quantity, counted_quantity, difference) -- (KHÔNG có is_deleted)

            QUY TẮC VIẾT SQL BẮT BUỘC:
            1. Luôn đặt alias tiếng Việt dễ hiểu cho các cột SELECT (ví dụ: `SELECT p.code AS 'Mã SP', p.name AS 'Tên sản phẩm', p.sale_price AS 'Giá bán'`).
            2. Với các bảng có cột `is_deleted` (product, invoice, sales_return, stock_in, stock_out, stock_transfer, stock_take, customer, employee, supplier, warehouse, promotion, brand, unit, product_category), LUÔN thêm điều kiện `is_deleted = 0`. Các bảng `_detail`, `warehouse_product`, `status_invoice` KHÔNG có cột `is_deleted`.
            3. Khi người dùng hỏi vừa đếm số lượng vừa xem thông tin (hoặc hỏi "có bao nhiêu phiếu xuất kho/nhập kho/hóa đơn/sản phẩm/nhân viên/khách hàng..."), HÃY VIẾT CÂU SELECT LẤY DANH SÁCH CÁC DÒNG CHI TIẾT (ví dụ: Mã phiếu, Kho, Nhân viên, Tổng tiền, Trạng thái, Ngày lập) sắp xếp mới nhất trước (`ORDER BY ... DESC`) thay vì chỉ `SELECT COUNT(*)` khô khan! Hệ thống Java sẽ tự động đếm tổng số dòng, tự cộng tổng tiền từ danh sách đó và tạo cả Thẻ KPI lẫn Bảng chi tiết + File CSV cho người dùng!
            4. Chỉ dùng `GROUP BY` / `COUNT` / `SUM` khi người dùng yêu cầu gom nhóm thống kê (ví dụ: "theo từng kho", "top sản phẩm bán chạy", "nhà cung cấp nào nhập nhiều nhất", "trung bình mỗi đơn").
            5. Lọc ngày/tháng trên các cột kiểu Instant/Timestamp (`invoice_create_date`, `stock_in_date`, `stock_out_date`, `transfer_date`, `stock_take_date`, `return_date`): dùng `COALESCE(stock_out_date, created_date) >= 'YYYY-MM-DD 00:00:00' AND COALESCE(stock_out_date, created_date) <= 'YYYY-MM-DD 23:59:59'`. Nếu chỉ nói "tháng 10" mà không nói năm, lấy năm hiện tại (%d).
            6. Mỗi câu hỏi mới là ĐỘC LẬP, tuyệt đối không lấy điều kiện của câu trước trừ khi câu hỏi hiện tại có từ "đó", "ấy", "còn...".
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SlmQueryParser slmQueryParser;
    private final ObjectMapper objectMapper;

    // Cache SQL plans for 10 minutes to save Free API Key rate limits on repeated questions
    private final TtlCache<String, SqlPlan> planCache = new TtlCache<>(200, Duration.ofMinutes(10));

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SqlPlan(
            String mode,
            String title,
            String sql,
            String summaryPrefix,
            String link,
            String linkLabel
    ) {
    }

    public record AgentResult(
            String mode,        // "sql", "docs", "smalltalk", "denied"
            String markdown,
            String link,
            String linkLabel
    ) {
    }

    public Optional<AgentResult> tryAnswer(String question, Set<String> roleCodes, List<ChatMessage> recent) {
        if (slmQueryParser == null || !slmQueryParser.isEnabled() || question == null || question.isBlank()) {
            return Optional.empty();
        }
        try {
            LocalDate today = LocalDate.now(ShiftUtil.VIETNAM_ZONE);
            String plainQ = VietnameseTextUtil.stripDiacritics(VietnameseTextUtil.normalize(question));
            boolean isFollowUp = plainQ.startsWith("con ") || plainQ.startsWith("the con ")
                    || plainQ.contains(" do") || plainQ.contains(" ay") || plainQ.contains("thi sao");

            String cacheKey = today + "|" + String.join(",", new TreeSet<>(roleCodes)) + "|" + plainQ;
            SqlPlan plan = (!isFollowUp) ? planCache.get(cacheKey) : null;

            if (plan == null) {
                String systemPrompt = SCHEMA_PROMPT.formatted(today.toString(), today.getYear());
                StringBuilder userPrompt = new StringBuilder();
                if (isFollowUp && recent != null && !recent.isEmpty()) {
                    userPrompt.append("Ngữ cảnh hội thoại gần nhất (chỉ tham khảo khi câu hỏi có từ 'đó', 'ấy', 'còn'):\n");
                    int start = Math.max(0, recent.size() - 2);
                    for (int i = start; i < recent.size(); i++) {
                        ChatMessage m = recent.get(i);
                        String c = m.getContent() != null ? m.getContent() : "";
                        if (c.length() > 400) c = c.substring(0, 400) + "...";
                        userPrompt.append(m.getRole()).append(": ").append(c).append("\n");
                    }
                    userPrompt.append("\n");
                }
                userPrompt.append("Câu hỏi của người dùng: ").append(question);

                Optional<String> rawJson = slmQueryParser.completeJson(systemPrompt, userPrompt.toString());
                if (rawJson.isEmpty()) {
                    return Optional.empty();
                }
                plan = objectMapper.readValue(rawJson.get(), SqlPlan.class);
                if (plan != null && !isFollowUp && "sql".equalsIgnoreCase(plan.mode())) {
                    planCache.put(cacheKey, plan);
                }
            }

            if (plan == null || plan.mode() == null) {
                return Optional.empty();
            }

            String mode = plan.mode().trim().toLowerCase(Locale.ROOT);
            if ("docs".equals(mode) || "smalltalk".equals(mode) || "denied".equals(mode)) {
                return Optional.of(new AgentResult(mode, null, null, null));
            }

            if (!"sql".equals(mode) || plan.sql() == null || plan.sql().isBlank()) {
                return Optional.empty();
            }

            String sql = sanitizeSql(plan.sql());
            Optional<String> rbacError = checkSecurityAndRbac(sql, roleCodes);
            if (rbacError.isPresent()) {
                if ("DENIED".equals(rbacError.get())) {
                    return Optional.of(new AgentResult("denied", null, null, null));
                }
                log.warn("Rejected unsafe SQL from AI: {} ({})", sql, rbacError.get());
                return Optional.empty();
            }

            try {
                AgentResult executed = executeAndFormat(plan, sql);
                return Optional.of(executed);
            } catch (Exception sqlEx) {
                log.info("Initial AI SQL failed ({}), attempting 1-step self-correction...", sqlEx.getMessage());
                planCache.clear();
                Optional<String> fixedSqlOpt = selfCorrectSql(question, sql, sqlEx.getMessage(), today);
                if (fixedSqlOpt.isPresent()) {
                    String fixedSql = sanitizeSql(fixedSqlOpt.get());
                    if (checkSecurityAndRbac(fixedSql, roleCodes).isEmpty()) {
                        return Optional.of(executeAndFormat(plan, fixedSql));
                    }
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            log.debug("DynamicSqlAgent fallback to standard router: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> selfCorrectSql(String question, String badSql, String errorMsg, LocalDate today) {
        String sys = SCHEMA_PROMPT.formatted(today.toString(), today.getYear())
                + "\nCâu SQL trước đó bị lỗi MySQL. Hãy sửa lại câu SQL cho đúng tên bảng/cột và trả về JSON như mẫu.";
        String user = "Câu hỏi: " + question + "\nSQL lỗi: " + badSql + "\nThông báo lỗi MySQL: " + errorMsg;
        Optional<String> raw = slmQueryParser.completeJson(sys, user);
        if (raw.isEmpty()) return Optional.empty();
        try {
            SqlPlan fixed = objectMapper.readValue(raw.get(), SqlPlan.class);
            return (fixed != null && fixed.sql() != null && !fixed.sql().isBlank())
                    ? Optional.of(fixed.sql())
                    : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static String sanitizeSql(String rawSql) {
        String s = rawSql.trim();
        while (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        if (!s.toLowerCase(Locale.ROOT).contains(" limit ")) {
            s = s + " LIMIT " + MAX_ROWS;
        }
        return s;
    }

    static Optional<String> checkSecurityAndRbac(String sql, Set<String> roleCodes) {
        String trimmed = sql.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("select") && !lower.startsWith("with")) {
            return Optional.of("NOT_SELECT");
        }
        if (trimmed.contains(";")) {
            return Optional.of("MULTI_STATEMENT");
        }
        if (FORBIDDEN_SQL.matcher(trimmed).find()) {
            return Optional.of("FORBIDDEN_KEYWORD");
        }
        if (SENSITIVE_COL.matcher(trimmed).find()) {
            return Optional.of("SENSITIVE_COLUMN");
        }

        Set<String> allowedTables = allowedTablesFor(roleCodes);
        Matcher m = TABLE_REF.matcher(trimmed);
        boolean foundTable = false;
        while (m.find()) {
            String table = m.group(1).toLowerCase(Locale.ROOT);
            foundTable = true;
            if (!ALL_BUSINESS_TABLES.contains(table)) {
                return Optional.of("UNKNOWN_TABLE:" + table);
            }
            if (!allowedTables.contains(table)) {
                return Optional.of("DENIED");
            }
        }
        if (!foundTable) {
            return Optional.of("NO_TABLE");
        }
        return Optional.empty();
    }

    private static Set<String> allowedTablesFor(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return BASIC_TABLES;
        }
        if (roleCodes.contains("ADMIN")) {
            return ALL_BUSINESS_TABLES;
        }
        Set<String> union = new HashSet<>(BASIC_TABLES);
        if (roleCodes.contains("ACCOUNTING_STAFF")) {
            union.addAll(ACCOUNTING_TABLES);
        }
        if (roleCodes.contains("STOREKEEPER")) {
            union.addAll(STOREKEEPER_TABLES);
        }
        if (roleCodes.contains("SALESMAN")) {
            union.addAll(SALESMAN_TABLES);
        }
        return union;
    }

    private AgentResult executeAndFormat(SqlPlan plan, String sql) {
        List<String> headers = new ArrayList<>();
        List<List<Object>> rawRows = new ArrayList<>();

        jdbcTemplate.query(con -> {
            var ps = con.prepareStatement(sql);
            ps.setMaxRows(MAX_ROWS);
            ps.setQueryTimeout(5);
            return ps;
        }, rs -> {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            if (headers.isEmpty()) {
                for (int i = 1; i <= colCount; i++) {
                    headers.add(meta.getColumnLabel(i));
                }
            }
            do {
                List<Object> row = new ArrayList<>(colCount);
                for (int i = 1; i <= colCount; i++) {
                    row.add(rs.getObject(i));
                }
                rawRows.add(row);
            } while (rs.next());
        });

        String title = (plan.title() != null && !plan.title().isBlank()) ? plan.title().trim() : "Kết quả tra cứu dữ liệu";
        StringBuilder md = new StringBuilder();
        md.append("**").append(title).append("**\n\n");

        if (rawRows.isEmpty()) {
            md.append("Không có dữ liệu nào khớp với điều kiện tra cứu.");
            return new AgentResult("sql", md.toString(), plan.link(), plan.linkLabel());
        }

        // Convert raw values to formatted strings and compute automatic KPIs
        List<List<String>> formattedRows = new ArrayList<>(rawRows.size());
        BigDecimal totalMoney = BigDecimal.ZERO;
        String moneyColName = null;
        long totalQty = 0;
        String qtyColName = null;

        for (List<Object> rawRow : rawRows) {
            List<String> rowStr = new ArrayList<>(headers.size());
            for (int c = 0; c < headers.size(); c++) {
                String colName = headers.get(c);
                Object val = rawRow.get(c);
                rowStr.add(formatCell(colName, val));

                if (val instanceof Number num) {
                    if (isMoneyColumn(colName)) {
                        if (moneyColName == null || moneyColName.equals(colName)) {
                            moneyColName = colName;
                            totalMoney = totalMoney.add(new BigDecimal(num.toString()));
                        }
                    } else if (isQuantityColumn(colName)) {
                        if (qtyColName == null || qtyColName.equals(colName)) {
                            qtyColName = colName;
                            totalQty += num.longValue();
                        }
                    }
                }
            }
            formattedRows.add(rowStr);
        }

        // Single aggregate row (e.g. SELECT COUNT(*), SUM(...))
        if (formattedRows.size() == 1 && headers.size() <= 4) {
            List<String> singleRow = formattedRows.getFirst();
            List<KpiCard> kpis = new ArrayList<>();
            for (int c = 0; c < headers.size(); c++) {
                kpis.add(new KpiCard(headers.get(c), singleRow.get(c), title, c == 0 ? "primary" : "success"));
            }
            md.append(kpiCards(kpis)).append("\n");
            if (plan.summaryPrefix() != null && !plan.summaryPrefix().isBlank()) {
                md.append(plan.summaryPrefix().trim()).append(" ");
            }
            List<String> summaryParts = new ArrayList<>();
            for (int c = 0; c < headers.size(); c++) {
                summaryParts.add(headers.get(c) + ": **" + singleRow.get(c) + "**");
            }
            md.append(String.join(" · ", summaryParts)).append(".\n\n");
            md.append(table(headers, formattedRows));
            return new AgentResult("sql", md.toString(), plan.link(), plan.linkLabel());
        }

        // Multi-row dataset: build KPI summary cards automatically
        List<KpiCard> kpis = new ArrayList<>();
        kpis.add(new KpiCard("Tổng số bản ghi", number(formattedRows.size()) + " dòng", "Kết quả khớp điều kiện", "primary"));
        if (moneyColName != null && totalMoney.compareTo(BigDecimal.ZERO) > 0) {
            kpis.add(new KpiCard("Tổng " + moneyColName.toLowerCase(Locale.ROOT), money(totalMoney), "Cộng gộp trên " + formattedRows.size() + " dòng", "success"));
        }
        if (qtyColName != null && totalQty > 0) {
            kpis.add(new KpiCard("Tổng " + qtyColName.toLowerCase(Locale.ROOT), number(totalQty), "Tổng số lượng", "primary"));
        }
        md.append(kpiCards(kpis)).append("\n");

        if (plan.summaryPrefix() != null && !plan.summaryPrefix().isBlank()) {
            md.append(plan.summaryPrefix().trim()).append(" ");
        }
        md.append("Tìm thấy tổng cộng **").append(number(formattedRows.size())).append(" bản ghi**");
        if (moneyColName != null && totalMoney.compareTo(BigDecimal.ZERO) > 0) {
            md.append(" (tổng ").append(moneyColName.toLowerCase(Locale.ROOT)).append(": **").append(money(totalMoney)).append("**)");
        }
        md.append(".\n\n");

        // Auto-render visual bar chart when SQL is a GROUP BY aggregation (2..12 rows) with a numeric metric
        if (sql.toLowerCase(Locale.ROOT).contains("group by") && rawRows.size() >= 2 && rawRows.size() <= 12 && headers.size() >= 2) {
            int numColIdx = -1;
            for (int c = 1; c < headers.size(); c++) {
                if (rawRows.getFirst().get(c) instanceof Number) {
                    numColIdx = c;
                    break;
                }
            }
            if (numColIdx > 0) {
                final int metricIdx = numColIdx;
                long maxVal = rawRows.stream()
                        .map(r -> r.get(metricIdx) instanceof Number n ? n.longValue() : 0L)
                        .max(Long::compareTo)
                        .orElse(1L);
                if (maxVal > 0) {
                    List<ChartBar> bars = new ArrayList<>();
                    for (int r = 0; r < rawRows.size(); r++) {
                        String label = formattedRows.get(r).getFirst();
                        long val = rawRows.get(r).get(metricIdx) instanceof Number n ? n.longValue() : 0L;
                        String detail = formattedRows.get(r).get(metricIdx);
                        int pct = (int) Math.round((val * 100.0) / maxVal);
                        bars.add(new ChartBar(label, val, detail, Math.max(pct, 5)));
                    }
                    md.append(chart("Biểu đồ: " + title, bars)).append("\n\n");
                }
            }
        }

        if (formattedRows.size() <= PREVIEW_ROWS) {
            md.append(table(headers, formattedRows));
        } else {
            md.append(table(headers, formattedRows.subList(0, PREVIEW_ROWS)));
            md.append("\n").append(collapsible(
                    "Nhấn để xem toàn bộ danh sách " + formattedRows.size() + " dòng (mở rộng)",
                    table(headers, formattedRows)
            ));
        }
        md.append(downloadCsvLink("Tải dữ liệu báo cáo (.CSV)", "bao-cao-du-lieu.csv", headers, formattedRows));
        return new AgentResult("sql", md.toString(), plan.link(), plan.linkLabel());
    }

    private static boolean isMoneyColumn(String colName) {
        if (colName == null) return false;
        String plain = VietnameseTextUtil.stripDiacritics(colName.toLowerCase(Locale.ROOT));
        return plain.contains("tien") || plain.contains("gia") || plain.contains("doanh thu")
                || plain.contains("loi nhuan") || plain.contains("hoan") || plain.contains("price")
                || plain.contains("revenue") || plain.contains("refund") || plain.contains("amount")
                || plain.contains("line_total") || plain.contains("total_price");
    }

    private static boolean isQuantityColumn(String colName) {
        if (colName == null) return false;
        String plain = VietnameseTextUtil.stripDiacritics(colName.toLowerCase(Locale.ROOT));
        return (plain.contains("so luong") || plain.equals("sl") || plain.contains("sl ")
                || plain.contains("ton") || plain.contains("quantity") || plain.contains("total_product"))
                && !plain.contains("gia") && !plain.contains("tien");
    }

    private static String formatCell(String colName, Object val) {
        if (val == null) {
            return "—";
        }
        if (val instanceof String s) {
            if ("COMPLETED".equalsIgnoreCase(s)) return "Đã hoàn thành";
            if ("PENDING".equalsIgnoreCase(s)) return "Chờ xử lý";
            if ("CANCELLED".equalsIgnoreCase(s)) return "Đã hủy";
            if ("CASH".equalsIgnoreCase(s)) return "Tiền mặt";
            if ("TRANSFER".equalsIgnoreCase(s)) return "Chuyển khoản";
            if ("CARD".equalsIgnoreCase(s)) return "Thẻ";
            return ChatFormat.cell(s);
        }
        if (val instanceof Timestamp ts) {
            return ChatFormat.dateTime(ts.toInstant());
        }
        if (val instanceof LocalDateTime ldt) {
            return ChatFormat.dateTime(ldt.atZone(ShiftUtil.VIETNAM_ZONE).toInstant());
        }
        if (val instanceof Instant inst) {
            return ChatFormat.dateTime(inst);
        }
        if (val instanceof java.sql.Date sqlDate) {
            return ChatFormat.date(sqlDate.toLocalDate());
        }
        if (val instanceof LocalDate ld) {
            return ChatFormat.date(ld);
        }
        if (val instanceof BigDecimal bd) {
            if (isMoneyColumn(colName)) {
                return ChatFormat.money(bd);
            }
            return ChatFormat.number(bd.longValue());
        }
        if (val instanceof Number num) {
            if (isMoneyColumn(colName)) {
                return ChatFormat.money(BigDecimal.valueOf(num.doubleValue()));
            }
            return ChatFormat.number(num.longValue());
        }
        if (val instanceof Boolean b) {
            return b ? "Có" : "Không";
        }
        return ChatFormat.cell(val.toString());
    }
}
