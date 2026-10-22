package com.anonymous.utils;

import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class ShiftUtil {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int SHIFT_HOURS = 8;

    public record ShiftInfo(LocalDate date, String code, String name) {

        public Instant startInstant() {
            return date.atTime(startHourOf(code), 0).atZone(VIETNAM_ZONE).toInstant();
        }

        public Instant endInstant() {
            return date.atTime(startHourOf(code), 0).plusHours(SHIFT_HOURS).atZone(VIETNAM_ZONE).toInstant();
        }

    }

    private ShiftUtil() {
    }

    // Missing code/date fall back to the shift running now (Vietnam time)
    public static ShiftInfo resolve(LocalDate date, String shiftCode) {
        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        int hour = now.getHour();

        String resolvedCode = shiftCode;
        if (resolvedCode == null || resolvedCode.isBlank()) {
            if (hour >= 6 && hour < 14) {
                resolvedCode = "MORNING";
            } else if (hour >= 14 && hour < 22) {
                resolvedCode = "AFTERNOON";
            } else {
                resolvedCode = "NIGHT";
            }
        }
        resolvedCode = resolvedCode.trim().toUpperCase();

        LocalDate resolvedDate = date;
        if (resolvedDate == null) {
            resolvedDate = now.toLocalDate();
            // Nếu là ca đêm và đang ở khung giờ 00:00 - 05:59, ca này thuộc về ngày hôm trước
            if ("NIGHT".equals(resolvedCode) && hour < 6) {
                resolvedDate = resolvedDate.minusDays(1);
            }
        }

        return new ShiftInfo(resolvedDate, resolvedCode, nameOf(resolvedCode));
    }

    public static String nameOf(String code) {
        if (code == null) return "";
        return switch (code.toUpperCase()) {
            case "MORNING" -> "Ca sáng (06:00 - 14:00)";
            case "AFTERNOON" -> "Ca chiều (14:00 - 22:00)";
            case "NIGHT" -> "Ca đêm (22:00 - 06:00)";
            default -> code;
        };
    }

    private static int startHourOf(String code) {
        return switch (code) {
            case "MORNING" -> 6;
            case "AFTERNOON" -> 14;
            case "NIGHT" -> 22;
            default -> throw new AppException(ErrorCode.SHIFT_NOT_EXIST);
        };
    }

}
