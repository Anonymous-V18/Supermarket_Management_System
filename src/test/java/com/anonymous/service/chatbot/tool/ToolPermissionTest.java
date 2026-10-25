package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.RevenueByDayResponse;
import com.anonymous.service.IReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// A chatbot tool must be exactly as restricted as the screen it reads from
class ToolPermissionTest {

    private static final List<Class<? extends ChatTool>> TOOLS = List.of(
            ReportTools.RevenueTool.class, ReportTools.TopProductsTool.class, ReportTools.SalesByEmployeeTool.class,
            StockTools.InventoryTool.class, StockTools.ExpiringLotsTool.class, ProductLookupTool.class,
            DocumentLookupTools.InvoiceLookupTool.class, DocumentLookupTools.SalesReturnLookupTool.class,
            DocumentLookupTools.StockDocumentLookupTool.class, MySummaryTool.class);

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void executeCarriesTheSamePermissionTheToolAdvertises() throws Exception {
        for (Class<? extends ChatTool> type : TOOLS) {
            ChatTool tool = instantiate(type);
            Method execute = type.getMethod("execute", ToolArgs.class);
            PreAuthorize rule = execute.getAnnotation(PreAuthorize.class);
            assertThat(rule).as(type.getSimpleName() + " has @PreAuthorize").isNotNull();
            assertThat(rule.value()).as(type.getSimpleName()).isEqualTo(tool.permission());
        }
    }

    @Test
    void rolesAreReadFromThePermissionExpression() {
        assertThat(ToolAccess.rolesOf(Permissions.REPORT)).containsExactly("ADMIN", "ACCOUNTING_STAFF");
        assertThat(ToolAccess.rolesOf(Permissions.AUTHENTICATED)).isNull();
        assertThat(ToolAccess.allows(Permissions.STOCK_READ, Set.of("SALESMAN"))).isFalse();
        assertThat(ToolAccess.allows(Permissions.STOCK_READ, Set.of("EMPLOYEE", "STOREKEEPER"))).isTrue();
        assertThat(ToolAccess.allows(Permissions.AUTHENTICATED, Set.of("EMPLOYEE"))).isTrue();
    }

    @Configuration
    @EnableMethodSecurity
    static class SecuredTools {

        @Bean
        IReportService reportService() {
            IReportService service = mock(IReportService.class);
            when(service.revenueByDay(any(), any(), any())).thenReturn(List.of(RevenueByDayResponse.builder()
                    .date(LocalDate.of(2026, 10, 1)).invoiceCount(2).build()));
            return service;
        }

        @Bean
        ReportTools.RevenueTool revenueTool(IReportService reportService) {
            return new ReportTools.RevenueTool(reportService);
        }

    }

    // Spring's method security really stops a cashier, even if the router were wrong
    @Test
    void methodSecurityBlocksRolesWithoutAccess() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SecuredTools.class)) {
            // The secured bean is a proxy of the ChatTool interface
            ChatTool tool = context.getBean(ChatTool.class);

            signIn("ROLE_SALESMAN");
            assertThatThrownBy(() -> tool.execute(ToolArgs.empty())).isInstanceOf(AccessDeniedException.class);

            signIn("ROLE_ACCOUNTING_STAFF");
            assertThat(tool.execute(ToolArgs.empty()).markdown()).contains("Số hóa đơn hoàn thành");
        }
    }

    private static void signIn(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    // permission() does not touch the dependencies, so nulls are enough
    private static ChatTool instantiate(Class<? extends ChatTool> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        return (ChatTool) constructor.newInstance(new Object[constructor.getParameterCount()]);
    }

}
