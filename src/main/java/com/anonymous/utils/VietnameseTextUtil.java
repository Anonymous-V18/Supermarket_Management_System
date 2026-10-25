package com.anonymous.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Text handling for the chatbot. Vietnamese words are space-separated syllables, so syllables (plus pairs of
// them) are the search terms; every term also has an accent-free twin so "hoa don" finds "hóa đơn".
public final class VietnameseTextUtil {

    private static final Locale VI = Locale.forLanguageTag("vi");
    private static final Pattern SYLLABLE = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final String ACCENT_FREE_PREFIX = "a:";

    // Function words and chat filler that say nothing about the topic
    private static final Set<String> STOPWORDS = Set.of(
            "là", "của", "và", "các", "những", "thì", "mà", "cho", "với", "được", "có", "này", "đó", "khi",
            "nào", "gì", "như", "thế", "để", "trong", "một", "bị", "ra", "vào", "tôi", "bạn", "mình", "em",
            "anh", "chị", "ạ", "ơi", "nhé", "vậy", "sao", "hãy", "giúp", "xin", "cần", "muốn", "biết", "hỏi",
            "bao", "nhiêu", "không", "à", "hả", "ở", "đâu", "lại", "cũng", "đã", "sẽ", "đang", "rồi",
            "từ", "đến", "về", "theo", "trên", "dưới", "nếu", "hay", "hoặc", "làm", "phải", "nên", "ai"
    );

    private VietnameseTextUtil() {
    }

    // NFC + lower case + single spaces: the form every comparison is made on
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(VI).replaceAll("\\s+", " ").trim();
    }

    public static String stripDiacritics(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return MARKS.matcher(decomposed).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    public static List<String> syllables(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = SYLLABLE.matcher(normalize(text));
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }

    public static boolean isStopword(String syllable) {
        return STOPWORDS.contains(syllable);
    }

    // Accent-free syllables that carry meaning; used to measure how much of a question a passage covers
    public static Set<String> keywords(String text) {
        return keywords(text, false);
    }

    // keepAccents: compare accented syllables ("thủ đô" must not match "thu ngân"); used when the
    // question itself was typed with accents
    public static Set<String> keywords(String text, boolean keepAccents) {
        Set<String> result = new LinkedHashSet<>();
        for (String syllable : syllables(text)) {
            if (!isStopword(syllable)) {
                result.add(keepAccents ? syllable : stripDiacritics(syllable));
            }
        }
        return result;
    }

    public static boolean hasDiacritics(String text) {
        String normalized = normalize(text);
        return !stripDiacritics(normalized).equals(normalized);
    }

    // Search terms: meaningful syllables, adjacent syllable pairs (phrases such as "hóa đơn"),
    // each in its accented and accent-free form. Repeated terms are kept so callers can count them.
    public static List<String> terms(String text) {
        List<String> syllables = syllables(text);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < syllables.size(); i++) {
            String current = syllables.get(i);
            if (!isStopword(current)) {
                addWithTwin(result, current);
            }
            if (i + 1 < syllables.size()) {
                String next = syllables.get(i + 1);
                if (!(isStopword(current) && isStopword(next))) {
                    addWithTwin(result, current + "_" + next);
                }
            }
        }
        return result;
    }

    public static boolean isAccentFreeTwin(String term) {
        return term.startsWith(ACCENT_FREE_PREFIX);
    }

    private static void addWithTwin(List<String> result, String term) {
        result.add(term);
        result.add(ACCENT_FREE_PREFIX + stripDiacritics(term));
    }

}
