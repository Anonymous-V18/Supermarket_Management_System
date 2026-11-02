package com.anonymous.service;

import com.anonymous.dto.request.ChatAskRequest;
import com.anonymous.dto.response.ChatCapabilitiesResponse;
import com.anonymous.dto.response.ChatConversationResponse;
import com.anonymous.dto.response.ChatMessageResponse;
import com.anonymous.service.chatbot.SlmQueryParser.AiConfigStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface IChatbotService {

    ChatCapabilitiesResponse getCapabilities();

    AiConfigStatus getAiConfig();

    AiConfigStatus updateAiConfig(Map<String, Object> request);

    // Streams the answer as server-sent events: meta, delta*, replace?, citations?, action?, suggestions?, done | error
    SseEmitter ask(ChatAskRequest request);

    List<ChatConversationResponse> getConversations();

    List<ChatMessageResponse> getMessages(String conversationId);

    void deleteConversation(String conversationId);

    void setFeedback(String messageId, int value);

}
