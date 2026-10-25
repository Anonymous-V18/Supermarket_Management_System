package com.anonymous.service.chatbot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FollowUpQuestionTest {

    private static final String PREVIOUS = "Luồng trả hàng > Quy tắc & lỗi";

    @Test
    void followUpsCarryThePreviousSection() {
        assertThat(FollowUpQuestion.standalone("Còn nếu khách không có hóa đơn?", PREVIOUS))
                .isEqualTo("Còn nếu khách không có hóa đơn?\nLuồng trả hàng Quy tắc & lỗi");
        assertThat(FollowUpQuestion.isFollowUp("vay neu huy thi sao")).isTrue();
        assertThat(FollowUpQuestion.isFollowUp("Bước đó ai làm?")).isTrue();
        assertThat(FollowUpQuestion.isFollowUp("Hủy được không?")).isTrue();
    }

    @Test
    void standaloneQuestionsAreLeftAlone() {
        assertThat(FollowUpQuestion.standalone("Quy trình kiểm kê kho gồm những bước nào?", PREVIOUS))
                .isEqualTo("Quy trình kiểm kê kho gồm những bước nào?");
        assertThat(FollowUpQuestion.isFollowUp("Lý do xuất kho có bắt buộc không?")).isFalse();
        assertThat(FollowUpQuestion.isFollowUp("Sản phẩm không có trên hóa đơn thì sao?")).isFalse();
        assertThat(FollowUpQuestion.standalone("Còn hủy thì sao?", null)).isEqualTo("Còn hủy thì sao?");
    }

}
