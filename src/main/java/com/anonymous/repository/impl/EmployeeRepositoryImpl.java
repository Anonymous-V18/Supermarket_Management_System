package com.anonymous.repository.impl;

import com.anonymous.constant.QueryConst;
import com.anonymous.entity.Employee;
import com.anonymous.repository.criteria.SearchCriteria;
import com.anonymous.repository.criteria.SearchCriteriaConsumer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EmployeeRepositoryImpl {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Employee> searchCriteria(int page, int limit, String sortBy, String... search) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Employee> criteriaQuery = criteriaBuilder.createQuery(Employee.class);
        Root<Employee> root = criteriaQuery.from(Employee.class);

        Predicate predicate = criteriaBuilder.conjunction();
        List<SearchCriteria> searchCriteriaList = new ArrayList<>();
        if (search != null && search.length > 0) {
            for (String s : search) {
                Pattern pattern = Pattern.compile(QueryConst.SEARCH_OPERATION);
                Matcher matcher = pattern.matcher(s);
                if (matcher.find()) {
                    searchCriteriaList.add(new SearchCriteria(matcher.group(1), matcher.group(2), matcher.group(3)));
                }
            }

            SearchCriteriaConsumer searchCriteriaConsumer = new SearchCriteriaConsumer(criteriaBuilder, predicate, root);
            searchCriteriaList.forEach(searchCriteriaConsumer);
            predicate = searchCriteriaConsumer.getPredicate();
        }
        criteriaQuery.where(predicate);

        Pattern pattern = Pattern.compile(QueryConst.SORT_OPERATION);
        Matcher matcher = pattern.matcher(sortBy);
        if (matcher.find()) {
            String fieldName = matcher.group(1);
            String value = matcher.group(3);
            if (value.equals("ASC")) {
                criteriaQuery.orderBy(criteriaBuilder.asc(root.get(fieldName)));
            } else {
                criteriaQuery.orderBy(criteriaBuilder.desc(root.get(fieldName)));
            }
        }

        return entityManager.createQuery(criteriaQuery)
                .setFirstResult((page - 1) * limit)
                .setMaxResults(limit)
                .getResultList();
    }

}
