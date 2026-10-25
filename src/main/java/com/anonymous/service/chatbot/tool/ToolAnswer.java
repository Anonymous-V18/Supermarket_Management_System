package com.anonymous.service.chatbot.tool;

// Markdown built from service results by a fixed template, plus the screen that shows the same data.
// matched = false when the question named nothing the tool could find (the chatbot may then try the
// knowledge base instead, e.g. "giảm giá hóa đơn tối đa bao nhiêu" is not a product).
public record ToolAnswer(String markdown, String link, String linkLabel, boolean matched) {

    public static ToolAnswer of(String markdown, String link, String linkLabel) {
        return new ToolAnswer(markdown, link, cleanLabel(linkLabel), true);
    }

    public static ToolAnswer notMatched(String markdown, String link, String linkLabel) {
        return new ToolAnswer(markdown, link, cleanLabel(linkLabel), false);
    }

    private static String cleanLabel(String label) {
        if (label == null) return null;
        return label.replaceFirst("^[🔗👉📥\\s]+", "").trim();
    }

}
