package com.anonymous.service.chatbot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Vietnamese date expressions -> an inclusive date range. Works on accent-free lower-case text
// ("hom nay", "thang 9", "tu 01/10 den 15/10", "7 ngay qua", ...). Ranges never end after today.
public final class TimeRangeParser {

    public record Range(LocalDate from, LocalDate to) {
    }

    private static final String DATE = "(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{4}))?";
    private static final Pattern BETWEEN = Pattern.compile("(?:tu|tu ngay)\\s*" + DATE + "\\s*(?:den|toi|-)\\s*(?:ngay\\s*)?" + DATE);
    private static final Pattern SINGLE_DATE = Pattern.compile("(?<![\\d/])" + DATE + "(?![\\d/])");
    private static final Pattern MONTH = Pattern.compile("thang\\s*(\\d{1,2})(?:\\s*(?:nam|/)\\s*(\\d{4}))?");
    private static final Pattern QUARTER = Pattern.compile("quy\\s*([1-4])(?:\\s*(?:nam|/)\\s*(\\d{4}))?");
    private static final Pattern YEAR = Pattern.compile("nam\\s*(\\d{4})");
    private static final Pattern LAST_DAYS = Pattern.compile("(\\d{1,3})\\s*ngay\\s*(?:qua|gan day|gan nhat|truoc|vua qua)");
    private static final Pattern NEXT_DAYS = Pattern.compile("(\\d{1,3})\\s*ngay\\s*(?:toi|nua|sap toi|ke tiep)");

    private TimeRangeParser() {
    }

    public static Optional<Range> parse(String accentFree, LocalDate today) {
        String t = accentFree;
        Matcher between = BETWEEN.matcher(t);
        if (between.find()) {
            LocalDate from = dateOf(between.group(1), between.group(2), between.group(3), today);
            LocalDate to = dateOf(between.group(4), between.group(5), between.group(6), today);
            if (from != null && to != null && !from.isAfter(to)) {
                return Optional.of(capped(from, to, today));
            }
        }
        Matcher lastDays = LAST_DAYS.matcher(t);
        if (lastDays.find()) {
            int days = Math.clamp(Integer.parseInt(lastDays.group(1)), 1, 366);
            return Optional.of(new Range(today.minusDays(days - 1L), today));
        }
        if (t.contains("hom nay") || t.contains("ngay hom nay")) {
            return Optional.of(new Range(today, today));
        }
        if (t.contains("hom qua")) {
            return Optional.of(new Range(today.minusDays(1), today.minusDays(1)));
        }
        if (t.contains("hom kia")) {
            return Optional.of(new Range(today.minusDays(2), today.minusDays(2)));
        }
        if (t.contains("tuan nay")) {
            return Optional.of(new Range(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today));
        }
        if (t.contains("tuan truoc") || t.contains("tuan qua")) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
            return Optional.of(new Range(monday, monday.plusDays(6)));
        }
        if (t.contains("thang nay")) {
            return Optional.of(new Range(today.withDayOfMonth(1), today));
        }
        if (t.contains("thang truoc") || t.contains("thang qua")) {
            YearMonth previous = YearMonth.from(today).minusMonths(1);
            return Optional.of(new Range(previous.atDay(1), previous.atEndOfMonth()));
        }
        Matcher month = MONTH.matcher(t);
        if (month.find()) {
            int monthValue = Integer.parseInt(month.group(1));
            if (monthValue >= 1 && monthValue <= 12) {
                int year = month.group(2) != null ? Integer.parseInt(month.group(2))
                        : (monthValue > today.getMonthValue() ? today.getYear() - 1 : today.getYear());
                YearMonth yearMonth = YearMonth.of(year, monthValue);
                return Optional.of(capped(yearMonth.atDay(1), yearMonth.atEndOfMonth(), today));
            }
        }
        Matcher quarter = QUARTER.matcher(t);
        if (quarter.find()) {
            int q = Integer.parseInt(quarter.group(1));
            int year = quarter.group(2) != null ? Integer.parseInt(quarter.group(2)) : today.getYear();
            YearMonth first = YearMonth.of(year, (q - 1) * 3 + 1);
            return Optional.of(capped(first.atDay(1), first.plusMonths(2).atEndOfMonth(), today));
        }
        if (t.contains("nam nay")) {
            return Optional.of(new Range(today.withDayOfYear(1), today));
        }
        if (t.contains("nam ngoai") || t.contains("nam truoc")) {
            LocalDate first = today.minusYears(1).withDayOfYear(1);
            return Optional.of(new Range(first, first.with(TemporalAdjusters.lastDayOfYear())));
        }
        Matcher year = YEAR.matcher(t);
        if (year.find()) {
            LocalDate first = LocalDate.of(Integer.parseInt(year.group(1)), 1, 1);
            return Optional.of(capped(first, first.with(TemporalAdjusters.lastDayOfYear()), today));
        }
        Matcher single = SINGLE_DATE.matcher(t);
        if (single.find()) {
            LocalDate date = dateOf(single.group(1), single.group(2), single.group(3), today);
            if (date != null) {
                return Optional.of(new Range(date, date));
            }
        }
        return Optional.empty();
    }

    // "7 ngay toi" (future window, used for expiry questions)
    public static Optional<Integer> nextDays(String accentFree) {
        Matcher matcher = NEXT_DAYS.matcher(accentFree);
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    public static boolean mentionsTime(String accentFree, LocalDate today) {
        return parse(accentFree, today).isPresent();
    }

    private static Range capped(LocalDate from, LocalDate to, LocalDate today) {
        return new Range(from, to.isAfter(today) ? today : to);
    }

    // dd/mm without a year means the most recent such date not after today
    private static LocalDate dateOf(String day, String month, String year, LocalDate today) {
        try {
            int d = Integer.parseInt(day);
            int m = Integer.parseInt(month);
            if (year != null) {
                return LocalDate.of(Integer.parseInt(year), m, d);
            }
            LocalDate date = LocalDate.of(today.getYear(), m, d);
            return date.isAfter(today) ? date.minusYears(1) : date;
        } catch (RuntimeException e) {
            return null;
        }
    }

}
