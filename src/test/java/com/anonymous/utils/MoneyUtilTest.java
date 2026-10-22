package com.anonymous.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUtilTest {

    @Test
    void lineTotalAppliesDiscountAndRoundsToWholeDong() {
        assertThat(MoneyUtil.lineTotal(new BigDecimal("20000"), 2, new BigDecimal("10")))
                .isEqualByComparingTo("36000");
        // 3 x 9,999 = 29,997; -15% = 25,497.45 -> 25,497
        assertThat(MoneyUtil.lineTotal(new BigDecimal("9999"), 3, new BigDecimal("15")))
                .isEqualByComparingTo("25497");
    }

    @Test
    void missingValuesCountAsZero() {
        assertThat(MoneyUtil.lineTotal(null, 5, null)).isEqualByComparingTo("0");
        assertThat(MoneyUtil.lineTotal(new BigDecimal("1000"), 2, null)).isEqualByComparingTo("2000");
        assertThat(MoneyUtil.round(null)).isEqualByComparingTo("0");
    }

    @Test
    void roundUsesHalfUp() {
        assertThat(MoneyUtil.round(new BigDecimal("1000.5"))).isEqualByComparingTo("1001");
        assertThat(MoneyUtil.round(new BigDecimal("1000.49"))).isEqualByComparingTo("1000");
    }

}
