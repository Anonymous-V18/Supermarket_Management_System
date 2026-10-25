package com.anonymous.service.chatbot;

import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Turns an uploaded file into plain text with Markdown-style headings ("## Title"), which the chunker
// uses to keep every passage attached to the section it came from.
@Component
public class DocumentTextExtractor {

    public record Extracted(String sourceType, String text) {
    }

    private static final Set<String> BLOCK_TAGS = Set.of(
            "p", "div", "section", "article", "main", "header", "footer", "ul", "ol", "li", "dl", "dt", "dd",
            "table", "thead", "tbody", "tfoot", "tr", "blockquote", "pre", "h1", "h2", "h3", "h4", "h5", "h6");

    public Extracted extract(String fileName, byte[] content) {
        String extension = extensionOf(fileName);
        String text = switch (extension) {
            case "md", "markdown" -> decode(content);
            case "txt" -> decode(content);
            case "pdf" -> pdfText(content);
            case "html", "htm" -> htmlText(decode(content));
            default -> throw new AppException(ErrorCode.KB_FILE_TYPE_UNSUPPORTED);
        };
        String sourceType = switch (extension) {
            case "md", "markdown" -> "MD";
            case "pdf" -> "PDF";
            case "html", "htm" -> "HTML";
            default -> "TXT";
        };
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).replace("\r\n", "\n").strip();
        if (normalized.isBlank()) {
            throw new AppException(ErrorCode.KB_FILE_EMPTY);
        }
        return new Extracted(sourceType, normalized);
    }

    private static String extensionOf(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0) {
            throw new AppException(ErrorCode.KB_FILE_TYPE_UNSUPPORTED);
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String decode(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        return text.startsWith("﻿") ? text.substring(1) : text;
    }

    private static String pdfText(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setParagraphStart("\n");
            return stripper.getText(document);
        } catch (IOException e) {
            throw new AppException(ErrorCode.KB_FILE_EMPTY);
        }
    }

    static String htmlText(String html) {
        Element body = Jsoup.parse(html).body();
        body.select("script, style, nav, aside, noscript, template, svg").remove();
        // Boxes and labels laid out side by side (status flows, chips) would otherwise run together
        body.select("span, div, small, td, th").forEach(element -> element.after(" "));
        List<String> lines = new ArrayList<>();
        walk(body, lines);
        return String.join("\n", lines);
    }

    private static void walk(Element element, List<String> lines) {
        String tag = element.tagName();
        if (tag.length() == 2 && tag.charAt(0) == 'h' && Character.isDigit(tag.charAt(1))) {
            add(lines, "#".repeat(tag.charAt(1) - '0') + " " + element.text());
            return;
        }
        switch (tag) {
            case "table" -> {
                // First row is the header; the separator row makes it a Markdown table
                boolean first = true;
                for (Element row : element.select("tr")) {
                    List<String> cells = row.children().stream().map(cell -> cell.text().replace("|", "/")).toList();
                    add(lines, "| " + String.join(" | ", cells) + " |");
                    if (first) {
                        lines.add("|" + " --- |".repeat(cells.size()));
                        first = false;
                    }
                }
                return;
            }
            case "li" -> {
                Element own = element.clone();
                own.select("ul, ol").remove();
                add(lines, "- " + own.text());
                element.select("> ul, > ol").forEach(list -> walk(list, lines));
                return;
            }
            case "dt" -> {
                Element dd = element.nextElementSibling();
                String definition = dd != null && "dd".equals(dd.tagName()) ? ": " + dd.text() : "";
                add(lines, "- " + element.text() + definition);
                return;
            }
            case "dd" -> {
                // Already written together with its <dt>
                Element dt = element.previousElementSibling();
                if (dt == null || !"dt".equals(dt.tagName())) {
                    add(lines, element.text());
                }
                return;
            }
            case "p", "pre", "blockquote" -> {
                add(lines, element.text());
                return;
            }
            default -> {
            }
        }
        if (element.children().stream().noneMatch(child -> BLOCK_TAGS.contains(child.tagName()))) {
            add(lines, element.text());
            return;
        }
        // Mixed container: inline runs between blocks become their own lines
        StringBuilder inline = new StringBuilder();
        for (Node child : element.childNodes()) {
            if (child instanceof Element childElement && BLOCK_TAGS.contains(childElement.tagName())) {
                add(lines, inline.toString());
                inline.setLength(0);
                walk(childElement, lines);
            } else if (child instanceof Element childElement) {
                inline.append(childElement.text()).append(' ');
            } else if (child instanceof TextNode textNode) {
                inline.append(textNode.text());
            }
        }
        add(lines, inline.toString());
    }

    // Blank lines and bare section numbers ("08") carry nothing
    private static void add(List<String> lines, String line) {
        String trimmed = line.replaceAll("\\s+", " ").strip();
        if (!trimmed.isEmpty() && !trimmed.matches("[#\\-|\\s\\d.]*")) {
            lines.add(trimmed);
        }
    }

}
