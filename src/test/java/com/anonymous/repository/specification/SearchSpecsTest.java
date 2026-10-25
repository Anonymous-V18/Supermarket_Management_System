package com.anonymous.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchSpecsTest {

    @Test
    void keywordDirectAttributeCallsGetInsteadOfJoin() {
        Specification<Object> spec = SearchSpecs.keyword("Thảo", "code", "name");
        assertThat(spec).isNotNull();

        Root<Object> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> codePath = mock(Path.class);
        Path<Object> namePath = mock(Path.class);
        when(root.get("code")).thenReturn(codePath);
        when(root.get("name")).thenReturn(namePath);

        Expression<String> expr = mock(Expression.class);
        when(codePath.as(String.class)).thenReturn(expr);
        when(namePath.as(String.class)).thenReturn(expr);
        when(cb.lower(any())).thenReturn(expr);

        Predicate p = mock(Predicate.class);
        when(cb.like(any(), anyString())).thenReturn(p);
        when(cb.and(any(Predicate[].class))).thenReturn(p);
        when(cb.or(any(Predicate[].class))).thenReturn(p);

        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();

        // Must call root.get("code"), NEVER root.join("code", ...)
        verify(root).get("code");
        verify(root).get("name");
        verify(root, never()).join(eq("code"), any(JoinType.class));
        verify(root, never()).join(eq("name"), any(JoinType.class));
    }

    @Test
    void keywordNestedAttributeJoinsParentAndGetsChild() {
        Specification<Object> spec = SearchSpecs.keyword("Thảo", "customer.name");
        assertThat(spec).isNotNull();

        Root<Object> root = mock(Root.class);
        Join<Object, Object> customerJoin = mock(Join.class);
        when(root.join("customer", JoinType.LEFT)).thenReturn(customerJoin);

        Path<Object> namePath = mock(Path.class);
        when(customerJoin.get("name")).thenReturn(namePath);

        Expression<String> expr = mock(Expression.class);
        when(namePath.as(String.class)).thenReturn(expr);

        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        when(cb.lower(any())).thenReturn(expr);

        Predicate p = mock(Predicate.class);
        when(cb.like(any(), anyString())).thenReturn(p);
        when(cb.and(any(Predicate[].class))).thenReturn(p);
        when(cb.or(any(Predicate[].class))).thenReturn(p);

        Predicate result = spec.toPredicate(root, query, cb);
        assertThat(result).isNotNull();

        verify(root).join("customer", JoinType.LEFT);
        verify(customerJoin).get("name");
    }

}
