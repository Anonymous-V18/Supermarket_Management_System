package com.anonymous.service.chatbot;

import com.anonymous.service.chatbot.KnowledgeRetriever.Passage;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Writes the answer to a knowledge-base question from the retrieved passages only.
// When an AI model (Gemini / Groq / OpenRouter) is configured, synthesizes a natural answer with [1], [2] citations;
// otherwise falls back to deterministic extract().
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnswerComposer {

    public static final String NO_INFORMATION =
            "Mình chưa tìm thấy thông tin này trong tài liệu bạn được phép xem. Bạn thử hỏi cụ thể hơn, hoặc liên hệ quản lý để được hỗ trợ.";

    private static final String RAG_SYSTEM_PROMPT = """
            Bạn là Trợ lý AI nội bộ của Hệ thống Quản lý Siêu thị.
            Nhiệm vụ: Dựa HOÀN TOÀN vào các đoạn tài liệu nội bộ được cung cấp bên dưới để trả lời câu hỏi của nhân viên một cách tự nhiên, rõ ràng, súc tích và chuyên nghiệp bằng tiếng Việt.
            Quy tắc bắt buộc:
            1. Chỉ sử dụng thông tin có trong các đoạn tài liệu được cung cấp. Không tự bịa thêm quy định không có trong tài liệu.
            2. Luôn gắn trích dẫn nguồn dạng [1], [2] ở cuối câu hoặc đoạn tương ứng với số thứ tự tài liệu.
            3. Trình bày đẹp mắt bằng Markdown (in đậm các ý chính, gạch đầu dòng hoặc bảng nếu phù hợp).
            4. Nếu tài liệu hoàn toàn không liên quan đến câu hỏi, chỉ trả lời đúng cụm từ: NO_INFO
            """;

    static final double MIN_TOPICAL_SHARE = 0.5;
    private static final int MAX_EXTRACT_SECTIONS = 2;
    private static final int MAX_EXTRACT_LINES = 10;

    SlmQueryParser slmQueryParser;

    @Autowired
    public AnswerComposer(SlmQueryParser slmQueryParser) {
        this.slmQueryParser = slmQueryParser;
    }

    public AnswerComposer() {
        this(null);
    }

    public interface Sink {
        void delta(String text);

        // The streamed text was rejected; show this instead
        void replace(String text);
    }

    // answered = false when the reply is "no information"
    public record Answer(String text, boolean generated, boolean answered) {
    }

    // topic: the question's topic as weighted by the retriever (see TopicMatcher)
    public Answer compose(String question, List<Passage> passages, TopicMatcher topic, Sink sink) {
        if (passages.isEmpty()) {
            sink.delta(NO_INFORMATION);
            return new Answer(NO_INFORMATION, false, false);
        }
        String extracted = extract(topic, passages);
        if (extracted == null) {
            sink.delta(NO_INFORMATION);
            return new Answer(NO_INFORMATION, false, false);
        }

        if (slmQueryParser != null && slmQueryParser.isEnabled()) {
            StringBuilder contextPrompt = new StringBuilder("Câu hỏi: ").append(question).append("\n\nTài liệu tham khảo:\n");
            for (int i = 0; i < Math.min(passages.size(), 4); i++) {
                Passage p = passages.get(i);
                String heading = p.chunk().getHeadingPath() != null ? p.chunk().getHeadingPath() : p.documentTitle();
                contextPrompt.append("\n[").append(i + 1).append("] Tiêu đề: ").append(heading)
                        .append("\nNội dung:\n").append(p.chunk().getContent()).append("\n");
            }
            Optional<String> aiReply = slmQueryParser.completeText(RAG_SYSTEM_PROMPT, contextPrompt.toString());
            if (aiReply.isPresent() && !aiReply.get().contains("NO_INFO")) {
                String text = aiReply.get();
                sink.delta(text);
                return new Answer(text, true, true);
            }
        }

        sink.delta(extracted);
        return new Answer(extracted, false, true);
    }

    // Answer: the passages' own lines, word for word, cut down to what the question is about.
    //  - the first section is shown whole when its heading names the topic (e.g. every step of "Luồng kiểm kê kho"),
    //    otherwise only its best-matching lines, with the header of a table they belong to;
    //  - a passage the embedding search found close in meaning (denseScore set) counts as naming the topic,
    //    even when it words it differently ("mật khẩu mặc định" for "mật khẩu ban đầu");
    //  - a second section is added only when its heading also names the topic;
    //  - each section carries its source number [n] (same order as the citations).
    // null when nothing found is really about the question (the caller then says it has no information).
    static String extract(TopicMatcher topic, List<Passage> passages) {
        StringBuilder answer = new StringBuilder();
        Set<String> usedHeadings = new LinkedHashSet<>();
        int sections = 0;
        boolean closeInMeaning = false;
        for (int i = 0; i < passages.size() && sections < MAX_EXTRACT_SECTIONS; i++) {
            Passage passage = passages.get(i);
            String heading = passage.chunk().getHeadingPath() != null ? passage.chunk().getHeadingPath() : passage.documentTitle();
            if (!usedHeadings.add(heading)) {
                continue;
            }
            boolean aboutTopic = topic.share(heading) >= MIN_TOPICAL_SHARE || passage.denseScore() != null;
            if (sections > 0 && !aboutTopic) {
                continue;
            }
            List<String> lines = aboutTopic
                    ? List.of(passage.chunk().getContent().split("\n"))
                    : matchingLines(passage.chunk().getContent(), topic);
            if (lines.isEmpty()) {
                continue;
            }
            if (sections > 0) {
                answer.append("\n\n");
            }
            answer.append("Theo **").append(heading).append("** [").append(i + 1).append("]:\n\n")
                    .append(String.join("\n", lines));
            sections++;
            closeInMeaning |= passage.denseScore() != null;
        }
        if (sections == 0 || (!closeInMeaning && topic.share(answer.toString()) < MIN_TOPICAL_SHARE)) {
            return null;
        }
        return answer.toString();
    }

    // Lines close to the best match (at least half of the topic), kept in document order;
    // a table row brings its header row along
    private static List<String> matchingLines(String content, TopicMatcher topic) {
        String[] lines = content.split("\n");
        double[] scores = new double[lines.length];
        double best = 0;
        String currentTableHeader = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.startsWith("|") && !line.startsWith("| ---")) {
                if (currentTableHeader == null) {
                    currentTableHeader = line;
                    scores[i] = topic.share(line);
                } else {
                    scores[i] = topic.share(currentTableHeader + " " + line);
                }
            } else {
                if (!line.startsWith("|")) {
                    currentTableHeader = null;
                }
                scores[i] = topic.share(line);
            }
            best = Math.max(best, scores[i]);
        }
        if (best < MIN_TOPICAL_SHARE) {
            return List.of();
        }
        double cut = Math.max(MIN_TOPICAL_SHARE, best - 0.25);
        List<String> selected = new ArrayList<>();
        String tableHeader = null;
        boolean headerShown = false;
        for (int i = 0; i < lines.length && selected.size() < MAX_EXTRACT_LINES; i++) {
            String line = lines[i];
            boolean tableRow = line.startsWith("|");
            if (!tableRow) {
                tableHeader = null;
                headerShown = false;
            } else if (i + 1 < lines.length && lines[i + 1].startsWith("| ---")) {
                tableHeader = line + "\n" + lines[i + 1];
                headerShown = false;
                i++;
                continue;
            }
            if (scores[i] >= cut) {
                if (tableRow && tableHeader != null && !headerShown) {
                    selected.add(tableHeader);
                    headerShown = true;
                }
                selected.add(line);
            }
        }
        return selected;
    }

}
