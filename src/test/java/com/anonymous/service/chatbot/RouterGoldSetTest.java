package com.anonymous.service.chatbot;

import com.anonymous.constant.Permissions;
import com.anonymous.service.IWarehouseService;
import com.anonymous.service.chatbot.IntentRouter.Kind;
import com.anonymous.service.chatbot.IntentRouter.Route;
import com.anonymous.service.chatbot.tool.ChatTool;
import com.anonymous.service.chatbot.tool.ToolAnswer;
import com.anonymous.service.chatbot.tool.ToolArgs;
import com.anonymous.service.chatbot.tool.ToolRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Replays rag-eval/router-gold.jsonl through the router and reports the share routed as expected.
// Add a line per question that was answered wrongly; accuracy may never drop below MIN_ACCURACY.
class RouterGoldSetTest {

    private static final double MIN_ACCURACY = 1.0;

    private record StubTool(String name, String permission) implements ChatTool {

        @Override
        public String description() {
            return name;
        }

        @Override
        public List<String> examples() {
            return List.of(name);
        }

        @Override
        public ToolAnswer execute(ToolArgs args) {
            return ToolAnswer.of(name, null, null);
        }

    }

    private IntentRouter router() {
        ToolRegistry registry = new ToolRegistry(List.of(
                new StubTool("revenue", Permissions.REPORT),
                new StubTool("top_products", Permissions.REPORT),
                new StubTool("sales_by_employee", Permissions.REPORT),
                new StubTool("inventory", Permissions.STOCK_READ),
                new StubTool("expiring_lots", Permissions.STOCK_READ),
                new StubTool("product_lookup", Permissions.AUTHENTICATED),
                new StubTool("invoice_lookup", Permissions.SALES_READ),
                new StubTool("sales_return_lookup", Permissions.SALES_READ),
                new StubTool("stock_document_lookup", Permissions.STOCK_READ),
                new StubTool("my_shift_summary", Permissions.AUTHENTICATED),
                new StubTool("invoice_stats", Permissions.SALES_READ),
                new StubTool("data_overview", Permissions.AUTHENTICATED),
                new StubTool("customer_lookup", Permissions.SALES_READ),
                new StubTool("sales_return_search", Permissions.SALES_READ),
                new StubTool("invoice_search", Permissions.SALES_READ),
                new StubTool("supplier_lookup", Permissions.STOCK_READ),
                new StubTool("employee_lookup", Permissions.ADMIN),
                new StubTool("warehouse_lookup", Permissions.STOCK_READ),
                new StubTool("promotion_lookup", Permissions.SALES_READ),
                new StubTool("shift_staff", Permissions.AUTHENTICATED),
                new StubTool("shift_checklist", Permissions.AUTHENTICATED)));
        IWarehouseService warehouseService = mock(IWarehouseService.class);
        when(warehouseService.getAllToSelection()).thenReturn(List.of());
        return new IntentRouter(registry, new WarehouseResolver(warehouseService));
    }

    @Test
    void goldQuestionsAreRoutedAsExpected() throws Exception {
        IntentRouter router = router();
        ObjectMapper mapper = new ObjectMapper();
        List<String> misses = new ArrayList<>();
        int total = 0;
        try (InputStream in = getClass().getResourceAsStream("/rag-eval/router-gold.jsonl");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode gold = mapper.readTree(line);
                Set<String> roles = new HashSet<>();
                gold.get("roles").forEach(role -> roles.add(role.asString()));
                Route route = router.route(gold.get("q").asString(), roles);
                Kind expectedKind = Kind.valueOf(gold.get("kind").asString());
                String expectedTool = gold.has("tool") ? gold.get("tool").asString() : null;
                String actualTool = route.tool() != null ? route.tool().name() : route.deniedTool();
                total++;
                if (route.kind() != expectedKind || (expectedTool != null && !expectedTool.equals(actualTool))) {
                    misses.add(gold.get("q").asString() + " -> " + route.kind() + "/" + actualTool
                            + " (expected " + expectedKind + "/" + expectedTool + ")");
                }
            }
        }
        double accuracy = total == 0 ? 0 : (double) (total - misses.size()) / total;
        System.out.printf("router gold accuracy: %.3f (%d/%d)%n", accuracy, total - misses.size(), total);
        misses.forEach(miss -> System.out.println("  MISS " + miss));
        assertThat(total).isPositive();
        assertThat(accuracy).as("misses: %s", misses).isGreaterThanOrEqualTo(MIN_ACCURACY);
    }

}
