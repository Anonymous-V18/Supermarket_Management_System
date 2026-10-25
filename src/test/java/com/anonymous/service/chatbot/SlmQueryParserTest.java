package com.anonymous.service.chatbot;

import com.anonymous.configuration.ChatbotProperties;
import com.anonymous.constant.Permissions;
import com.anonymous.service.IWarehouseService;
import com.anonymous.service.chatbot.IntentRouter.Kind;
import com.anonymous.service.chatbot.IntentRouter.Route;
import com.anonymous.service.chatbot.tool.ChatTool;
import com.anonymous.service.chatbot.tool.ToolAnswer;
import com.anonymous.service.chatbot.tool.ToolArgs;
import com.anonymous.service.chatbot.tool.ToolRegistry;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlmQueryParserTest {

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

    @Test
    void slmResultConvertsToToolArgsProperly() {
        ChatbotProperties props = new ChatbotProperties();
        SlmQueryParser parser = new SlmQueryParser(props, new ObjectMapper());

        SlmQueryParser.SlmResult result = new SlmQueryParser.SlmResult(
                "revenue",
                "2026-10-21",
                "2026-10-21",
                "Kho Tổng",
                true,
                true,
                false,
                10
        );

        ToolArgs args = parser.toToolArgs(result);
        assertThat(args.from()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(args.to()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(args.keyword()).isEqualTo("Kho Tổng");
        assertThat(args.byWarehouse()).isTrue();
        assertThat(args.chart()).isTrue();
        assertThat(args.limit()).isEqualTo(10);
    }

    @Test
    void sanitizesGenericKeywordsAndNormalizesSingleDay() {
        ChatbotProperties props = new ChatbotProperties();
        SlmQueryParser parser = new SlmQueryParser(props, new ObjectMapper());

        // When SLM hallucinated "dịch vụ" or literal "null" as keyword for single day query
        SlmQueryParser.SlmResult result = new SlmQueryParser.SlmResult(
                "invoice_search",
                "2026-10-21",
                null,
                "dịch vụ",
                false,
                false,
                false,
                null
        );

        ToolArgs args = parser.toToolArgs(result, "Ngày 21/10 có đơn nào không ?", LocalDate.of(2026, 10, 25));
        assertThat(args.from()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(args.to()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(args.keyword()).isNull();

        // When keyword is string "null"
        SlmQueryParser.SlmResult resultNullStr = new SlmQueryParser.SlmResult(
                "invoice_search",
                "null",
                "null",
                "null",
                false,
                false,
                false,
                null
        );
        ToolArgs argsFallback = parser.toToolArgs(resultNullStr, "Ngày 21/10/2026 có đơn nào không ?", LocalDate.of(2026, 10, 25));
        assertThat(argsFallback.from()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(argsFallback.to()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(argsFallback.keyword()).isNull();
    }

    @Test
    void slmDisabledReturnsEmpty() {
        ChatbotProperties props = new ChatbotProperties();
        props.getSlm().setEnabled(false);
        SlmQueryParser parser = new SlmQueryParser(props, new ObjectMapper());

        Optional<SlmQueryParser.SlmResult> parsed = parser.parse("vẽ biểu đồ doanh thu theo kho", LocalDate.now());
        assertThat(parsed).isEmpty();
    }

    @Test
    void routerUsesSlmResultWhenAvailable() {
        ToolRegistry registry = new ToolRegistry(List.of(
                new StubTool("revenue", Permissions.REPORT),
                new StubTool("invoice_search", Permissions.SALES_READ)
        ));
        IWarehouseService warehouseService = mock(IWarehouseService.class);
        when(warehouseService.getAllToSelection()).thenReturn(List.of());

        ChatbotProperties props = new ChatbotProperties();
        props.getSlm().setEnabled(true);
        SlmQueryParser mockParser = mock(SlmQueryParser.class);
        when(mockParser.isEnabled()).thenReturn(true);

        LocalDate date = LocalDate.of(2026, 10, 21);
        SlmQueryParser.SlmResult slmResult = new SlmQueryParser.SlmResult(
                "revenue", "2026-10-21", "2026-10-21", null, true, true, null, null
        );
        when(mockParser.parse("câu hỏi tiếng Việt rất lạ mà regex không bắt được", LocalDate.now())).thenReturn(Optional.of(slmResult));
        when(mockParser.toToolArgs(eq(slmResult), any(), any())).thenReturn(ToolArgs.builder()
                .from(date).to(date).byWarehouse(true).chart(true).build());

        IntentRouter router = new IntentRouter(registry, new WarehouseResolver(warehouseService), mockParser);
        Route route = router.route("câu hỏi tiếng Việt rất lạ mà regex không bắt được", Set.of("ADMIN"));

        assertThat(route.kind()).isEqualTo(Kind.TOOL);
        assertThat(route.tool().name()).isEqualTo("revenue");
        assertThat(route.args().byWarehouse()).isTrue();
        assertThat(route.args().chart()).isTrue();
        assertThat(route.args().from()).isEqualTo(date);
    }

    @Test
    void routerFallsBackToRuleEngineWhenSlmEmpty() {
        ToolRegistry registry = new ToolRegistry(List.of(
                new StubTool("revenue", Permissions.REPORT)
        ));
        IWarehouseService warehouseService = mock(IWarehouseService.class);
        when(warehouseService.getAllToSelection()).thenReturn(List.of());

        SlmQueryParser mockParser = mock(SlmQueryParser.class);
        when(mockParser.isEnabled()).thenReturn(true);
        when(mockParser.parse("Doanh thu tháng này", LocalDate.now())).thenReturn(Optional.empty());

        IntentRouter router = new IntentRouter(registry, new WarehouseResolver(warehouseService), mockParser);
        Route route = router.route("Doanh thu tháng này", Set.of("ADMIN"));

        assertThat(route.kind()).isEqualTo(Kind.TOOL);
        assertThat(route.tool().name()).isEqualTo("revenue");
    }

}
