package com.anonymous.service.chatbot;

import com.anonymous.configuration.ChatbotProperties;
import com.anonymous.service.chatbot.tool.ToolArgs;
import com.anonymous.utils.VietnameseTextUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

// Zero-hardcode Natural Language Query Parser powered by a local Small Language Model (SLM).
// Translates natural Vietnamese user questions into structured JSON tool calls without regex or hardcoding.
// When the local SLM is disabled or unreachable, the system gracefully falls back to the deterministic rule router.
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SlmQueryParser {

    private static final String SYSTEM_PROMPT = """
            Bạn là bộ phân tích ý định (Query Parser) cho hệ thống siêu thị.
            Nhiệm vụ: Phân tích câu hỏi của người dùng và xuất ra DUY NHẤT một đối tượng JSON (không kèm giải thích).

            Hôm nay là: %s.

            Các tool có thể gọi:
            - "current_user": Xem thông tin tài khoản, hồ sơ của người đang đăng nhập (tôi là ai, ai đang đăng nhập, thông tin của tôi, quyền của tôi).
            - "revenue": Báo cáo doanh thu, hoàn trả, lợi nhuận gộp theo khoảng ngày và kho (byWarehouse=true khi hỏi theo kho).
            - "top_products": Mặt hàng/sản phẩm bán chạy nhất, doanh số bán cao nhất theo khoảng ngày (from, to, limit, chart).
            - "sales_by_employee": Doanh số bán hàng của từng nhân viên, nhân viên bán được bao nhiêu tiền (from, to, chart).
            - "inventory": Báo cáo tồn kho sản phẩm, số lượng tồn kho theo từng kho hoặc sản phẩm có tồn kho nhiều nhất/ít nhất, danh sách hàng sắp hết hàng (keyword, byWarehouse, lowStockOnly).
            - "expiring_lots": Hàng sắp hết hạn sử dụng, cận date, quá hạn (keyword, withinDays).
            - "product_lookup": Tra cứu giá bán, thông tin sản phẩm cụ thể theo tên hoặc mã (keyword).
            - "invoice_lookup": Tra cứu chi tiết một mã hóa đơn cụ thể như HD000001 (keyword).
            - "invoice_search": Tra cứu danh sách đơn hàng/hóa đơn theo ngày hoặc người (keyword, from, to).
            - "invoice_stats": Thống kê/đếm số lượng hóa đơn theo ngày tháng (from, to).
            - "employee_lookup": Tra cứu danh sách/hồ sơ nhân viên (chức vụ, liên hệ, sđt) theo tên người (keyword).
            - "customer_lookup": Tra cứu khách hàng theo tên hoặc số điện thoại (keyword).
            - "supplier_lookup": Tra cứu nhà cung cấp, nhà phân phối theo tên (keyword).
            - "warehouse_lookup": Danh sách các kho (keyword).
            - "shift_staff": Ai đang trực trong ca làm việc.
            - "shift_checklist": Checklist kiểm tra đầu ca, cuối ca.
            - "my_shift_summary": Doanh số trong ca làm việc của tôi (ca này tôi bán được bao nhiêu).
            - "docs": Hỏi về quy trình, chính sách, hướng dẫn, làm thế nào, quy định.
            - "smalltalk": Chào hỏi (xin chào, hello, hi), cảm ơn.

            Quy tắc quan trọng:
            1. keyword: Chỉ lấy tên người (ví dụ: "Thảo", "Mai", "Vinamilk") hoặc tên sản phẩm cụ thể. TUYỆT ĐỐI KHÔNG điền "null", "đơn hàng", "hóa đơn", "danh sách", "dịch vụ", "ngày tháng", "tôi", "người đăng nhập", "nhân viên", "khách", "hàng" vào keyword. Nếu không có tên riêng cụ thể, keyword BẮT BUỘC là null.
            2. from và to: CHỈ chứa ngày tháng (định dạng YYYY-MM-DD). TUYỆT ĐỐI KHÔNG điền tên người vào from hoặc to. Nếu không có ngày cụ thể, from và to BẮT BUỘC là null.
            3. Nếu hỏi về người/tài khoản đang đăng nhập, "tôi là ai", "ai đang đăng nhập", DÙNG tool "current_user".
            4. Nếu hỏi về kho có nhiều hàng hóa/sản phẩm/tồn kho nhất, DÙNG tool "inventory" với byWarehouse=true (KHÔNG DÙNG top_products vì top_products chỉ tính hàng BÁN ra).
            5. Nếu hỏi về hàng hết hạn / cận date, DÙNG tool "expiring_lots". Nếu hỏi hàng sắp hết trong kho, DÙNG tool "inventory" với lowStockOnly=true.
            6. Nếu hỏi về nhà cung cấp / nhà phân phối / NCC, DÙNG tool "supplier_lookup".
            7. Nếu hỏi quy trình, hướng dẫn, làm thế nào, DÙNG tool "docs".
            8. Nếu chào hỏi hoặc cảm ơn, DÙNG tool "smalltalk".

            Ví dụ:
            User: Hiện tại ai đang đăng nhập vậy
            Output: {"tool": "current_user", "from": null, "to": null, "keyword": null, "byWarehouse": null, "chart": null, "lowStockOnly": null, "limit": null}

            User: Kho nào đang có nhiều sản phẩm nhất
            Output: {"tool": "inventory", "from": null, "to": null, "keyword": null, "byWarehouse": true, "chart": null, "lowStockOnly": null, "limit": null}

            User: Hàng nào sắp hết hạn
            Output: {"tool": "expiring_lots", "from": null, "to": null, "keyword": null, "byWarehouse": null, "chart": null, "lowStockOnly": null, "limit": null}

            User: Doanh số nhân viên hôm nay
            Output: {"tool": "sales_by_employee", "from": "%s", "to": "%s", "keyword": null, "byWarehouse": null, "chart": null, "lowStockOnly": null, "limit": null}

            User: Ca này tôi bán được bao nhiêu
            Output: {"tool": "my_shift_summary", "from": null, "to": null, "keyword": null, "byWarehouse": null, "chart": null, "lowStockOnly": null, "limit": null}

            User: Thông tin nhà cung cấp Vinamilk
            Output: {"tool": "supplier_lookup", "from": null, "to": null, "keyword": "Vinamilk", "byWarehouse": null, "chart": null, "lowStockOnly": null, "limit": null}

            User: Chị Thảo là nhân viên hay khách hàng của hệ thống ?
            Output: {"tool": "docs", "from": null, "to": null, "keyword": "Thảo", "byWarehouse": null, "chart": null, "lowStockOnly": null, "limit": null}

            User: Ngày 21/10/2026 có đơn nào không ?
            Output: {"tool": "invoice_search", "from": "2026-10-21", "to": "2026-10-21", "keyword": null, "byWarehouse": null, "chart": null, "lowStockOnly": null, "limit": null}

            User: Doanh thu theo từng kho hôm nay
            Output: {"tool": "revenue", "from": "%s", "to": "%s", "keyword": null, "byWarehouse": true, "chart": null, "lowStockOnly": null, "limit": null}
            """;

    private static final Set<String> STOP_KEYWORDS = Set.of(
            "null", "none", "undefined", "n/a", "danh sach", "don hang", "hoa don", "dich vu", "san pham", "mat hang", "tat ca", "don"
    );

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$|^\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?$");

    ChatbotProperties chatbotProperties;
    ObjectMapper objectMapper;
    HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SlmResult(
            String tool,
            String from,
            String to,
            String keyword,
            Boolean byWarehouse,
            Boolean chart,
            Boolean lowStockOnly,
            Integer limit
    ) {
    }

    public boolean isEnabled() {
        return chatbotProperties.getSlm() != null && chatbotProperties.getSlm().isEnabled();
    }

    public Optional<SlmResult> parse(String question, LocalDate today) {
        if (!isEnabled() || question == null || question.isBlank()) {
            return Optional.empty();
        }

        try {
            String url = chatbotProperties.getSlm().getUrl();
            if (url == null || url.isBlank()) {
                url = "http://localhost:11434";
            }
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            String todayStr = today != null ? today.toString() : LocalDate.now().toString();
            String systemMsg = SYSTEM_PROMPT.formatted(todayStr, todayStr, todayStr, todayStr, todayStr);
            Map<String, Object> payload = Map.of(
                    "model", chatbotProperties.getSlm().getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemMsg),
                            Map.of("role", "user", "content", question)
                    ),
                    "stream", false,
                    "format", "json",
                    "options", Map.of("temperature", 0.0)
            );

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(chatbotProperties.getSlm().getTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("SLM service returned status {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode message = root.path("message").path("content");
            if (message.isMissingNode() || message.asText().isBlank()) {
                return Optional.empty();
            }

            SlmResult result = objectMapper.readValue(message.asText(), SlmResult.class);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.debug("Local SLM query parsing bypassed or timed out: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public ToolArgs toToolArgs(SlmResult result) {
        return toToolArgs(result, null, null);
    }

    public ToolArgs toToolArgs(SlmResult result, String originalQuestion, LocalDate today) {
        if (result == null) {
            return ToolArgs.empty();
        }
        ToolArgs.ToolArgsBuilder builder = ToolArgs.builder();

        LocalDate fromDate = parseDate(result.from());
        LocalDate toDate = parseDate(result.to());

        // Single-day query normalization:
        if (fromDate != null && toDate == null) {
            toDate = fromDate;
        } else if (fromDate == null && toDate != null) {
            fromDate = toDate;
        }

        // If SLM didn't extract the date, use deterministic TimeRangeParser as fallback
        if (fromDate == null && toDate == null && originalQuestion != null && today != null) {
            String t = VietnameseTextUtil.stripDiacritics(VietnameseTextUtil.normalize(originalQuestion));
            TimeRangeParser.parse(t, today).ifPresent(r -> {
                builder.from(r.from());
                builder.to(r.to());
            });
        } else {
            if (fromDate != null) builder.from(fromDate);
            if (toDate != null) builder.to(toDate);
        }

        if (result.keyword() != null && !result.keyword().isBlank()) {
            String kw = result.keyword().trim();
            String norm = VietnameseTextUtil.stripDiacritics(kw.toLowerCase(Locale.ROOT));
            boolean isStop = STOP_KEYWORDS.contains(norm) || DATE_PATTERN.matcher(kw).matches();
            boolean matchesDate = (result.from() != null && kw.equalsIgnoreCase(result.from().trim()))
                    || (result.to() != null && kw.equalsIgnoreCase(result.to().trim()));
            if (!isStop && !matchesDate) {
                builder.keyword(kw);
            }
        }

        if (Boolean.TRUE.equals(result.byWarehouse())) {
            builder.byWarehouse(true);
        }
        if (Boolean.TRUE.equals(result.chart())) {
            builder.chart(true);
        }
        if (Boolean.TRUE.equals(result.lowStockOnly())) {
            builder.lowStockOnly(true);
        }
        if (result.limit() != null && result.limit() > 0) {
            builder.limit(result.limit());
        }
        return builder.build();
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s.trim())) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

}
