package com.anonymous.service;

import com.anonymous.dto.request.ChatAskRequest;
import com.anonymous.dto.response.ChatCapabilitiesResponse;
import com.anonymous.dto.response.ChatConversationResponse;
import com.anonymous.dto.response.ChatMessageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface IChatbotService {

    ChatCapabilitiesResponse getCapabilities();

    // Streams the answer as server-sent events: meta, delta*, replace?, citations?, action?, suggestions?, done | error
    SseEmitter ask(ChatAskRequest request);

    List<ChatConversationResponse> getConversations();

    List<ChatMessageResponse> getMessages(String conversationId);

    void deleteConversation(String conversationId);

    void setFeedback(String messageId, int value);

}
