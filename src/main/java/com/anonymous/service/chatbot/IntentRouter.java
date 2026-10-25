package com.anonymous.service.chatbot;

import com.anonymous.service.chatbot.tool.ChatTool;
import com.anonymous.service.chatbot.tool.ToolAccess;
import com.anonymous.service.chatbot.tool.ToolArgs;
import com.anonymous.service.chatbot.tool.ToolRegistry;
import com.anonymous.utils.ShiftUtil;
import com.anonymous.utils.VietnameseTextUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Rule-based first pass over a question: document codes, barcodes, dates, warehouse names and fixed
// Vietnamese phrases. Fast and fully predictable; when it is not sure, the caller falls back to the documents.
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IntentRouter {

    public enum Kind {
        TOOL,       // run tool with args
        DENIED,     // a data question about something the asker's roles may not see
        SMALLTALK,  // greeting / "what can you do"
        DOCS,       // a how-to / policy question for the knowledge base
        UNSURE      // looks like data but the rules could not tell which / with what
    }

    public record Route(Kind kind, ChatTool tool, ToolArgs args, String deniedTool) {

        static Route docs() {
            return new Route(Kind.DOCS, null, null, null);
        }

        static Route unsure() {
            return new Route(Kind.UNSURE, null, null, null);
        }

    }

    private static final Pattern DOCUMENT_CODE = Pattern.compile("\\b(hd|th|pn|px|ck|kk)\\s*-?\\s*(\\d{1,10})\\b");
    private static final Pattern PRODUCT_CODE = Pattern.compile("\\bsp\\s*-?\\s*(\\d{1,8})\\b");
    private static final Pattern BARCODE = Pattern.compile("\\b(\\d{8,14})\\b");
    // On the accented text, so "tối đa" is not "tôi"; accent-free only when the question was typed without accents
    private static final Pattern PERSONAL = Pattern.compile("(?U)\\b(tôi|mình|em)\\b");
    private static final Pattern PERSONAL_ACCENT_FREE = Pattern.compile("\\b(toi|minh|em)\\b");
    private static final Pattern SMALLTALK = Pattern.compile(
            "^(xin chao|chao|hello|hi|alo|cam on|thanks?|thank you|ban la ai|ban lam (duoc )?gi|ban giup (duoc )?gi|tro giup|help)\\b.*");

    // Structural pattern: [Grouping Operator: theo/từng/mỗi/phân/chia/so sánh/giữa] + [Warehouse Noun: kho/chi nhánh/cơ sở/điểm bán]
    // Matches any natural phrasing without hardcoding fixed phrases ("theo kho", "mỗi chi nhánh", "phân riêng từng cơ sở"...)
    private static final Pattern GROUP_BY_WAREHOUSE_PATTERN = Pattern.compile(
            "(?ui)(?:(?:theo|từng|mỗi|phân(?:\\s+(?:chia|loại|biệt|riêng))?|chia|riêng|so\\s+sánh|giữa\\s+các|ở\\s+các|tại\\s+các)\\s+(?:từng\\s+|riêng\\s+|các\\s+|những\\s+|rõ\\s+|tên\\s+)*(?:kho|chi\\s+nhánh|cơ\\s+sở|điểm\\s+bán)|(?:kho|chi\\s+nhánh|cơ\\s+sở)\\s+nào)"
    );

    private static final List<String> HOW_TO = List.of(
            "lam the nao", "lam sao", "cach", "quy trinh", "huong dan", "nhu the nao", "the nao de", "quy dinh",
            "quy tac", "tai sao", "vi sao", "la gi", "nghia la", "co duoc", "duoc khong", "duoc phep", "khi nao thi",
            "bao lau", "o dau", "man hinh", "cac buoc", "chinh sach", "luu y", "dieu kien", "bao loi", "bi loi", "gap loi", "loi gi", "thong bao",
            "duoc tinh", "tinh the nao", "tinh nhu", "cong thuc", "y nghia", "toi da", "may gio", "luc nao", "ai duoc",
            "ai co quyen", "quyen nao", "nhung quyen", "nhung quyen nao", "co quyen gi", "quyen gi", "co nhung quyen",
            "vai tro", "vai tro nao", "nhung vai tro", "co vai tro", "duoc lam gi", "co the lam gi", "quyen han", "chuc nang cua",
            "trang thai nao", "nhung trang thai", "gom nhung", "khac gi", "khac nhau");
    // "thế nào" alone: "doanh thu hôm nay thế nào" is a data question, "hoàn tiền thế nào" is not
    private static final List<String> HOW_TO_WEAK = List.of("the nao", "ra sao");
    // A data question names something countable in the system
    private static final List<String> DATA_NOUNS = List.of(
            "hang", "hoa don", "phieu", "doanh", "ban", "ton", "kho", "san pham", "khach", "tien", "nhan vien", "lo");

    // There are no customer orders apart from invoices, so "đơn hàng" means a sales invoice
    private static final List<String> INVOICE_NOUNS = List.of("don hang", "hoa don", "don ban", "so don");
    private static final List<String> COUNT_WORDS = List.of(
            "bao nhieu", "may", "so luong", "tong so", "thong ke", "dem", "liet ke", "danh sach");
    // A request for figures that names no particular report: "thống kê giúp tôi", "tổng quan hôm nay"
    private static final List<String> OVERVIEW_WORDS = List.of(
            "thong ke", "tong quan", "bao cao", "so lieu", "tinh hinh", "du lieu", "tong hop");

    // Words that say "this is a data question" but are not part of a product name
    private static final Set<String> DATA_WORDS = Set.of(
            "giá", "bán", "tiền", "còn", "hàng", "tồn", "kho", "số", "lượng", "mã", "vạch", "sản", "phẩm", "mặt",
            "hiện", "tại", "tra", "cứu", "xem", "loại", "món", "bây", "giờ", "liệt", "kê", "danh", "sách", "tổng",
            "kiểm", "tìm", "sp", "cái", "chiếc", "hộp", "gói", "chai", "lon", "sắp", "hết", "dưới", "mức", "tối",
            "thiểu", "thấp", "thiếu", "nhập", "thêm", "hạn", "lô", "sử", "dụng", "date", "hsd", "chạy", "nhất", "top");
    private static final Set<String> DATA_WORDS_ACCENT_FREE = accentFree(DATA_WORDS);
    private static final Set<String> TIME_WORDS = Set.of(
            "hom", "nay", "qua", "kia", "tuan", "thang", "nam", "ngay", "truoc", "gan", "day", "nhat", "quy", "tu", "den");

    private static final Set<String> STOP_PERSON_NAMES = Set.of(
            "do", "nay", "kia", "ay", "ho", "ai", "gi", "nao", "tat ca", "nhung ai", "cac ai",
            "nguoi", "khach", "nhan vien", "nha cung cap", "ncc");

    private static final List<String> PERSON_PRONOUN_PHRASES = List.of(
            "khach hang do", "khach hang nay", "khach do", "khach nay",
            "nguoi do", "nguoi nay", "ong ay", "ba ay", "anh ay", "chi ay",
            "nhan vien do", "nhan vien nay");

    private static final List<String> PRODUCT_SALES_WORDS = List.of(
            "luong mat hang", "mat hang ban ra", "mat hang duoc ban", "san pham ban ra",
            "so luong ban", "luong hang ban", "cac mat hang da ban", "nhung mat hang da ban",
            "cac san pham da ban", "nhung san pham da ban", "ban duoc nhung gi", "ban duoc mon gi",
            "chi tiet mat hang", "so luong mat hang");

    ToolRegistry toolRegistry;
    WarehouseResolver warehouseResolver;
    SlmQueryParser slmQueryParser;

    @Autowired
    public IntentRouter(ToolRegistry toolRegistry, WarehouseResolver warehouseResolver, SlmQueryParser slmQueryParser) {
        this.toolRegistry = toolRegistry;
        this.warehouseResolver = warehouseResolver;
        this.slmQueryParser = slmQueryParser;
    }

    public IntentRouter(ToolRegistry toolRegistry, WarehouseResolver warehouseResolver) {
        this(toolRegistry, warehouseResolver, null);
    }

    public Route route(String question, Collection<String> roleCodes) {
        return route(question, roleCodes, ConversationContext.empty());
    }

    public Route route(String question, Collection<String> roleCodes, String previousIntent) {
        return route(question, roleCodes, ConversationContext.ofIntent(previousIntent));
    }

    public Route route(String question, Collection<String> roleCodes, ConversationContext context) {
        String normalized = VietnameseTextUtil.normalize(question);
        String t = VietnameseTextUtil.stripDiacritics(normalized);
        LocalDate today = LocalDate.now(ShiftUtil.VIETNAM_ZONE);

        if (t.length() <= 60 && SMALLTALK.matcher(t).matches()) {
            return new Route(Kind.SMALLTALK, null, null, null);
        }

        ToolArgs.ToolArgsBuilder args = ToolArgs.builder();
        TimeRangeParser.parse(t, today).ifPresent(range -> args.from(range.from()).to(range.to()));
        Optional<WarehouseResolver.Match> warehouse = warehouseResolver.find(t);
        warehouse.ifPresent(match -> args.warehouseId(match.id()).warehouseName(match.name()));

        boolean chartRequested = containsAny(t, List.of("ve bieu do", "bieu do", "do thi", "chart"));
        if (chartRequested) {
            args.chart(true);
        }

        boolean byWarehouse = isWarehouseGrouping(question) || isWarehouseGrouping(t);
        if (byWarehouse) {
            args.byWarehouse(true);
        }

        Matcher code = DOCUMENT_CODE.matcher(t);
        if (code.find()) {
            String prefix = code.group(1).toUpperCase(Locale.ROOT);
            String fullCode = prefix + pad(code.group(2), 6);
            String tool = switch (prefix) {
                case "HD" -> "invoice_lookup";
                case "TH" -> "sales_return_lookup";
                default -> "stock_document_lookup";
            };
            return decide(tool, args.keyword(fullCode).build(), roleCodes);
        }

        Matcher productCode = PRODUCT_CODE.matcher(t);
        if (productCode.find()) {
            return decide("product_lookup", args.keyword("SP" + pad(productCode.group(1), 5)).build(), roleCodes);
        }
        Matcher barcode = BARCODE.matcher(t);
        if (barcode.find()) {
            return decide("product_lookup", args.keyword(barcode.group(1)).build(), roleCodes);
        }

        if (containsAny(t, List.of(
                "ai dang dang nhap", "ai dang dang nhap vay", "dang nhap la ai", "nguoi dang dang nhap",
                "tai khoan dang dang nhap", "tai khoan hien tai", "toi la ai", "minh la ai",
                "thong tin cua toi", "thong tin ca nhan cua toi", "thong tin ca nhan", "ho so cua toi",
                "tai khoan cua toi", "quyen cua toi", "toi co quyen gi", "toi co nhung quyen gi",
                "phan quyen cua toi", "vai tro cua toi"))) {
            return decide("current_user", args.build(), roleCodes);
        }

        if (containsAny(t, List.of("ca nay", "ca hien tai", "trong ca", "ca cua toi", "ca lam viec"))
                || (personal(normalized, t) && containsAny(t, List.of("ban duoc", "lap duoc", "doanh thu", "hoa don", "ban bao nhieu")))) {
            return decide("my_shift_summary", args.build(), roleCodes);
        }

        // Zero-Hardcode Local SLM Query Parser (for natural language analytical questions)
        if (slmQueryParser != null && slmQueryParser.isEnabled()) {
            Optional<SlmQueryParser.SlmResult> slmParsed = slmQueryParser.parse(question, today);
            if (slmParsed.isPresent()) {
                Route slmRoute = mapSlmResult(slmParsed.get(), question, today, roleCodes);
                if (slmRoute != null) {
                    return slmRoute;
                }
            }
        }

        String previousIntent = context != null ? context.previousIntent() : null;
        String lastPerson = context != null ? context.lastPersonName() : null;
        String lastProduct = context != null ? context.lastProductKeyword() : null;

        // Follow-up on person lookup (e.g. user previously looked up an employee, and now says "Họ Đỗ"):
        if (previousIntent != null && isPersonIntent(previousIntent)) {
            Matcher hoFollowUp = Pattern.compile("^(?:(?:người|ai|nhân viên|khách hàng)?\\s*(?:có\\s+)?họ\\s+([A-Za-zÀ-ỹ]+))\\??$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(question.trim());
            if (hoFollowUp.matches()) {
                String ho = hoFollowUp.group(1).trim();
                String kw = (lastPerson != null && !isPronoun(lastPerson)) ? ho + " " + lastPerson : ho;
                return decide(previousIntent, args.keyword(kw).build(), roleCodes);
            }
        }

        // Elliptical follow-up: "Đặng Thanh Mai thì sao?", "Còn Nguyễn Văn B?", "Thế còn sữa tươi?"
        Matcher elliptical = Pattern.compile("^(?:(?:còn|thế còn)\\s+([A-Za-zÀ-ỹ0-9\\s]{2,35}?)(?:\\s+(?:thì sao|thì như thế nào|sao))?|([A-Za-zÀ-ỹ0-9\\s]{2,35}?)\\s+(?:thì sao|thì như thế nào|sao))\\??$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(question.trim());
        if (elliptical.matches() && previousIntent != null && toolRegistry.find(previousIntent).isPresent()) {
            String subject = elliptical.group(1) != null ? elliptical.group(1).trim() : elliptical.group(2).trim();
            if (!isPronoun(subject) && subject.length() >= 2) {
                if (isPersonIntent(previousIntent)) {
                    return decide(previousIntent, args.keyword(subject).build(), roleCodes);
                } else if ("product_lookup".equals(previousIntent) || "inventory".equals(previousIntent)) {
                    return decide(previousIntent, args.keyword(subject).build(), roleCodes);
                }
            }
        }

        boolean mentionsTime = args.build().from() != null;
        if (containsAny(t, HOW_TO) || (!mentionsTime && containsAny(t, HOW_TO_WEAK))) {
            return Route.docs();
        }

        if (containsAny(t, List.of("ai dang truc", "ai truc", "ai lam ca", "ai trong ca",
                "nhung ai dang truc", "co nhung ai dang truc", "danh sach truc", "nhan vien truc",
                "nhan vien trong ca", "ca nay co ai", "ca truc co ai", "ai dang lam", "ai truc ca", "truc ca nay"))) {
            return decide("shift_staff", args.build(), roleCodes);
        }
        if (containsAny(t, List.of("checklist", "dau ca", "cuoi ca", "kiem tra ca"))) {
            return decide("shift_checklist", args.build(), roleCodes);
        }
        if (containsAny(t, List.of("khuyen mai", "voucher", "ma giam gia", "uu dai", "chuong trinh giam gia"))) {
            return decide("promotion_lookup", args.build(), roleCodes);
        }
        if (containsAny(t, List.of("danh sach kho", "cac kho", "co may kho", "nhung kho nao", "kho hang nao", "dia chi kho", "thong tin kho"))) {
            return decide("warehouse_lookup", args.keyword(warehouse.map(WarehouseResolver.Match::name).orElse(null)).build(), roleCodes);
        }
        if (containsAny(t, List.of("tra hang", "doi tra", "phieu tra", "hoan tra"))) {
            String person = extractPersonName(question);
            if (person == null && lastPerson != null && (containsPersonPronoun(t) || "customer_lookup".equals(previousIntent))) {
                person = lastPerson;
            }
            return decide("sales_return_search", args.keyword(person != null ? person : "").build(), roleCodes);
        }
        if (containsAny(t, INVOICE_NOUNS)) {
            String person = extractPersonName(question);
            if (person == null && lastPerson != null && (containsPersonPronoun(t) || ("customer_lookup".equals(previousIntent) && !mentionsTime))) {
                person = lastPerson;
            }
            if (person != null) {
                return decide("invoice_search", args.keyword(person).build(), roleCodes);
            }
            if (containsAny(t, COUNT_WORDS) || mentionsTime || warehouse.isPresent()
                    || containsAny(t, List.of("co hoa don nao", "co don nao", "co don hang nao", "danh sach", "thong ke", "co khong"))) {
                return decide("invoice_stats", args.build(), roleCodes);
            }
        }
        if (containsAny(t, List.of("nha cung cap", "ncc", "nha phan phoi", "nha san xuat", "dai ly"))) {
            String supplier = extractPersonName(question);
            if (supplier == null && containsPersonPronoun(t) && lastPerson != null) {
                supplier = lastPerson;
            }
            return decide("supplier_lookup", args.keyword(supplier != null ? supplier : "").build(), roleCodes);
        }
        if (containsAny(t, List.of("khach hang", "khach", "nguoi mua"))
                || (t.contains("ten") && (t.contains("ai") || t.contains("co ai") || t.contains("nguoi")))) {
            String person = extractPersonName(question);
            if (person == null && containsPersonPronoun(t) && lastPerson != null) {
                person = lastPerson;
            }
            return decide("customer_lookup", args.keyword(person != null ? person : "").build(), roleCodes);
        }
        if (containsAny(t, List.of("het han", "han su dung", "hsd", "qua han", "can date", "sap het date"))) {
            TimeRangeParser.nextDays(t).ifPresent(args::withinDays);
            return decide("expiring_lots", args.keyword(productKeyword(normalized, warehouse)).build(), roleCodes);
        }
        if (containsAny(t, PRODUCT_SALES_WORDS) || ((containsAny(t, List.of("ban chay", "top", "ban nhieu nhat", "ban duoc nhieu nhat", "mat hang chay")) || chartRequested)
                && !t.contains("nhan vien") && (t.contains("mat hang") || t.contains("san pham") || t.contains("hang")))) {
            firstNumber(t).ifPresent(args::limit);
            return decide("top_products", args.build(), roleCodes);
        }
        if (t.contains("nhan vien") && containsAny(t, List.of("doanh so", "ban duoc", "ban nhieu", "doanh thu", "ban gioi"))) {
            return decide("sales_by_employee", args.build(), roleCodes);
        }
        if (containsAny(t, List.of("nhan vien", "nhan su", "dong nghiep")) || (t.contains("nv") && (t.contains("tim") || t.contains("thong tin")))) {
            if (containsAny(t, List.of("quyen", "vai tro", "chuc nang", "duoc lam", "duoc phep", "nhiem vu"))) {
                return Route.docs();
            }
            String employee = extractPersonName(question);
            if (employee == null && containsPersonPronoun(t) && lastPerson != null) {
                employee = lastPerson;
            }
            return decide("employee_lookup", args.keyword(employee != null ? employee : "").build(), roleCodes);
        }
        if (containsAny(t, List.of("doanh thu", "loi nhuan", "doanh so", "lai gop", "gia von", "tien ban hang"))
                || (byWarehouse && (t.contains("ban") || t.contains("doanh")))) {
            return decide("revenue", args.build(), roleCodes);
        }
        if (byWarehouse && (containsAny(t, List.of("san pham", "mat hang", "hang ton", "ton kho", "hang hoa", "so luong ton", "nhieu hang"))
                || containsAny(t, List.of("nhieu nhat", "it nhat", "con nhieu", "con it")))
                && !t.contains("doanh") && !t.contains("ban")) {
            return decide("inventory", args.byWarehouse(true).keyword(productKeyword(normalized, warehouse)).build(), roleCodes);
        }
        if (containsAny(t, INVOICE_NOUNS) && (containsAny(t, COUNT_WORDS) || mentionsTime || warehouse.isPresent())) {
            return decide("invoice_stats", args.build(), roleCodes);
        }
        if (containsAny(t, List.of("sap het hang", "het hang", "duoi muc", "ton thap", "ton toi thieu", "can nhap them", "thieu hang"))) {
            return decide("inventory", args.lowStockOnly(true).keyword(productKeyword(normalized, warehouse)).build(), roleCodes);
        }
        String keyword = productKeyword(normalized, warehouse);
        if (containsAny(t, List.of("ton kho", "gia tri ton", "hang ton", "so luong ton"))) {
            Route inventory = decide("inventory", args.keyword(keyword).build(), roleCodes);
            // Cashiers may not open the stock report, but may see a product's stock (sales screen)
            if (inventory.kind() == Kind.DENIED && keyword != null) {
                return decide("product_lookup", args.build(), roleCodes);
            }
            return inventory;
        }
        if (containsAny(t, List.of("gia ban", "gia cua", "bao nhieu tien", "gia bao nhieu", "con hang", "con bao nhieu",
                "ma vach", "gia", "het chua", "con khong"))) {
            return keyword != null ? decide("product_lookup", args.keyword(keyword).build(), roleCodes) : Route.unsure();
        }

        if (containsAny(t, OVERVIEW_WORDS)) {
            return decide("data_overview", args.build(), roleCodes);
        }

        // Follow-up such as "còn tháng trước?" right after a data answer: same tool, new period / warehouse
        boolean newScope = args.build().from() != null || warehouse.isPresent();
        if (previousIntent != null && newScope && VietnameseTextUtil.syllables(t).size() <= 8
                && toolRegistry.find(previousIntent).isPresent()) {
            return decide(previousIntent, args.build(), roleCodes);
        }
        if ((containsAny(t, List.of("bao nhieu", "may", "liet ke", "danh sach", "tong so", "thong ke")) || newScope)
                && containsAny(t, DATA_NOUNS)) {
            return Route.unsure();
        }
        return Route.docs();
    }

    private Route mapSlmResult(SlmQueryParser.SlmResult result, String question, LocalDate today, Collection<String> roleCodes) {
        if (result == null || result.tool() == null || result.tool().isBlank()) {
            return null;
        }
        String toolName = result.tool().trim().toLowerCase(Locale.ROOT);
        if ("docs".equals(toolName)) {
            return Route.docs();
        }
        if ("smalltalk".equals(toolName)) {
            return new Route(Kind.SMALLTALK, null, null, null);
        }
        ToolArgs args = slmQueryParser.toToolArgs(result, question, today);
        return decide(toolName, args, roleCodes);
    }

    private Route decide(String toolName, ToolArgs args, Collection<String> roleCodes) {
        Optional<ChatTool> tool = toolRegistry.find(toolName);
        if (tool.isEmpty()) {
            return Route.unsure();
        }
        if (!ToolAccess.allows(tool.get().permission(), roleCodes)) {
            return new Route(Kind.DENIED, null, null, toolName);
        }
        return new Route(Kind.TOOL, tool.get(), args, null);
    }

    // What is left of the question once data words, time words, the warehouse and filler are removed:
    // "Giá sữa chua Vinamilk ở kho A bao nhiêu?" -> "sữa chua vinamilk"
    static String productKeyword(String normalized, Optional<WarehouseResolver.Match> warehouse) {
        boolean typedWithAccents = !VietnameseTextUtil.stripDiacritics(normalized).equals(normalized);
        Set<String> warehouseWords = new HashSet<>();
        warehouse.ifPresent(match -> warehouseWords.addAll(VietnameseTextUtil.syllables(match.phrase())));
        List<String> kept = new ArrayList<>();
        for (String syllable : VietnameseTextUtil.syllables(normalized)) {
            String plain = VietnameseTextUtil.stripDiacritics(syllable);
            boolean dataWord = DATA_WORDS.contains(syllable) || (!typedWithAccents && DATA_WORDS_ACCENT_FREE.contains(plain));
            if (dataWord || VietnameseTextUtil.isStopword(syllable) || TIME_WORDS.contains(plain)
                    || warehouseWords.contains(plain) || syllable.chars().allMatch(Character::isDigit)) {
                continue;
            }
            kept.add(syllable);
        }
        return kept.isEmpty() ? null : String.join(" ", kept);
    }

    private static Optional<Integer> firstNumber(String text) {
        Matcher matcher = Pattern.compile("\\b(\\d{1,2})\\b").matcher(text);
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    private static boolean personal(String normalized, String accentFree) {
        boolean typedWithAccents = !accentFree.equals(normalized);
        return typedWithAccents ? PERSONAL.matcher(normalized).find() : PERSONAL_ACCENT_FREE.matcher(accentFree).find();
    }

    // Whole words only: "het han" must not match inside "het hang"
    static boolean containsAny(String text, List<String> phrases) {
        for (String phrase : phrases) {
            int from = 0;
            int at;
            while ((at = text.indexOf(phrase, from)) >= 0) {
                int end = at + phrase.length();
                boolean startOk = at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
                boolean endOk = end == text.length() || !Character.isLetterOrDigit(text.charAt(end));
                if (startOk && endOk) {
                    return true;
                }
                from = at + 1;
            }
        }
        return false;
    }

    private static final Set<String> INVALID_NAME_WORDS = Set.of(
            "co", "duoc", "lam", "nhung", "cac", "may", "bao", "nhieu", "nao", "gi",
            "o", "tai", "trong", "vao", "theo", "ve", "la", "va", "hoac",
            "quyen", "vai", "tro", "chuc", "vu", "nhiem", "cong", "viec",
            "quy", "dinh", "trinh", "chinh", "huong", "dan", "he",
            "danh", "sach", "thong", "ke", "cao", "truc", "ca", "mua", "ban",
            "tra", "hang", "don", "hoa", "nhap", "xuat", "kiem", "kho", "tien"
    );

    public static boolean isValidPersonName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String plain = VietnameseTextUtil.stripDiacritics(name.trim().toLowerCase(Locale.ROOT));
        if (STOP_PERSON_NAMES.contains(plain) || COMMON_NON_PERSONS.contains(plain)) {
            return false;
        }
        String[] words = plain.split("\\s+");
        for (String w : words) {
            if (INVALID_NAME_WORDS.contains(w) || STOP_PERSON_NAMES.contains(w)) {
                return false;
            }
        }
        return words.length >= 1 && name.trim().length() >= 2;
    }

    public static String extractPersonName(String normalized) {
        // Compound patterns: "tên Thảo và có họ Đỗ" -> "Đỗ Thảo"
        Matcher compound1 = Pattern.compile("(?ui)tên\\s+([A-Za-zÀ-ỹ]+)\\s+và\\s+(?:có\\s+)?họ\\s+([A-Za-zÀ-ỹ]+)").matcher(normalized);
        if (compound1.find()) {
            return compound1.group(2).trim() + " " + compound1.group(1).trim();
        }
        Matcher compound2 = Pattern.compile("(?ui)họ\\s+([A-Za-zÀ-ỹ]+)\\s+và\\s+(?:có\\s+)?tên\\s+([A-Za-zÀ-ỹ]+)").matcher(normalized);
        if (compound2.find()) {
            return compound2.group(1).trim() + " " + compound2.group(2).trim();
        }
        Matcher compound3 = Pattern.compile("(?ui)họ\\s+([A-Za-zÀ-ỹ]+)\\s+tên\\s+([A-Za-zÀ-ỹ]+)").matcher(normalized);
        if (compound3.find()) {
            return compound3.group(1).trim() + " " + compound3.group(2).trim();
        }
        Matcher compound4 = Pattern.compile("(?ui)tên\\s+([A-Za-zÀ-ỹ]+)\\s+họ\\s+([A-Za-zÀ-ỹ]+)").matcher(normalized);
        if (compound4.find()) {
            return compound4.group(2).trim() + " " + compound4.group(1).trim();
        }

        Pattern p = Pattern.compile("(?ui)(?:tên\\s+(?:là\\s+)?|khách\\s+(?:hàng\\s+)?|nhân\\s+viên\\s+|nhà\\s+cung\\s+cấp\\s+|của\\s+|(?:chị|anh|cô|chú|bác|em|bạn)\\s+)([A-Za-zÀ-ỹ0-9\\s]+?)(?:\\s+(?:từng|đã|không|chưa|ở|tại|vào|ngày|tháng|có|làm|bán|mua|nào|được|sinh)|\\?|$)");
        Matcher m = p.matcher(normalized.trim());
        if (m.find()) {
            String match = m.group(1).trim();
            if (match.toLowerCase().startsWith("tên là ")) {
                match = match.substring(7).trim();
            } else if (match.toLowerCase().startsWith("tên ")) {
                match = match.substring(4).trim();
            }
            if (isValidPersonName(match)) {
                return match;
            }
        }
        Matcher phone = Pattern.compile("\\b(0\\d{9,10})\\b").matcher(normalized);
        if (phone.find()) {
            return phone.group(1);
        }
        return null;
    }

    public static boolean isPronoun(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String plain = VietnameseTextUtil.stripDiacritics(text.trim().toLowerCase(Locale.ROOT));
        return STOP_PERSON_NAMES.contains(plain);
    }

    public static boolean containsPersonPronoun(String plain) {
        if (plain == null || plain.isBlank()) {
            return false;
        }
        return containsAny(plain, PERSON_PRONOUN_PHRASES);
    }

    public static boolean isWarehouseGrouping(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return GROUP_BY_WAREHOUSE_PATTERN.matcher(text).find();
    }

    public static boolean containsPronoun(String plain) {
        return containsPersonPronoun(plain);
    }

    public static boolean isPersonIntent(String intent) {
        return "customer_lookup".equals(intent)
                || "sales_return_search".equals(intent)
                || "invoice_search".equals(intent)
                || "employee_lookup".equals(intent)
                || "supplier_lookup".equals(intent);
    }

    private static final Set<String> COMMON_NON_PERSONS = Set.of(
            "hoa don", "phieu tra hang", "doanh thu", "loi nhuan", "kho", "san pham", "trang thai", "so luong"
    );

    public static boolean isCommonKeyword(String text) {
        if (text == null) {
            return true;
        }
        String plain = VietnameseTextUtil.stripDiacritics(text.trim().toLowerCase(Locale.ROOT));
        return COMMON_NON_PERSONS.contains(plain);
    }

    private static String pad(String digits, int width) {
        return digits.length() >= width ? digits : "0".repeat(width - digits.length()) + digits;
    }

    private static Set<String> accentFree(Set<String> words) {
        Set<String> result = new HashSet<>();
        words.forEach(word -> result.add(VietnameseTextUtil.stripDiacritics(word)));
        return result;
    }

}
