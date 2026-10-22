package com.anonymous.configuration.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditInterceptorTest {

    @Test
    void writesAndSignInsAreAuditedReadsAreNot() {
        assertThat(AuditInterceptor.actionOf("POST", "/invoices/insert")).isEqualTo("CREATE");
        assertThat(AuditInterceptor.actionOf("PUT", "/invoices/update/abc")).isEqualTo("UPDATE");
        assertThat(AuditInterceptor.actionOf("DELETE", "/products/delete")).isEqualTo("DELETE");
        assertThat(AuditInterceptor.actionOf("POST", "/auth/login")).isEqualTo("LOGIN");
        assertThat(AuditInterceptor.actionOf("POST", "/auth/logout")).isEqualTo("LOGOUT");
        assertThat(AuditInterceptor.actionOf("GET", "/invoices/search")).isNull();
        assertThat(AuditInterceptor.actionOf("POST", "/auth/refresh-token")).isNull();
    }

    @Test
    void resourceIsTheFirstPathSegment() {
        assertThat(AuditInterceptor.resourceOf("/sales-returns/update/abc")).isEqualTo("sales-returns");
        assertThat(AuditInterceptor.resourceOf("/")).isEqualTo("/");
    }

}
