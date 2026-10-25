package com.anonymous.repository.specification;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Locale;

// Building blocks for the paged "search" endpoints
public final class SearchSpecs {

    private SearchSpecs() {
    }

    public static <T> Specification<T> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    // Case-insensitive "contains" over several attributes; "customer.name" walks a LEFT JOIN.
    // Multi-word keywords (e.g. "Đỗ Thảo") match if all words appear in the attribute (e.g. "Đỗ Kim Thảo").
    public static <T> Specification<T> keyword(String keyword, String... paths) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String[] words = keyword.trim().toLowerCase(Locale.ROOT).split("\\s+");
        return (root, query, cb) -> cb.or(Arrays.stream(paths)
                .map(path -> {
                    var expr = cb.lower(resolve(root, path).as(String.class));
                    Predicate[] wordPreds = Arrays.stream(words)
                            .map(w -> cb.like(expr, "%" + w + "%"))
                            .toArray(Predicate[]::new);
                    return cb.and(wordPreds);
                })
                .toArray(Predicate[]::new));
    }

    public static <T> Specification<T> statusCode(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("statusInvoice").get("code"), statusCode);
    }

    @SafeVarargs
    public static <T> Specification<T> allOf(Specification<T>... specs) {
        Specification<T> combined = (root, query, cb) -> cb.conjunction();
        for (Specification<T> spec : specs) {
            if (spec != null) {
                combined = combined.and(spec);
            }
        }
        return combined;
    }

    private static Path<?> resolve(Path<?> root, String path) {
        String[] parts = path.split("\\.");
        if (!(root instanceof From<?, ?> from)) {
            Path<?> current = root;
            for (String part : parts) {
                current = current.get(part);
            }
            return current;
        }
        for (int i = 0; i < parts.length - 1; i++) {
            from = from.join(parts[i], JoinType.LEFT);
        }
        return from.get(parts[parts.length - 1]);
    }

}
