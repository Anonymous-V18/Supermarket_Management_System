package com.anonymous.repository.criteria;

import com.anonymous.constant.QueryConst;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.function.Consumer;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchCriteriaConsumer implements Consumer<SearchCriteria> {

    CriteriaBuilder criteriaBuilder;
    Predicate predicate;
    Root<?> root;

    @Override
    public void accept(SearchCriteria searchCriteria) {
        switch (searchCriteria.getOperation()) {
            case "~" -> predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.like(
                            root.get(searchCriteria.getKey()).as(String.class),
                            String.format(QueryConst.LIKE_FORMAT, searchCriteria.getValue().toString().trim())
                    )

            );
            case ">" -> predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.greaterThan(
                            root.get(searchCriteria.getKey()).as(String.class),
                            searchCriteria.getValue().toString()
                    )
            );
            case "<" -> predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.lessThan(
                            root.get(searchCriteria.getKey()).as(String.class),
                            searchCriteria.getValue().toString()
                    )
            );
            case "!" -> predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.notEqual(
                            root.get(searchCriteria.getKey()).as(String.class),
                            searchCriteria.getValue()
                    )
            );
            case ":" -> predicate = criteriaBuilder.and(
                    predicate,
                    criteriaBuilder.equal(
                            root.get(searchCriteria.getKey()).as(String.class),
                            searchCriteria.getValue()
                    )
            );
            default -> throw new AppException(ErrorCode.OPERATION_NOT_SUPPORTED);
        }
    }
}
