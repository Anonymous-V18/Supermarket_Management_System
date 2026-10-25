package com.anonymous.service.chatbot;

import com.anonymous.utils.VietnameseTextUtil;

import java.util.List;
import java.util.regex.Pattern;

// Makes a follow-up question searchable on its own. "Còn nếu khách không có hóa đơn?" right after an answer
// from "Luồng trả hàng > Quy tắc & lỗi" says nothing about returns by itself, so the section the previous
// answer came from is appended to it. Only for document questions; the router still sees the raw question.
public final class FollowUpQuestion {

    // Openers that continue the previous turn ("còn ...", "vậy ...", "thế thì ...", "nếu ...")
    private static final Pattern CONTINUATION = Pattern.compile(
            "^(còn|vậy|thế|thì|nếu|nhưng|rồi|sau đó|tiếp theo|bước tiếp|con|vay|neu|nhung|roi|sau do|tiep theo|buoc tiep)\\b.*",
            Pattern.UNICODE_CHARACTER_CLASS);
    // Words pointing back at what was just said (accented only: "do", "ay" are ordinary words without accents)
    private static final Pattern REFERENCE = Pattern.compile(
            ".*\\b(đó|ấy|vừa rồi|bước sau|bước tiếp theo|vua roi|buoc sau|buoc tiep theo)\\b.*",
            Pattern.UNICODE_CHARACTER_CLASS);
    // Sections that are only the last parts of the path: the document title alone is too broad
    private static final int HEADING_PARTS = 2;

    private FollowUpQuestion() {
    }

    // previousHeading: heading path of the first passage cited by the previous answer (null when it was not a
    // document answer)
    public static String standalone(String question, String previousHeading) {
        if (previousHeading == null || previousHeading.isBlank() || !isFollowUp(question)) {
            return question;
        }
        List<String> parts = List.of(previousHeading.split(" > "));
        String section = String.join(" ", parts.subList(Math.max(0, parts.size() - HEADING_PARTS), parts.size()));
        return question + "\n" + section;
    }

    static boolean isFollowUp(String question) {
        String text = VietnameseTextUtil.normalize(question);
        return CONTINUATION.matcher(text).matches() || REFERENCE.matcher(text).matches()
                || new TopicMatcher(question).syllableCount() <= 1;
    }

}
