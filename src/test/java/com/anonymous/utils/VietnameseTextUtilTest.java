package com.anonymous.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VietnameseTextUtilTest {

    @Test
    void stripsAccentsIncludingDStroke() {
        assertThat(VietnameseTextUtil.stripDiacritics("Hóa đơn Đã hủy")).isEqualTo("Hoa don Da huy");
    }

    @Test
    void normalizeLowersAndCollapsesSpaces() {
        assertThat(VietnameseTextUtil.normalize("  Phiếu   NHẬP\tkho ")).isEqualTo("phiếu nhập kho");
    }

    @Test
    void termsHaveSyllablesPairsAndAccentFreeTwins() {
        assertThat(VietnameseTextUtil.terms("Hóa đơn"))
                .contains("hóa", "đơn", "hóa_đơn", "a:hoa", "a:don", "a:hoa_don");
        // A question typed without accents shares the accent-free terms
        assertThat(VietnameseTextUtil.terms("hoa don")).contains("a:hoa_don");
    }

    @Test
    void keywordsDropStopwordsAndCanKeepAccents() {
        assertThat(VietnameseTextUtil.keywords("Làm sao để đổi mật khẩu?")).containsExactly("doi", "mat", "khau");
        assertThat(VietnameseTextUtil.keywords("Thủ đô", true)).containsExactly("thủ", "đô");
        assertThat(VietnameseTextUtil.hasDiacritics("thu do")).isFalse();
        assertThat(VietnameseTextUtil.hasDiacritics("thủ đô")).isTrue();
    }

}
