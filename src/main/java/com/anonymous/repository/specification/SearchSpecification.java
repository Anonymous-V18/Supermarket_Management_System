package com.anonymous.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

@Getter
@AllArgsConstructor
public class SearchSpecification implements Specification<Object> {

    private SearchCriteria searchCriteria;

    @Override
    public Predicate toPredicate(@NonNull Root<Object> root, CriteriaQuery<?> query, @NonNull CriteriaBuilder criteriaBuilder) {
        switch (searchCriteria.getOperation()) {
            case EQUALITY -> criteriaBuilder.equal(root.get(searchCriteria.getKey()), searchCriteria.getValue());
            case NEGATION -> criteriaBuilder.notEqual(root.get(searchCriteria.getKey()), searchCriteria.getValue());
            case GREATER_THAN -> criteriaBuilder.greaterThan(root.get(searchCriteria.getKey()), searchCriteria.getValue().toString());
            case LESS_THAN -> criteriaBuilder.lessThan(root.get(searchCriteria.getKey()), searchCriteria.getValue().toString());
            case LIKE -> criteriaBuilder.like(root.get(searchCriteria.getKey()), "%" + searchCriteria.getValue().toString() + "%");
            case STARTS_WITH -> criteriaBuilder.like(root.get(searchCriteria.getKey()), searchCriteria.getValue() + "%");
            case ENDS_WITH -> criteriaBuilder.like(root.get(searchCriteria.getKey()), "%" + searchCriteria.getValue());
            case CONTAINS -> criteriaBuilder.like(root.get(searchCriteria.getKey()), "%" + searchCriteria.getValue() + "%");
        }
        return null;
    }

}
