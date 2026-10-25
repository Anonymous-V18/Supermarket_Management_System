package com.anonymous.service.chatbot;

import com.anonymous.utils.VietnameseTextUtil;

import java.util.*;

/**
 * Domain-specific thesaurus for the Supermarket Knowledge Base (Non-LLM semantic expansion).
 * Bridges terminology gaps between user colloquial phrasings and formal functional specifications.
 */
public final class DomainThesaurus {

    private static final List<Set<String>> SYNONYM_GROUPS = List.of(
            // Delete / Soft delete / Physical delete
            Set.of("vĩnh viễn", "vật lý", "xóa cứng", "xóa vật lý", "xóa vĩnh viễn", "vĩnh", "vật", "viễn", "lý", "xóa vĩnh", "xóa vật"),
            // Default / initial password
            Set.of("ban đầu", "mặc định", "lần đầu", "ban", "mặc", "khẩu ban", "khẩu mặc"),
            // Resignation / dismissal
            Set.of("nghỉ việc", "cho nghỉ", "thôi việc", "nghỉ"),
            // Shift checklist / tasks to do
            Set.of("việc cần làm", "checklist", "nhiệm vụ đầu ca", "công việc đầu ca", "nhiệm vụ", "công việc"),
            // Pending status
            Set.of("đang chờ", "chờ xử lý"),
            // Stock deduction / stock effect
            Set.of("trừ tồn kho", "trừ tồn", "ảnh hưởng tồn kho", "tính vào tồn kho", "giữ hàng"),
            // Expiry / near expiry
            Set.of("sắp hết hạn", "cận date", "hết hạn", "hạn sử dụng", "hạn dùng", "hàng sắp"),
            // Business code format / prefix
            Set.of("tiền tố", "tiền tố mã", "mã nghiệp vụ", "ký hiệu mã", "dạng", "định dạng"),
            // Supplier
            Set.of("nhà cung cấp", "ncc"),
            // Customer order / invoice
            Set.of("đơn hàng", "hóa đơn", "hóa đơn bán")
    );

    private static final Map<String, Set<String>> SYNONYM_MAP = new HashMap<>();

    static {
        for (Set<String> group : SYNONYM_GROUPS) {
            Set<String> normalizedGroup = new HashSet<>();
            for (String phrase : group) {
                normalizedGroup.add(VietnameseTextUtil.normalize(phrase));
            }
            for (String phrase : normalizedGroup) {
                Set<String> synonyms = new HashSet<>(normalizedGroup);
                synonyms.remove(phrase);
                SYNONYM_MAP.computeIfAbsent(phrase, k -> new HashSet<>()).addAll(synonyms);
            }
        }
    }

    private DomainThesaurus() {
    }

    /**
     * Checks if two phrases or terms are registered synonyms.
     */
    public static boolean areSynonyms(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String normA = VietnameseTextUtil.normalize(a);
        String normB = VietnameseTextUtil.normalize(b);
        if (normA.equals(normB)) {
            return true;
        }
        Set<String> syns = SYNONYM_MAP.get(normA);
        return syns != null && syns.contains(normB);
    }

    /**
     * Returns all registered synonyms for a phrase or term.
     */
    public static Set<String> synonymsOf(String term) {
        if (term == null) {
            return Set.of();
        }
        Set<String> syns = SYNONYM_MAP.get(VietnameseTextUtil.normalize(term));
        return syns != null ? syns : Set.of();
    }

    /**
     * Extracts all synonym expansions matching any phrase in the query.
     */
    public static List<String> synonymsForQuery(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalized = VietnameseTextUtil.normalize(query);
        List<String> results = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : SYNONYM_MAP.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                results.addAll(entry.getValue());
            }
        }
        return results;
    }

}
