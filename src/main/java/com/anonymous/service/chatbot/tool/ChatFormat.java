package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.PaymentMethod;
import com.anonymous.dto.response.StatusInvoiceResponse;
import com.anonymous.utils.MoneyUtil;
import com.anonymous.utils.ShiftUtil;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// Vietnamese formatting for tool answers: 1.234.567 ₫, dd/MM/yyyy, Markdown tables
public final class ChatFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ChatFormat() {
    }

    public static String money(BigDecimal amount) {
        return number(MoneyUtil.round(amount)) + " ₫";
    }

    public static String number(Number value) {
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi")));
        return format.format(value == null ? 0 : value);
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    public static String dateTime(Instant instant) {
        return instant == null ? "—" : DATE_TIME.format(instant.atZone(ShiftUtil.VIETNAM_ZONE));
    }

    public static String range(LocalDate from, LocalDate to) {
        return from.equals(to) ? "ngày " + date(from) : "từ " + date(from) + " đến " + date(to);
    }

    public static String status(StatusInvoiceResponse status) {
        return status == null ? "—" : status.getName();
    }

    public static String payment(PaymentMethod method) {
        if (method == null) {
            return "—";
        }
        return switch (method) {
            case CASH -> "Tiền mặt";
            case CARD -> "Thẻ";
            case TRANSFER -> "Chuyển khoản";
        };
    }

    public static String cell(Object value) {
        return value == null ? "—" : value.toString().replace("|", "/").replace("\n", " ");
    }

    public static String table(List<String> headers, List<List<String>> rows) {
        StringBuilder table = new StringBuilder();
        table.append("| ").append(String.join(" | ", headers)).append(" |\n");
        table.append("|").append(" --- |".repeat(headers.size())).append('\n');
        for (List<String> row : rows) {
            table.append("| ").append(String.join(" | ", row.stream().map(ChatFormat::cell).toList())).append(" |\n");
        }
        return table.toString();
    }

    public static String collapsible(String title, String content) {
        String cleanTitle = title.replace("\n", " ").trim().replaceFirst("^[👉📥🔗\\s]+", "");
        return "\n:::details " + cleanTitle + "\n" + content.trim() + "\n:::\n";
    }

    public record ChartBar(String label, long rawValue, String displayValue, int percent) {
    }

    public static String chart(String title, List<ChartBar> bars) {
        StringBuilder sb = new StringBuilder("\n:::chart ").append(title.replace("\n", " ").trim()).append("\n");
        for (ChartBar bar : bars) {
            sb.append(cell(bar.label())).append(" | ")
                    .append(bar.rawValue()).append(" | ")
                    .append(cell(bar.displayValue())).append(" | ")
                    .append(Math.clamp(bar.percent(), 2, 100)).append("%\n");
        }
        sb.append(":::\n");
        return sb.toString();
    }

    public static String downloadCsvLink(String label, String fileName, List<String> headers, List<List<String>> rows) {
        String cleanLabel = label.replaceFirst("^[📥👉🔗\\s]+", "").trim();
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(headers.stream().map(ChatFormat::escapeCsv).collect(Collectors.joining(","))).append("\r\n");
        for (List<String> row : rows) {
            sb.append(row.stream().map(ChatFormat::escapeCsv).collect(Collectors.joining(","))).append("\r\n");
        }
        String encoded = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8).replace("+", "%20");
        String uri = "data:text/csv;charset=utf-8," + encoded;
        return "\n[" + cleanLabel + "](" + uri + "#" + fileName + ")\n";
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replace("\r", " ").replace("\n", " ");
        if (clean.contains(",") || clean.contains("\"")) {
            return "\"" + clean.replace("\"", "\"\"") + "\"";
        }
        return clean;
    }

}
