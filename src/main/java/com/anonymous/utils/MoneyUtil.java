package com.anonymous.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

// VND has no minor unit: every amount the server computes is rounded to whole đồng
public final class MoneyUtil {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private MoneyUtil() {
    }

    public static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public static BigDecimal round(BigDecimal value) {
        return orZero(value).setScale(0, RoundingMode.HALF_UP);
    }

    // unitPrice x quantity, minus discountPercent %
    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity, BigDecimal discountPercent) {
        BigDecimal gross = orZero(unitPrice).multiply(BigDecimal.valueOf(quantity));
        BigDecimal keptPercent = HUNDRED.subtract(orZero(discountPercent));
        return round(gross.multiply(keptPercent).divide(HUNDRED, 4, RoundingMode.HALF_UP));
    }

}
