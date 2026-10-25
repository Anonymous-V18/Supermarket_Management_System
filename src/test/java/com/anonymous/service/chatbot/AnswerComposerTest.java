package com.anonymous.service.chatbot;

import com.anonymous.entity.KbChunk;
import com.anonymous.service.chatbot.KnowledgeRetriever.Passage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerComposerTest {

    private static final String REPORT_ROW = "| Doanh thu | Thẻ tổng: số hóa đơn, doanh thu, hoàn trả, doanh thu thuần, lãi gộp. | "
            + "Chỉ hóa đơn đã hoàn thành. Giá vốn = giá vốn hàng bán − giá vốn hàng trả lại. Lãi gộp = doanh thu thuần − giá vốn. |";

    private static Passage passage(String heading, String content) {
        KbChunk chunk = KbChunk.builder().id(heading).documentId("doc").chunkIndex(0).headingPath(heading).content(content).build();
        return new Passage(chunk, heading, 1, null);
    }

    private static final Passage REFUND = passage("Luồng trả hàng > Cách tính tiền hoàn",
            "Khách được hoàn đúng số đã trả cho món đó: đã trừ % giảm giá dòng.\nTrả 3 hộp sữa chua: 30.000 × 174/180 = 29.000 ₫.");
    private static final Passage REPORT = passage("Luồng xem báo cáo",
            "- Vào Báo cáo; chỉ các tab đúng quyền mới hiện ra.\n| Tab | Nội dung | Cách tính |\n| --- | --- | --- |\n" + REPORT_ROW
                    + "\n| Tồn kho | Tồn từng sản phẩm theo kho | Dòng tồn ≤ tồn tối thiểu tô vàng |");
    private static final Passage MONEY = passage("Quy tắc chung > Tiền, giá và dữ liệu nhập",
            "- Mọi giá trị tiền do server tính: đơn giá, thành tiền, tổng tiền, tiền thối.\n- Tiền làm tròn tới đồng (làm tròn nửa lên).");

    private static AnswerComposer composer() {
        return new AnswerComposer();
    }

    private record Collected(List<String> deltas, List<String> replaced) implements AnswerComposer.Sink {

        Collected() {
            this(new ArrayList<>(), new ArrayList<>());
        }

        @Override
        public void delta(String text) {
            deltas.add(text);
        }

        @Override
        public void replace(String text) {
            replaced.add(text);
        }

    }

    @Test
    void sectionNamedByTheQuestionIsQuotedWhole() {
        String question = "Cách tính tiền hoàn khi khách trả hàng?";
        String answer = AnswerComposer.extract(new TopicMatcher(question), List.of(REFUND, MONEY));
        assertThat(answer).startsWith("Theo **Luồng trả hàng > Cách tính tiền hoàn** [1]:")
                .contains("Trả 3 hộp sữa chua: 30.000 × 174/180 = 29.000 ₫.")
                .doesNotContain("làm tròn");
    }

    @Test
    void otherwiseOnlyTheMatchingTableRowWithItsHeader() {
        String question = "Doanh thu thuần được tính thế nào?";
        String answer = AnswerComposer.extract(new TopicMatcher(question), List.of(REPORT));
        assertThat(answer).contains("| Tab | Nội dung | Cách tính |\n| --- | --- | --- |\n" + REPORT_ROW)
                .doesNotContain("Tồn kho")
                .doesNotContain("Vào Báo cáo");
    }

    @Test
    void aDateAloneIsNotATopic() {
        TopicMatcher topic = new TopicMatcher("Ngày 22/10/2026 có bao nhiêu đơn hàng ?");
        assertThat(topic.share("Supermarket Management · Đặc tả chức năng theo luồng nghiệp vụ · 22/10/2026")).isZero();
    }

    @Test
    void offTopicPassagesGiveNoAnswerInsteadOfAGuess() {
        // Only money-rounding rules are readable for this asker: not an answer to a refund question
        String question = "Cách tính tiền hoàn khi khách trả hàng?";
        assertThat(AnswerComposer.extract(new TopicMatcher(question), List.of(MONEY))).isNull();

        Collected sink = new Collected();
        AnswerComposer.Answer answer = composer().compose(question, List.of(MONEY), new TopicMatcher(question), sink);
        assertThat(answer.answered()).isFalse();
        assertThat(sink.deltas()).containsExactly(AnswerComposer.NO_INFORMATION);
    }

    @Test
    void theAnswerIsTheExtract() {
        String question = "Cách tính tiền hoàn khi khách trả hàng?";
        Collected sink = new Collected();
        AnswerComposer.Answer answer = composer().compose(question, List.of(REFUND), new TopicMatcher(question), sink);
        assertThat(answer.answered()).isTrue();
        assertThat(answer.generated()).isFalse();
        assertThat(String.join("", sink.deltas())).contains("29.000 ₫");
    }

}
