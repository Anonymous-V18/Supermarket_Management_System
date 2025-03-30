package com.anonymous.repository.specification;


import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static com.anonymous.repository.specification.SearchOperation.*;

public final class SpecificationsBuilder {

    public final List<SearchCriteria> params;

    public SpecificationsBuilder() {
        params = new ArrayList<>();
    }
    
    public SpecificationsBuilder with(final String key, final String operation, final Object value, final String prefix, final String suffix) {
        return with(null, key, operation, value, prefix, suffix);
    }

    public SpecificationsBuilder with(final String orPredicate, final String key, final String operation, final Object value, final String prefix, final String suffix) {
        SearchOperation searchOperation = SearchOperation.getSimpleOperation(operation.charAt(0));
        if (searchOperation != null) {
            if (searchOperation == EQUALITY) { // the operation may be complex operation
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
            params.add(new SearchCriteria(key, searchOperation, value, orPredicate));
        }
        return this;
    }

    public SpecificationsBuilder with(SearchCriteria criteria) {
        params.add(criteria);
        return this;
    }

    public SpecificationsBuilder with(SearchSpecification spec) {
        params.add(spec.getSearchCriteria());
        return this;
    }

    public Specification<Object> build() {
        if (params.isEmpty())
            return null;

        Specification<Object> result = new SearchSpecification(params.get(0));

        for (int i = 1; i < params.size(); i++) {
            result = params.get(i).isOrPredicate()
                    ? Specification.where(result).or(new SearchSpecification(params.get(i)))
                    : Specification.where(result).and(new SearchSpecification(params.get(i)));
        }

        return result;
    }

}
