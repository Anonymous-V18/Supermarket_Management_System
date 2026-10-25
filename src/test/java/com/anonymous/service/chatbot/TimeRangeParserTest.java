package com.anonymous.service.chatbot;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TimeRangeParserTest {

    // A Saturday
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    private static TimeRangeParser.Range parse(String text) {
        return TimeRangeParser.parse(text, TODAY).orElseThrow();
    }

    @Test
    void relativeDays() {
        assertThat(parse("doanh thu hom nay")).isEqualTo(new TimeRangeParser.Range(TODAY, TODAY));
        assertThat(parse("hom qua")).isEqualTo(new TimeRangeParser.Range(TODAY.minusDays(1), TODAY.minusDays(1)));
        assertThat(parse("7 ngay qua")).isEqualTo(new TimeRangeParser.Range(LocalDate.of(2026, 10, 18), TODAY));
    }

    @Test
    void weeksStartOnMonday() {
        assertThat(parse("tuan nay")).isEqualTo(new TimeRangeParser.Range(LocalDate.of(2026, 10, 19), TODAY));
        assertThat(parse("tuan truoc"))
                .isEqualTo(new TimeRangeParser.Range(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)));
    }

    @Test
    void monthsWithoutYearMeanTheLatestOne() {
        assertThat(parse("thang nay")).isEqualTo(new TimeRangeParser.Range(LocalDate.of(2026, 10, 1), TODAY));
        assertThat(parse("thang 9"))
                .isEqualTo(new TimeRangeParser.Range(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
        // November has not come yet this year
        assertThat(parse("thang 11"))
                .isEqualTo(new TimeRangeParser.Range(LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 30)));
        assertThat(parse("thang truoc"))
                .isEqualTo(new TimeRangeParser.Range(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
    }

    @Test
    void explicitDatesAndRanges() {
        assertThat(parse("tu 01/10 den 15/10"))
                .isEqualTo(new TimeRangeParser.Range(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)));
        assertThat(parse("ngay 05/09/2026"))
                .isEqualTo(new TimeRangeParser.Range(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 5)));
    }

    @Test
    void rangesNeverEndAfterToday() {
        assertThat(parse("thang 10").to()).isEqualTo(TODAY);
        assertThat(parse("quy 4").to()).isEqualTo(TODAY);
    }

    @Test
    void noDateInText() {
        assertThat(TimeRangeParser.parse("cach tinh tien hoan", TODAY)).isEmpty();
        assertThat(TimeRangeParser.nextDays("het han trong 7 ngay toi")).contains(7);
    }

}
