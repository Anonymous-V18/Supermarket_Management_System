package com.anonymous.service.chatbot;

import com.anonymous.utils.VietnameseTextUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// What a question is about, and how much of it a text covers, without any LLM.
// Topic units are the question's meaningful syllables (minus words that only shape a question: "cách",
// "quy trình", "bước", "ai được", ...) and pairs of adjacent ones ("tiền hoàn", "kiểm kê"): a Vietnamese
// word is often two syllables and a single syllable such as "hoàn" also appears in "hoàn thành".
// With a corpus (the search candidates) each unit is weighted by how rare it is among them, so the words
// that tell passages apart count more than words every passage contains ("hàng", "kho").
public final class TopicMatcher {

    private static final Set<String> QUESTION_FRAME = Set.copyOf(List.of(
            "cách", "quy", "trình", "gồm", "bước", "thế", "nào", "tính", "làm", "sao", "hướng", "dẫn", "mấy", "giờ",
            "lúc", "những", "các", "khi", "nếu", "như", "gì", "là", "nghĩa", "tại", "vì", "thông",
            "tin", "nhập", "cần", "trường", "hợp", "điều", "kiện", "ý", "định", "được", "bắt", "đầu", "ai", "khác",
            "nhau", "để", "phải", "có", "không", "thì", "mà", "nên", "của", "mới"));
    private static final Set<String> QUESTION_FRAME_ACCENT_FREE = QUESTION_FRAME.stream()
            .map(VietnameseTextUtil::stripDiacritics).collect(Collectors.toSet());
    private static final double PAIR_WEIGHT = 1.5;
    private static final double SYNONYM_MATCH_DISCOUNT = 0.95;

    private final boolean accented;
    private final Map<String, Double> weights = new LinkedHashMap<>();
    // Before rarity weighting: for headings, where a common word ("hóa đơn") still names the section's subject
    private final Map<String, Double> baseWeights = new LinkedHashMap<>();

    public TopicMatcher(String question) {
        this.accented = VietnameseTextUtil.hasDiacritics(question);
        List<String> syllables = forms(VietnameseTextUtil.syllables(question));
        String previous = null;
        for (String syllable : syllables) {
            // Bare numbers (dates, amounts) are not a topic: "22/10/2026" must not pull in a page footer dated that day
            boolean topical = !VietnameseTextUtil.isStopword(syllable) && !VietnameseTextUtil.isStopword(VietnameseTextUtil.stripDiacritics(syllable))
                    && !QUESTION_FRAME.contains(syllable) && !QUESTION_FRAME_ACCENT_FREE.contains(syllable)
                    && !syllable.chars().allMatch(Character::isDigit);
            if (topical) {
                weights.putIfAbsent(syllable, 1.0);
                if (previous != null) {
                    weights.putIfAbsent(previous + " " + syllable, PAIR_WEIGHT);
                }
                previous = syllable;
            } else {
                previous = null;
            }
        }
        baseWeights.putAll(weights);
    }

    // Weights each unit by its rarity among the given texts (inverse document frequency)
    public TopicMatcher withCorpus(Collection<String> texts) {
        if (texts.isEmpty() || weights.isEmpty()) {
            return this;
        }
        List<Set<String>> units = texts.stream().map(this::unitsOf).toList();
        int n = units.size();
        weights.replaceAll((unit, weight) -> {
            long df = units.stream().filter(set -> set.contains(unit)).count();
            double idf = Math.log((n + 1.0) / (df + 0.5));
            return weight * Math.clamp(idf, 0.05, 3.0);
        });
        return this;
    }

    public boolean isEmpty() {
        return weights.isEmpty();
    }

    // Topic syllables (not counting the pairs of them)
    public int syllableCount() {
        return (int) baseWeights.keySet().stream().filter(unit -> !unit.contains(" ")).count();
    }

    // Weighted share of the topic found in the text (1 when the question has no topic words)
    public double share(String text) {
        return share(text, weights);
    }

    // Share of the topic in a heading, every unit counted at its base weight
    public double headingShare(String heading) {
        return share(heading, baseWeights);
    }

    private double share(String text, Map<String, Double> weights) {
        if (weights.isEmpty()) {
            return 1;
        }
        Set<String> units = unitsOf(text);
        double found = 0;
        double total = 0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            total += entry.getValue();
            String key = entry.getKey();
            if (units.contains(key)) {
                found += entry.getValue();
            } else {
                // Check DomainThesaurus for non-LLM synonym matches
                boolean synFound = false;
                for (String syn : DomainThesaurus.synonymsOf(key)) {
                    String formatted = accented ? syn : VietnameseTextUtil.stripDiacritics(syn);
                    if (units.contains(formatted)) {
                        synFound = true;
                        break;
                    }
                }
                if (synFound) {
                    found += entry.getValue() * SYNONYM_MATCH_DISCOUNT;
                }
            }
        }
        return total == 0 ? 1 : found / total;
    }

    // Share of the topic in the best-matching line of the text.
    // Table rows are combined with their header line so column headers count towards matching cells.
    public double bestLineShare(String text) {
        if (weights.isEmpty() || text == null || text.isBlank()) {
            return 0;
        }
        double best = 0;
        String currentTableHeader = null;
        for (String line : text.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("| ---")) {
                continue;
            }
            if (trimmed.startsWith("|")) {
                if (currentTableHeader == null) {
                    currentTableHeader = trimmed;
                    best = Math.max(best, share(trimmed));
                } else {
                    best = Math.max(best, share(currentTableHeader + " " + trimmed));
                }
            } else {
                currentTableHeader = null;
                best = Math.max(best, share(trimmed));
            }
        }
        return best;
    }

    private Set<String> unitsOf(String text) {
        List<String> syllables = forms(VietnameseTextUtil.syllables(text));
        Set<String> units = new HashSet<>(syllables);
        for (int i = 0; i + 1 < syllables.size(); i++) {
            units.add(syllables.get(i) + " " + syllables.get(i + 1));
        }
        return units;
    }

    // Accented comparison when the question was typed with accents ("thủ đô" is not "thu ngân")
    private List<String> forms(List<String> syllables) {
        if (accented) {
            return syllables;
        }
        List<String> plain = new ArrayList<>(syllables.size());
        syllables.forEach(syllable -> plain.add(VietnameseTextUtil.stripDiacritics(syllable)));
        return plain;
    }

}
