package com.anonymous.repository.specification;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import static com.anonymous.repository.specification.SearchOperation.*;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchCriteria {

    String key;
    SearchOperation operation;
    Object value;
    boolean isOrPredicate;

    public SearchCriteria(final String key, final SearchOperation operation, final Object value) {
        super();
        this.key = key;
        this.operation = operation;
        this.value = value;
    }

    public SearchCriteria(final String key, final SearchOperation operation, final Object value, final String orPredicate) {
        super();
        this.isOrPredicate = orPredicate != null && orPredicate.equals(OR_PREDICATE_FLAG);
        this.key = key;
        this.operation = operation;
        this.value = value;
    }

    public SearchCriteria(String key, String operation, String prefix, String value, String suffix) {
        SearchOperation searchOperation = SearchOperation.getSimpleOperation(operation.charAt(0));
        if (searchOperation == EQUALITY) {
            final boolean startWithAsterisk = prefix != null && prefix.contains(ZERO_OR_MORE_REGEX);
            final boolean endWithAsterisk = suffix != null && suffix.contains(ZERO_OR_MORE_REGEX);

            if (startWithAsterisk && endWithAsterisk) {
                searchOperation = CONTAINS;
            } else if (startWithAsterisk) {
                searchOperation = ENDS_WITH;
            } else if (endWithAsterisk) {
                searchOperation = STARTS_WITH;
            }
        }

        this.key = key;
        this.operation = searchOperation;
        this.value = value;
    }

}
