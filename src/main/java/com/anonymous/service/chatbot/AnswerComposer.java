package com.anonymous.service.chatbot;

import com.anonymous.service.chatbot.KnowledgeRetriever.Passage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Writes the answer to a knowledge-base question from the retrieved passages only.
// extract() quotes the matching section / lines of the passages word for word.
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnswerComposer {

    public static final String NO_INFORMATION =
            "Mình chưa tìm thấy thông tin này trong tài liệu bạn được phép xem. Bạn thử hỏi cụ thể hơn, hoặc liên hệ quản lý để được hỗ trợ.";

    static final double MIN_TOPICAL_SHARE = 0.5;
    private static final int MAX_EXTRACT_SECTIONS = 2;
    private static final int MAX_EXTRACT_LINES = 10;

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
        String text = extracted != null ? extracted : NO_INFORMATION;
        sink.delta(text);
        return new Answer(text, false, extracted != null);
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
