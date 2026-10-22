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

    // Case-insensitive "contains" over several attributes; "customer.name" walks a LEFT JOIN
    public static <T> Specification<T> keyword(String keyword, String... paths) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(Arrays.stream(paths)
                .map(path -> cb.like(cb.lower(resolve(root, path).as(String.class)), pattern))
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
        // Spring Data 4 rejects null specifications, so start from "always true"
        Specification<T> all = (root, query, cb) -> cb.conjunction();
        return Arrays.stream(specs)
                .filter(spec -> spec != null)
                .reduce(all, Specification::and);
    }

    private static Path<?> resolve(From<?, ?> root, String path) {
        String[] parts = path.split("\\.");
        From<?, ?> from = root;
        for (int i = 0; i < parts.length - 1; i++) {
            from = from.join(parts[i], JoinType.LEFT);
        }
        return from.get(parts[parts.length - 1]);
    }

}
