package com.anonymous.service.chatbot;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Cuts a document into passages that follow its sections: each passage stays inside one heading, so a
// citation points at a real section and a verbatim answer reads as a whole. Long sections are split on
// paragraph/line boundaries, repeating the last short line so a rule is not cut from its context.
// When tables are split across chunk boundaries, the table header is preserved in subsequent chunks.
@Component
public class DocumentChunker {

    public record Chunk(String headingPath, String content) {
    }

    static final int MAX_CHARS = 1500;
    private static final int OVERLAP_MAX_CHARS = 300;
    private static final int MAX_HEADING_PATH = 500;

    public List<Chunk> chunk(String title, String text) {
        List<Chunk> chunks = new ArrayList<>();
        List<String> headings = new ArrayList<>();
        List<String> section = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.strip();
            int level = headingLevel(trimmed);
            if (level > 0) {
                flush(chunks, pathOf(title, headings), section);
                while (headings.size() >= level) {
                    headings.removeLast();
                }
                while (headings.size() < level - 1) {
                    headings.add("");
                }
                headings.add(trimmed.substring(level).strip());
            } else if (!trimmed.isEmpty()) {
                section.add(trimmed);
            }
        }
        flush(chunks, pathOf(title, headings), section);
        return chunks;
    }

    private static void flush(List<Chunk> chunks, String headingPath, List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        StringBuilder current = new StringBuilder();
        String lastLine = null;
        String activeTableHeader = null;
        for (String line : splitLong(lines)) {
            if (line.startsWith("|") && !line.startsWith("| ---")) {
                if (activeTableHeader == null) {
                    activeTableHeader = line;
                }
            } else if (!line.startsWith("|")) {
                activeTableHeader = null;
            }
            if (!current.isEmpty() && current.length() + line.length() + 1 > MAX_CHARS) {
                chunks.add(new Chunk(headingPath, current.toString()));
                current.setLength(0);
                if (activeTableHeader != null && line.startsWith("|")) {
                    current.append(activeTableHeader).append('\n');
                    current.append("|").append(" --- |".repeat(Math.max(1, activeTableHeader.split("\\|").length - 2)));
                } else if (lastLine != null && lastLine.length() <= OVERLAP_MAX_CHARS) {
                    current.append(lastLine);
                }
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(line);
            lastLine = line;
        }
        chunks.add(new Chunk(headingPath, current.toString()));
        lines.clear();
    }

    // A single line longer than a chunk (PDF paragraphs) is split on sentence ends
    private static List<String> splitLong(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (line.length() <= MAX_CHARS) {
                result.add(line);
                continue;
            }
            StringBuilder piece = new StringBuilder();
            for (String sentence : sentencesOf(line)) {
                if (!piece.isEmpty() && piece.length() + sentence.length() + 1 > MAX_CHARS) {
                    result.add(piece.toString());
                    piece.setLength(0);
                }
                if (!piece.isEmpty()) {
                    piece.append(' ');
                }
                piece.append(sentence);
            }
            if (!piece.isEmpty()) {
                result.add(piece.toString());
            }
        }
        return result;
    }

    // Sentences, with any sentence still longer than a chunk cut into chunk-sized pieces (nothing is dropped)
    private static List<String> sentencesOf(String line) {
        List<String> sentences = new ArrayList<>();
        for (String sentence : line.split("(?<=[.!?;:])\\s+")) {
            for (int from = 0; from < sentence.length(); from += MAX_CHARS) {
                sentences.add(sentence.substring(from, Math.min(from + MAX_CHARS, sentence.length())));
            }
        }
        return sentences;
    }

    private static int headingLevel(String line) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        return level > 0 && level <= 6 && level < line.length() && line.charAt(level) == ' ' ? level : 0;
    }

    private static String pathOf(String title, List<String> headings) {
        List<String> parts = new ArrayList<>();
        parts.add(title);
        headings.stream().filter(heading -> !heading.isBlank() && !heading.equalsIgnoreCase(title)).forEach(parts::add);
        String path = String.join(" > ", parts);
        return path.length() <= MAX_HEADING_PATH ? path : path.substring(0, MAX_HEADING_PATH);
    }

}
