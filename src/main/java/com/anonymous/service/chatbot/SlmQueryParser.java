package com.anonymous.service.chatbot;

import com.anonymous.configuration.ChatbotProperties;
import com.anonymous.service.chatbot.tool.ToolArgs;
import com.anonymous.utils.VietnameseTextUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
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
            - "product_lookup": Tra cứu tổng số lượng sản phẩm, danh sách sản phẩm đang kinh doanh (keyword=null), hoặc tra giá bán, thông tin sản phẩm cụ thể theo tên/mã (keyword).
            - "invoice_lookup": Tra cứu chi tiết một mã hóa đơn cụ thể như HD000001 (keyword).
            - "invoice_search": Tra cứu danh sách đơn hàng/hóa đơn bán hàng theo ngày hoặc người (keyword, from, to).
            - "invoice_stats": Thống kê/đếm số lượng hóa đơn bán hàng theo ngày tháng (from, to).
            - "sales_return_search": Thống kê/danh sách phiếu trả hàng của khách theo ngày hoặc tên khách (keyword, from, to).
            - "stock_in_search": Thống kê/đếm số lượng và danh sách phiếu nhập kho (PN) theo ngày/tháng, kho hoặc nhà cung cấp (keyword, from, to).
            - "stock_out_search": Thống kê/đếm số lượng và danh sách phiếu xuất kho (PX) theo ngày/tháng, kho hoặc lý do (keyword, from, to).
            - "stock_transfer_search": Thống kê/đếm số lượng và danh sách phiếu chuyển kho (CK) theo ngày/tháng hoặc kho (from, to).
            - "stock_take_search": Thống kê/đếm số lượng và danh sách phiếu kiểm kê kho (KK) theo ngày/tháng hoặc kho (from, to).
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
            7. Nếu hỏi tổng số lượng sản phẩm, hiện tại có bao nhiêu sản phẩm, danh sách sản phẩm trong hệ thống, DÙNG tool "product_lookup" với keyword=null.
            8. Nếu hỏi quy trình, hướng dẫn, làm thế nào, DÙNG tool "docs".
            9. Nếu chào hỏi hoặc cảm ơn, DÙNG tool "smalltalk".

            Ví dụ:
            User: Hiện tại có bao nhiêu sản phẩm
            Output: {"tool": "product_lookup", "from": null, "to": null, "keyword": null, "byWarehouse": null, "chart": null, "lowStockOnly": null, "limit": null}

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

    private static final Path SAVED_CONFIG_PATH = Path.of(".chatbot-ai-config.json");

    private static final List<String> GEMINI_FREE_MODELS = List.of(
            "gemini-3.5-flash-lite",
            "gemini-3-flash-preview",
            "gemini-3.5-flash",
            "gemma-4-31b-it"
    );

    ChatbotProperties chatbotProperties;
    ObjectMapper objectMapper;
    HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SavedAiConfig(
            Boolean enabled,
            String provider,
            String apiKey,
            String model
    ) {
    }

    public record AiConfigStatus(
            boolean enabled,
            String provider,
            String model,
            boolean hasApiKey,
            String maskedApiKey
    ) {
    }

    @PostConstruct
    void loadSavedConfig() {
        try {
            if (Files.exists(SAVED_CONFIG_PATH)) {
                String json = Files.readString(SAVED_CONFIG_PATH, StandardCharsets.UTF_8);
                SavedAiConfig saved = objectMapper.readValue(json, SavedAiConfig.class);
                ChatbotProperties.Slm slm = chatbotProperties.getSlm();
                if (slm != null && saved != null) {
                    if (saved.enabled() != null) slm.setEnabled(saved.enabled());
                    if (saved.provider() != null && !saved.provider().isBlank()) slm.setProvider(saved.provider());
                    if (saved.apiKey() != null && !saved.apiKey().isBlank()) slm.setApiKey(saved.apiKey());
                    if (saved.model() != null && !saved.model().isBlank()) slm.setModel(saved.model());
                    log.info("Loaded Chatbot AI config: enabled={}, provider={}, model={}",
                            slm.isEnabled(), resolvedProvider(), resolvedModel());
                }
            }
        } catch (Exception e) {
            log.warn("Could not read saved chatbot AI config: {}", e.getMessage());
        }
    }

    public synchronized AiConfigStatus getConfigStatus() {
        ChatbotProperties.Slm slm = chatbotProperties.getSlm();
        boolean enabled = slm != null && slm.isEnabled();
        String key = slm != null && slm.getApiKey() != null ? slm.getApiKey().trim() : "";
        boolean hasKey = !key.isEmpty();
        String masked = hasKey
                ? (key.length() > 8 ? key.substring(0, 4) + "••••••" + key.substring(key.length() - 4) : "••••••••")
                : "";
        return new AiConfigStatus(enabled, resolvedProvider(), resolvedModel(), hasKey, masked);
    }

    public synchronized AiConfigStatus updateConfig(Boolean enabled, String provider, String apiKey, String model) {
        ChatbotProperties.Slm slm = chatbotProperties.getSlm();
        if (slm == null) {
            slm = new ChatbotProperties.Slm();
            chatbotProperties.setSlm(slm);
        }
        if (apiKey != null && !apiKey.isBlank() && !apiKey.contains("••")) {
            String trimmedKey = apiKey.trim();
            slm.setApiKey(trimmedKey);
            if (provider == null || provider.isBlank() || "auto".equalsIgnoreCase(provider)) {
                slm.setProvider(detectProviderFromKey(trimmedKey));
            }
        } else if (apiKey != null && apiKey.isBlank()) {
            slm.setApiKey("");
        }
        if (provider != null && !provider.isBlank()) {
            slm.setProvider(provider.trim().toLowerCase(Locale.ROOT));
        }
        if (model != null && !model.isBlank()) {
            slm.setModel(model.trim());
        } else {
            slm.setModel(defaultModelFor(resolvedProvider()));
        }
        if (enabled != null) {
            slm.setEnabled(enabled);
        } else if (slm.getApiKey() != null && !slm.getApiKey().isBlank()) {
            slm.setEnabled(true);
        }

        try {
            SavedAiConfig toSave = new SavedAiConfig(slm.isEnabled(), slm.getProvider(), slm.getApiKey(), slm.getModel());
            Files.writeString(SAVED_CONFIG_PATH, objectMapper.writeValueAsString(toSave), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Could not persist chatbot AI config: {}", e.getMessage());
        }
        return getConfigStatus();
    }

    public String resolvedProvider() {
        ChatbotProperties.Slm slm = chatbotProperties.getSlm();
        if (slm == null) return "gemini";
        String key = slm.getApiKey() != null ? slm.getApiKey().trim() : "";
        String p = slm.getProvider() != null ? slm.getProvider().trim().toLowerCase(Locale.ROOT) : "auto";
        if ("auto".equals(p) || p.isEmpty()) {
            return detectProviderFromKey(key);
        }
        return p;
    }

    public String resolvedModel() {
        ChatbotProperties.Slm slm = chatbotProperties.getSlm();
        String provider = resolvedProvider();
        if (slm != null && slm.getModel() != null && !slm.getModel().isBlank()) {
            String m = slm.getModel().trim();
            // Auto-switch model if user switched provider or had an incompatible/retired model configured
            if ("groq".equals(provider) && (m.startsWith("gemini") || m.startsWith("qwen") || m.contains(":"))) {
                return defaultModelFor("groq");
            }
            if ("gemini".equals(provider) && (!m.startsWith("gemini") && !m.startsWith("gemma") || m.startsWith("gemini-2.5"))) {
                return defaultModelFor("gemini");
            }
            if ("openrouter".equals(provider) && !m.contains("/")) {
                return defaultModelFor("openrouter");
            }
            return m;
        }
        return defaultModelFor(provider);
    }

    private static String detectProviderFromKey(String key) {
        if (key == null || key.isBlank()) return "gemini";
        if (key.startsWith("AIza") || key.startsWith("AQ.")) return "gemini";
        if (key.startsWith("gsk_")) return "groq";
        if (key.startsWith("sk-or-")) return "openrouter";
        return "gemini";
    }

    private static String defaultModelFor(String provider) {
        return switch (provider) {
            case "groq" -> "llama-3.3-70b-versatile";
            case "openrouter" -> "meta-llama/llama-3.3-70b-instruct:free";
            case "ollama" -> "qwen2.5:1.5b";
            default -> "gemini-3.5-flash-lite";
        };
    }

    public boolean isEnabled() {
        ChatbotProperties.Slm slm = chatbotProperties.getSlm();
        if (slm == null || !slm.isEnabled()) {
            return false;
        }
        String provider = resolvedProvider();
        if ("ollama".equals(provider)) {
            return true;
        }
        return slm.getApiKey() != null && !slm.getApiKey().isBlank();
    }

    public Optional<SlmResult> parse(String question, LocalDate today) {
        return parse(question, today, null);
    }

    public Optional<SlmResult> parse(String question, LocalDate today, ConversationContext context) {
        if (!isEnabled() || question == null || question.isBlank()) {
            return Optional.empty();
        }

        try {
            String todayStr = today != null ? today.toString() : LocalDate.now().toString();
            String systemMsg = SYSTEM_PROMPT.formatted(todayStr, todayStr, todayStr, todayStr, todayStr);
            String plainQ = VietnameseTextUtil.stripDiacritics(VietnameseTextUtil.normalize(question));
            boolean isExplicitFollowUp = plainQ.startsWith("con ") || plainQ.startsWith("the con ")
                    || plainQ.contains(" do") || plainQ.contains(" ay") || plainQ.contains("thi sao");
            if (context != null && isExplicitFollowUp) {
                StringBuilder ctxNote = new StringBuilder("\nNgữ cảnh câu trước (CHỈ dùng khi câu hỏi hiện tại đang hỏi nối tiếp bằng từ 'đó', 'ấy', 'còn...'):");
                if (context.previousIntent() != null) ctxNote.append("\n- Tool câu trước: ").append(context.previousIntent());
                if (context.lastFrom() != null) ctxNote.append("\n- Khoảng ngày câu trước: ").append(context.lastFrom()).append(" đến ").append(context.lastTo() != null ? context.lastTo() : context.lastFrom());
                if (context.lastDocumentCode() != null) ctxNote.append("\n- Mã chứng từ câu trước: ").append(context.lastDocumentCode());
                if (context.lastPersonName() != null) ctxNote.append("\n- Người được nhắc câu trước: ").append(context.lastPersonName());
                if (context.lastProductKeyword() != null) ctxNote.append("\n- Sản phẩm câu trước: ").append(context.lastProductKeyword());
                systemMsg = systemMsg + ctxNote;
            }

            String rawContent = callChatModel(systemMsg, question, true);
            if (rawContent == null || rawContent.isBlank()) {
                return Optional.empty();
            }

            String cleanedJson = stripJsonFences(rawContent);
            SlmResult result = objectMapper.readValue(cleanedJson, SlmResult.class);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.debug("AI query parsing bypassed or timed out: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> completeText(String systemPrompt, String userPrompt) {
        if (!isEnabled() || userPrompt == null || userPrompt.isBlank()) {
            return Optional.empty();
        }
        try {
            String reply = callChatModel(systemPrompt, userPrompt, false);
            return (reply != null && !reply.isBlank()) ? Optional.of(reply.trim()) : Optional.empty();
        } catch (Exception e) {
            log.debug("AI text completion bypassed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> completeJson(String systemPrompt, String userPrompt) {
        if (!isEnabled() || userPrompt == null || userPrompt.isBlank()) {
            return Optional.empty();
        }
        try {
            String raw = callChatModel(systemPrompt, userPrompt, true);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(stripJsonFences(raw));
        } catch (Exception e) {
            log.debug("AI JSON completion bypassed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String callChatModel(String systemMsg, String userMsg, boolean jsonMode) throws Exception {
        String provider = resolvedProvider();
        String model = resolvedModel();
        int timeoutMs = Math.max(chatbotProperties.getSlm().getTimeoutMs(), 3000);

        if ("ollama".equals(provider)) {
            String url = chatbotProperties.getSlm().getUrl();
            if (url == null || url.isBlank()) url = "http://localhost:11434";
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", systemMsg),
                    Map.of("role", "user", "content", userMsg)
            ));
            payload.put("stream", false);
            if (jsonMode) {
                payload.put("format", "json");
            }
            payload.put("options", Map.of("temperature", jsonMode ? 0.0 : 0.2));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Ollama returned status {}: {}", response.statusCode(), response.body());
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("message").path("content").asText(null);
        }

        // Cloud OpenAI-compatible providers: Gemini, Groq, OpenRouter
        String endpoint = switch (provider) {
            case "groq" -> "https://api.groq.com/openai/v1/chat/completions";
            case "openrouter" -> "https://openrouter.ai/api/v1/chat/completions";
            default -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        };
        String apiKey = chatbotProperties.getSlm().getApiKey() != null ? chatbotProperties.getSlm().getApiKey().trim() : "";

        HttpResponse<String> response = sendOpenAiRequest(endpoint, apiKey, model, systemMsg, userMsg, jsonMode, provider, timeoutMs);
        if (response.statusCode() != 200 && "gemini".equals(provider)) {
            for (String fallbackModel : GEMINI_FREE_MODELS) {
                if (fallbackModel.equals(model)) {
                    continue;
                }
                log.info("Rotating Gemini free model to {} after status {} on {}", fallbackModel, response.statusCode(), model);
                response = sendOpenAiRequest(endpoint, apiKey, fallbackModel, systemMsg, userMsg, jsonMode, provider, timeoutMs);
                if (response.statusCode() == 200) {
                    break;
                }
            }
        }
        if (response.statusCode() != 200) {
            log.warn("AI provider {} returned status {}: {}", provider, response.statusCode(), response.body());
            return null;
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0).path("message").path("content").asText(null);
        }
        return null;
    }

    private HttpResponse<String> sendOpenAiRequest(String endpoint, String apiKey, String model,
                                                   String systemMsg, String userMsg, boolean jsonMode,
                                                   String provider, int timeoutMs) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemMsg),
                Map.of("role", "user", "content", userMsg)
        ));
        payload.put("temperature", jsonMode ? 0.0 : 0.2);
        if (jsonMode && !"openrouter".equals(provider)) {
            payload.put("response_format", Map.of("type", "json_object"));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String stripJsonFences(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.trim();
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
