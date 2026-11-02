package com.anonymous.service.chatbot;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicSqlAgentTest {

    @Test
    void rbacBlocksUnauthorizedTablesAndAllowsAuthorizedTables() {
        // SALESMAN can query invoice, product, customer, but NOT employee, user, or stock_in
        assertThat(DynamicSqlAgent.checkSecurityAndRbac(
                "SELECT code, total_price FROM invoice WHERE is_deleted = 0 LIMIT 100",
                Set.of("SALESMAN"))).isEmpty();

        assertThat(DynamicSqlAgent.checkSecurityAndRbac(
                "SELECT code, name FROM employee WHERE is_deleted = 0",
                Set.of("SALESMAN"))).contains("DENIED");

        assertThat(DynamicSqlAgent.checkSecurityAndRbac(
                "SELECT code, total_price FROM stock_in WHERE is_deleted = 0",
                Set.of("SALESMAN"))).contains("DENIED");

        // STOREKEEPER can query stock_in, stock_out, stock_transfer, stock_take, warehouse_product
        assertThat(DynamicSqlAgent.checkSecurityAndRbac(
                "SELECT so.code, w.name FROM stock_out so JOIN warehouse w ON so.warehouse_id = w.id WHERE so.is_deleted = 0",
                Set.of("STOREKEEPER"))).isEmpty();

        assertThat(DynamicSqlAgent.checkSecurityAndRbac(
                "SELECT code, total_price FROM invoice WHERE is_deleted = 0",
                Set.of("STOREKEEPER"))).contains("DENIED");

        // ADMIN can query all business tables
        assertThat(DynamicSqlAgent.checkSecurityAndRbac(
                "SELECT code, name FROM employee WHERE is_deleted = 0",
                Set.of("ADMIN"))).isEmpty();
    }

    @Test
    void sqlGuardBlocksMutationsSensitiveColumnsAndMultiStatements() {
        assertThat(DynamicSqlAgent.checkSecurityAndRbac(
                "DELETE FROM product WHERE id = '1'",
                Set.of("ADMIN"))).isPresent();

        assertThat(DynamicSqlAgent.checkSecurityAndRbac(
                "SELECT username, password FROM user",
                Set.of("ADMIN"))).contains("SENSITIVE_COLUMN");

        assertThat(DynamicSqlAgent.checkSecurityAndRbac(
                "SELECT code FROM product; DROP TABLE product",
                Set.of("ADMIN"))).contains("MULTI_STATEMENT");
    }

    @Test
    void sanitizeSqlAppendsLimitWhenMissing() {
        String s = DynamicSqlAgent.sanitizeSql("SELECT code, name FROM product WHERE is_deleted = 0;");
        assertThat(s).isEqualTo("SELECT code, name FROM product WHERE is_deleted = 0 LIMIT 1000");
    }
}
