package com.anonymous.service.impl;

import com.anonymous.configuration.ChatbotProperties;
import com.anonymous.dto.request.ChatAskRequest;
import com.anonymous.dto.response.*;
import com.anonymous.entity.ChatConversation;
import com.anonymous.entity.ChatMessage;
import com.anonymous.entity.KbChunk;
import com.anonymous.entity.KbDocument;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IChatConversationRepository;
import com.anonymous.repository.IChatMessageRepository;
import com.anonymous.repository.IKbChunkRepository;
import com.anonymous.repository.IKbDocumentRepository;
import com.anonymous.service.IChatbotService;
import com.anonymous.service.chatbot.*;
import com.anonymous.service.chatbot.IntentRouter.Kind;
import com.anonymous.service.chatbot.IntentRouter.Route;
import com.anonymous.service.chatbot.KnowledgeRetriever.Passage;
import com.anonymous.service.chatbot.tool.ChatTool;
import com.anonymous.service.chatbot.tool.ToolAnswer;
import com.anonymous.service.chatbot.tool.ToolArgs;
import com.anonymous.service.chatbot.tool.ToolRegistry;
import com.anonymous.utils.TtlCache;
import com.anonymous.utils.VietnameseTextUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Answers one question end to end:
//  1. rules (IntentRouter) decide: live data via a tool, a knowledge-base question, small talk, or "not allowed";
//     when unsure it falls back to the documents or asks the user to clarify;
//  2. data answers come from the existing services with the asker's own permissions (@PreAuthorize on tools)
//     and fixed templates; document answers come from role-filtered passages (KnowledgeRetriever / AnswerComposer)
//  3. the answer is streamed (SSE) and stored in the asker's private history
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService implements IChatbotService {

    private static final int HISTORY_TURNS = 6;
    private static final int TITLE_LENGTH = 60;
    private static final long STREAM_TIMEOUT_MILLIS = 90_000;
    private static final Map<String, String[]> HISTORY_LINKS = Map.ofEntries(
            Map.entry("revenue", new String[]{"/home/report", "Đến trang Báo cáo"}),
            Map.entry("top_products", new String[]{"/home/report", "Đến trang Báo cáo"}),
            Map.entry("sales_by_employee", new String[]{"/home/report", "Đến trang Báo cáo"}),
            Map.entry("inventory", new String[]{"/home/report", "Đến trang Báo cáo tồn kho"}),
            Map.entry("expiring_lots", new String[]{"/home/report", "Đến trang Báo cáo tồn kho"}),
            Map.entry("product_lookup", new String[]{"/home/product", "Đến trang Sản phẩm"}),
            Map.entry("invoice_lookup", new String[]{"/home/invoice", "Đến trang Hóa đơn"}),
            Map.entry("sales_return_lookup", new String[]{"/home/sales-return", "Đến trang Quản lý Trả hàng"}),
            Map.entry("my_shift_summary", new String[]{"/home/dashboard", "Đến Tổng quan ca"}),
            Map.entry("customer_lookup", new String[]{"/home/customer", "Đến trang Khách hàng"}),
            Map.entry("sales_return_search", new String[]{"/home/sales-return", "Đến trang Quản lý Trả hàng"}),
            Map.entry("invoice_search", new String[]{"/home/invoice", "Đến trang Quản lý Hóa đơn"}),
            Map.entry("supplier_lookup", new String[]{"/home/supplier", "Đến trang Nhà cung cấp"}),
            Map.entry("employee_lookup", new String[]{"/home/employee", "Đến trang Nhân viên"}),
            Map.entry("warehouse_lookup", new String[]{"/home/warehouse", "Đến trang Kho hàng"}),
            Map.entry("promotion_lookup", new String[]{"/home/product", "Đến trang Khuyến mãi"}),
            Map.entry("shift_checklist", new String[]{"/home/dashboard", "Đến Checklist ca"})
    );
    private static final Map<String, List<String>> DOC_SUGGESTIONS = Map.of(
            "SALESMAN", List.of("Cách tính tiền hoàn khi khách trả hàng?", "Khi nào không hủy được hóa đơn?"),
            "STOREKEEPER", List.of("Quy trình kiểm kê kho gồm những bước nào?", "Phiếu chuyển kho có những trạng thái nào?"),
            "ACCOUNTING_STAFF", List.of("Doanh thu thuần được tính thế nào?", "Báo cáo cho xem tối đa bao nhiêu ngày?"),
            "ADMIN", List.of("Ai được phép hủy chứng từ đã hoàn thành?", "Checklist đầu ca gồm những mục nào?"),
            "EMPLOYEE", List.of("Ca sáng bắt đầu lúc mấy giờ?", "Làm sao để đổi mật khẩu?"));

    private final ChatbotProperties properties;
    private final IntentRouter intentRouter;
    private final KnowledgeRetriever knowledgeRetriever;
    private final AnswerComposer answerComposer;
    private final ToolRegistry toolRegistry;
    private final KnowledgeVersion knowledgeVersion;
    private final ChatRateLimiter rateLimiter;
    private final IChatConversationRepository conversationRepository;
    private final IChatMessageRepository messageRepository;
    private final IKbChunkRepository chunkRepository;
    private final IKbDocumentRepository documentRepository;

    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Document answers only (never live data): same question, same roles, same knowledge base -> same answer
    private final TtlCache<String, CachedAnswer> documentAnswers = new TtlCache<>(500, Duration.ofHours(6));

    private record Caller(String id, Set<String> roleCodes) {
    }

    private record CachedAnswer(String text, List<String> chunkIds) {
    }

    private record Outcome(String intent, String text, List<String> chunkIds, String link, String linkLabel) {

        static Outcome text(String intent, String text) {
            return new Outcome(intent, text, List.of(), null, null);
        }

    }

    @PreDestroy
    void shutdown() {
        workers.shutdownNow();
    }

    @Override
    public ChatCapabilitiesResponse getCapabilities() {
        Caller caller = caller();
        List<ChatTool> tools = toolRegistry.allowedFor(caller.roleCodes());
        List<String> suggestions = new ArrayList<>();
        tools.stream().limit(4).forEach(tool -> suggestions.add(tool.examples().getFirst()));
        caller.roleCodes().stream().sorted()
                .flatMap(role -> DOC_SUGGESTIONS.getOrDefault(role, List.of()).stream())
                .distinct().limit(3).forEach(suggestions::add);
        return ChatCapabilitiesResponse.builder()
                .roleCodes(caller.roleCodes().stream().sorted().toList())
                .tools(tools.stream().map(tool -> ChatToolResponse.builder()
                        .name(tool.name()).description(tool.description()).examples(tool.examples()).build()).toList())
                .suggestions(suggestions)
                .canManageKnowledge(caller.roleCodes().contains("ADMIN"))
                .llmEnabled(false)
                .build();
    }

    @Override
    public SseEmitter ask(ChatAskRequest request) {
        Caller caller = caller();
        if (!rateLimiter.tryAcquire(caller.id())) {
            throw new AppException(ErrorCode.CHAT_RATE_LIMITED);
        }
        String question = request.getQuestion().strip();
        ChatConversation conversation = conversationFor(request.getConversationId(), caller, question);
        List<ChatMessage> recent = new ArrayList<>(messageRepository.findAllByConversationIdOrderByCreatedDateDesc(
                conversation.getId(), PageRequest.of(0, HISTORY_TURNS)));
        Collections.reverse(recent);
        Instant askedAt = Instant.now();
        messageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .role(ChatMessage.ROLE_USER)
                .content(question)
                .createdDate(askedAt)
                .build());

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        // The answer runs on a virtual thread; tools need the caller's security context for @PreAuthorize
        SecurityContext context = SecurityContextHolder.getContext();
        String scope = request.getScope() == null ? "all" : request.getScope();
        workers.submit(() -> {
            SecurityContextHolder.setContext(context);
            try {
                answer(emitter, conversation, question, scope, caller, recent, askedAt);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
        return emitter;
    }

    private void answer(SseEmitter emitter, ChatConversation conversation, String question, String scope, Caller caller,
                        List<ChatMessage> recent, Instant askedAt) {
        long start = System.nanoTime();
        Stream stream = new Stream(emitter);
        Outcome outcome = null;
        try {
            stream.event("meta", Map.of("conversationId", conversation.getId(), "title", conversation.getTitle()));
            outcome = resolve(question, scope, caller, recent, stream);
        } catch (ClientGoneException e) {
            outcome = Outcome.text("cancelled", stream.text().isEmpty() ? "(Đã dừng)" : stream.text() + "\n\n_(Đã dừng)_");
        } catch (Exception e) {
            log.error("Chatbot answer failed", e);
            stream.tryEvent("error", Map.of("message", ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()));
        } finally {
            try {
                if (outcome != null) {
                    int latency = (int) ((System.nanoTime() - start) / 1_000_000);
                    log.info("Chatbot answered intent={} in {} ms", outcome.intent(), latency);
                    String messageId = "";
                    try {
                        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                                .conversationId(conversation.getId())
                                .role(ChatMessage.ROLE_ASSISTANT)
                                .content(outcome.text())
                                .intent(outcome.intent())
                                .citations(outcome.chunkIds().isEmpty() ? null : objectMapper.writeValueAsString(outcome.chunkIds()))
                                .latencyMs(latency)
                                // Strictly after the question, so the history keeps its order
                                .createdDate(Instant.now().isAfter(askedAt) ? Instant.now() : askedAt.plusMillis(1))
                                .build());
                        messageId = saved.getId();
                        conversation.setModifiedDate(saved.getCreatedDate());
                        conversationRepository.save(conversation);
                    } catch (Exception dbEx) {
                        log.error("Failed to persist assistant message to database", dbEx);
                    }
                    stream.tryEvent("done", Map.of("messageId", messageId, "intent", outcome.intent(), "latencyMs", latency));
                }
            } finally {
                try {
                    emitter.complete();
                } catch (Exception ex) {
                    log.debug("Emitter complete failed: {}", ex.getMessage());
                }
            }
        }
    }

    private ConversationContext buildContext(List<ChatMessage> recent) {
        String previousIntent = null;
        String lastPerson = null;
        String lastProduct = null;
        String lastWarehouseId = null;
        String lastWarehouseName = null;

        for (int i = recent.size() - 1; i >= 0; i--) {
            ChatMessage msg = recent.get(i);
            if (previousIntent == null && ChatMessage.ROLE_ASSISTANT.equals(msg.getRole()) && msg.getIntent() != null) {
                if (toolRegistry.find(msg.getIntent()).isPresent()) {
                    previousIntent = msg.getIntent();
                }
            }
            if (lastPerson == null) {
                if (ChatMessage.ROLE_USER.equals(msg.getRole()) && msg.getContent() != null) {
                    String p = IntentRouter.extractPersonName(msg.getContent());
                    if (p != null && !IntentRouter.isPronoun(p)) {
                        lastPerson = p;
                    }
                }
            }
            if (lastProduct == null && ChatMessage.ROLE_USER.equals(msg.getRole()) && msg.getContent() != null) {
                Matcher pCode = Pattern.compile("\\b(SP\\d{5})\\b", Pattern.CASE_INSENSITIVE).matcher(msg.getContent());
                if (pCode.find()) {
                    lastProduct = pCode.group(1).toUpperCase(Locale.ROOT);
                }
            }
        }
        return ConversationContext.builder()
                .previousIntent(previousIntent)
                .lastPersonName(lastPerson)
                .lastProductKeyword(lastProduct)
                .lastWarehouseId(lastWarehouseId)
                .lastWarehouseName(lastWarehouseName)
                .build();
    }

    private Outcome resolve(String question, String scope, Caller caller, List<ChatMessage> recent, Stream stream) {
        List<ChatTool> allowed = toolRegistry.allowedFor(caller.roleCodes());
        ConversationContext context = buildContext(recent);
        Route route = "docs".equals(scope)
                ? new Route(Kind.DOCS, null, null, null)
                : intentRouter.route(question, caller.roleCodes(), context);

        // Document searches get the previous answer's section added to a follow-up ("còn nếu ...?")
        String standalone = FollowUpQuestion.standalone(question, previousDocumentHeading(recent));
        return switch (route.kind()) {
            case SMALLTALK -> {
                String text = smalltalk(allowed);
                stream.delta(text);
                yield Outcome.text("smalltalk", text);
            }
            case DENIED -> denied(allowed, stream);
            case TOOL -> runTool(route.tool(), route.args(), standalone, caller, stream);
            case UNSURE -> {
                Outcome fromDocs = "data".equals(scope) ? null : documents(standalone, caller, stream, true);
                yield fromDocs != null ? fromDocs : clarify(allowed, stream);
            }
            case DOCS -> "data".equals(scope) ? clarify(allowed, stream) : documents(standalone, caller, stream, false);
        };
    }

    // Heading of the first passage cited by the last answer, when that answer came from the documents
    private String previousDocumentHeading(List<ChatMessage> recent) {
        for (int i = recent.size() - 1; i >= 0; i--) {
            ChatMessage message = recent.get(i);
            if (!ChatMessage.ROLE_ASSISTANT.equals(message.getRole())) {
                continue;
            }
            if (!"docs".equals(message.getIntent()) || message.getCitations() == null) {
                return null;
            }
            List<String> chunkIds = parseIds(message.getCitations());
            return chunkIds.isEmpty() ? null
                    : chunkRepository.findById(chunkIds.getFirst()).map(KbChunk::getHeadingPath).orElse(null);
        }
        return null;
    }

    private Outcome runTool(ChatTool tool, ToolArgs args, String standalone, Caller caller, Stream stream) {
        ToolAnswer answer;
        try {
            answer = tool.execute(args == null ? ToolArgs.empty() : args);
        } catch (AccessDeniedException e) {
            return denied(toolRegistry.allowedFor(caller.roleCodes()), stream);
        } catch (AppException e) {
            String text = e.getErrorCode().getMessage();
            stream.delta(text);
            return Outcome.text(tool.name(), text);
        }
        if (!answer.matched()) {
            // Nothing found under that name: maybe it was a policy question after all
            Outcome fromDocs = documents(standalone, caller, stream, true);
            if (fromDocs != null) {
                return fromDocs;
            }
        }
        stream.delta(answer.markdown());
        if (answer.link() != null) {
            stream.event("action", Map.of("link", answer.link(), "label", answer.linkLabel()));
        }
        return new Outcome(tool.name(), answer.markdown(), List.of(), answer.link(), answer.linkLabel());
    }

    // requireHit: return null (and send nothing) when no passage qualifies, so the caller can answer otherwise
    private Outcome documents(String question, Caller caller, Stream stream, boolean requireHit) {
        String cacheKey = knowledgeVersion.current() + "|" + String.join(",", new TreeSet<>(caller.roleCodes())) + "|"
                + VietnameseTextUtil.normalize(question);
        CachedAnswer cached = documentAnswers.get(cacheKey);
        if (cached != null) {
            stream.event("citations", Map.of("items", citationsOf(cached.chunkIds(), caller.roleCodes())));
            stream.delta(cached.text());
            return new Outcome("docs", cached.text(), cached.chunkIds(), null, null);
        }
        KnowledgeRetriever.Retrieval retrieval = knowledgeRetriever.retrieve(question, caller.roleCodes());
        List<Passage> passages = retrieval.passages();
        if (passages.isEmpty() && requireHit) {
            return null;
        }
        List<String> chunkIds = passages.stream().map(passage -> passage.chunk().getId()).toList();
        if (!passages.isEmpty()) {
            stream.event("citations", Map.of("items", citationsOf(passages)));
        }
        AnswerComposer.Answer answer = answerComposer.compose(question, passages, retrieval.topic(), stream);
        if (!answer.answered()) {
            if (!passages.isEmpty()) {
                stream.event("citations", Map.of("items", List.of()));
            }
            return Outcome.text("no_info", answer.text());
        }
        documentAnswers.put(cacheKey, new CachedAnswer(answer.text(), chunkIds));
        return new Outcome("docs", answer.text(), chunkIds, null, null);
    }

    private Outcome denied(List<ChatTool> allowed, Stream stream) {
        StringBuilder text = new StringBuilder("Bạn không có quyền xem dữ liệu này theo phân quyền tài khoản của bạn.");
        if (allowed.stream().anyMatch(tool -> "my_shift_summary".equals(tool.name()))) {
            text.append(" Bạn vẫn có thể xem số liệu bán hàng của chính mình, ví dụ: \"Ca này tôi bán được bao nhiêu?\"");
        }
        stream.delta(text.toString());
        stream.event("suggestions", Map.of("items", allowed.stream().limit(3).map(tool -> tool.examples().getFirst()).toList()));
        return Outcome.text("denied", text.toString());
    }

    private Outcome clarify(List<ChatTool> allowed, Stream stream) {
        String text = "Mình chưa rõ bạn muốn tra cứu gì. Bạn có thể hỏi cụ thể hơn, ví dụ:";
        stream.delta(text);
        stream.event("suggestions", Map.of("items", allowed.stream().limit(4).map(tool -> tool.examples().getFirst()).toList()));
        return Outcome.text("clarify", text);
    }

    private static String smalltalk(List<ChatTool> allowed) {
        StringBuilder text = new StringBuilder("""
                Xin chào! Mình là trợ lý nội bộ của hệ thống siêu thị. Mình có thể:

                - **Giải đáp quy trình, quy định** trong các tài liệu nghiệp vụ bạn được phép xem.
                """);
        if (!allowed.isEmpty()) {
            text.append("- **Tra số liệu thật** trong phạm vi quyền của bạn:\n");
            allowed.forEach(tool -> text.append("  - ").append(tool.description())
                    .append(" (VD: _").append(tool.examples().getFirst()).append("_)\n"));
        }
        return text.toString().strip();
    }

    @Override
    public List<ChatConversationResponse> getConversations() {
        return conversationRepository.findAllByOwnerIdOrderByModifiedDateDesc(caller().id(), PageRequest.of(0, 50)).stream()
                .map(conversation -> ChatConversationResponse.builder()
                        .id(conversation.getId())
                        .title(conversation.getTitle())
                        .createdDate(conversation.getCreatedDate())
                        .modifiedDate(conversation.getModifiedDate())
                        .build())
                .toList();
    }

    @Override
    public List<ChatMessageResponse> getMessages(String conversationId) {
        Caller caller = caller();
        ownedConversation(conversationId, caller);
        return messageRepository.findAllByConversationIdOrderByCreatedDateAsc(conversationId).stream()
                .map(message -> {
                    String[] link = message.getIntent() != null ? HISTORY_LINKS.get(message.getIntent()) : null;
                    return ChatMessageResponse.builder()
                            .id(message.getId())
                            .role(message.getRole())
                            .content(message.getContent())
                            .intent(message.getIntent())
                            .citations(message.getCitations() != null
                                    ? citationsOf(parseIds(message.getCitations()), caller.roleCodes()) : null)
                            .link(link != null ? link[0] : null)
                            .linkLabel(link != null ? link[1] : null)
                            .latencyMs(message.getLatencyMs())
                            .feedback(message.getFeedback())
                            .createdDate(message.getCreatedDate())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public void deleteConversation(String conversationId) {
        conversationRepository.delete(ownedConversation(conversationId, caller()));
    }

    @Override
    public void setFeedback(String messageId, int value) {
        Caller caller = caller();
        ChatMessage message = messageRepository.findById(messageId)
                .filter(found -> ChatMessage.ROLE_ASSISTANT.equals(found.getRole()))
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_MESSAGE_NOT_EXIST));
        // Someone else's message reads as "does not exist"
        conversationRepository.findByIdAndOwnerId(message.getConversationId(), caller.id())
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_MESSAGE_NOT_EXIST));
        message.setFeedback(value == 0 ? null : value);
        messageRepository.save(message);
    }

    // Conversations idle for longer than the retention period are removed with their messages
    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void purgeOldConversations() {
        int removed = conversationRepository.deleteAllModifiedBefore(
                Instant.now().minus(properties.getHistoryRetentionDays(), ChronoUnit.DAYS));
        if (removed > 0) {
            log.info("Removed {} chatbot conversations older than {} days", removed, properties.getHistoryRetentionDays());
        }
    }

    private ChatConversation conversationFor(String conversationId, Caller caller, String question) {
        if (conversationId != null && !conversationId.isBlank()) {
            return ownedConversation(conversationId, caller);
        }
        Instant now = Instant.now();
        String title = question.length() <= TITLE_LENGTH ? question : question.substring(0, TITLE_LENGTH - 1) + "…";
        return conversationRepository.save(ChatConversation.builder()
                .ownerId(caller.id())
                .title(title)
                .createdDate(now)
                .modifiedDate(now)
                .build());
    }

    private ChatConversation ownedConversation(String conversationId, Caller caller) {
        return conversationRepository.findByIdAndOwnerId(conversationId, caller.id())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_EXIST));
    }

    private List<ChatCitationResponse> citationsOf(List<Passage> passages) {
        List<ChatCitationResponse> citations = new ArrayList<>();
        for (int i = 0; i < passages.size(); i++) {
            Passage passage = passages.get(i);
            citations.add(citation(i + 1, passage.chunk(), passage.documentTitle()));
        }
        return citations;
    }

    // Citations of stored answers are re-read live, and only for documents the asker may still read
    private List<ChatCitationResponse> citationsOf(List<String> chunkIds, Set<String> roleCodes) {
        Map<String, KbChunk> chunks = chunkRepository.findAllById(chunkIds).stream()
                .collect(Collectors.toMap(KbChunk::getId, Function.identity()));
        Map<String, KbDocument> documents = documentRepository.findAllByIdInAndIsDeletedFalse(
                        chunks.values().stream().map(KbChunk::getDocumentId).distinct().toList()).stream()
                .collect(Collectors.toMap(KbDocument::getId, Function.identity()));
        List<ChatCitationResponse> citations = new ArrayList<>();
        for (int i = 0; i < chunkIds.size(); i++) {
            KbChunk chunk = chunks.get(chunkIds.get(i));
            KbDocument document = chunk == null ? null : documents.get(chunk.getDocumentId());
            if (document != null && !Collections.disjoint(document.getRoleCodes(), roleCodes)) {
                citations.add(citation(i + 1, chunk, document.getTitle()));
            }
        }
        return citations;
    }

    private static ChatCitationResponse citation(int index, KbChunk chunk, String documentTitle) {
        String content = chunk.getContent();
        return ChatCitationResponse.builder()
                .index(index)
                .chunkId(chunk.getId())
                .documentTitle(documentTitle)
                .headingPath(chunk.getHeadingPath())
                .snippet(content.length() <= 280 ? content : content.substring(0, 280) + "…")
                .build();
    }

    private List<String> parseIds(String json) {
        try {
            return List.of(objectMapper.readValue(json, String[].class));
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    // JWT subject = employee id; roles = ROLE_* authorities from the token (never from the client)
    private static Caller caller() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        Set<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .collect(Collectors.toCollection(TreeSet::new));
        return new Caller(authentication.getToken().getSubject(), roles);
    }

    private static class ClientGoneException extends RuntimeException {
    }

    // Writes SSE events; once the browser has gone (user pressed stop / closed the tab) the answer is cut short
    private class Stream implements AnswerComposer.Sink {

        private final SseEmitter emitter;
        private final StringBuilder text = new StringBuilder();
        private boolean gone;

        Stream(SseEmitter emitter) {
            this.emitter = emitter;
            emitter.onCompletion(() -> gone = true);
            emitter.onTimeout(() -> gone = true);
            emitter.onError(error -> gone = true);
        }

        String text() {
            return text.toString();
        }

        void event(String name, Object data) {
            if (gone) {
                throw new ClientGoneException();
            }
            try {
                emitter.send(SseEmitter.event().name(name).data(objectMapper.writeValueAsString(data), MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                gone = true;
                throw new ClientGoneException();
            }
        }

        void tryEvent(String name, Object data) {
            try {
                event(name, data);
            } catch (ClientGoneException ignored) {
                // Nobody is listening any more; the answer is stored anyway
            }
        }

        @Override
        public void delta(String chunk) {
            text.append(chunk);
            event("delta", Map.of("text", chunk));
        }

        @Override
        public void replace(String replacement) {
            text.setLength(0);
            text.append(replacement);
            event("replace", Map.of("text", replacement));
        }

    }

}
