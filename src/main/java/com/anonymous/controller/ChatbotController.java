package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.ChatAskRequest;
import com.anonymous.dto.request.ChatFeedbackRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.ChatCapabilitiesResponse;
import com.anonymous.dto.response.ChatConversationResponse;
import com.anonymous.dto.response.ChatMessageResponse;
import com.anonymous.service.IChatbotService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

// Every signed-in user has a chatbot; what it may answer is limited inside by the user's roles
// (tools carry the same @PreAuthorize rules as the screens, documents are filtered by allowed roles).
// History is private: a conversation is only reachable by its owner.
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatbotController {

    IChatbotService chatbotService;

    @GetMapping("/capabilities")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<ChatCapabilitiesResponse> getCapabilities() {
        return ApiResponse.<ChatCapabilitiesResponse>builder()
                .result(chatbotService.getCapabilities())
                .build();
    }

    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize(Permissions.AUTHENTICATED)
    public SseEmitter ask(@RequestBody @Valid ChatAskRequest request) {
        return chatbotService.ask(request);
    }

    @GetMapping("/conversations/show-all")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<List<ChatConversationResponse>> getConversations() {
        return ApiResponse.<List<ChatConversationResponse>>builder()
                .result(chatbotService.getConversations())
                .build();
    }

    @GetMapping("/conversations/get-messages")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<List<ChatMessageResponse>> getMessages(@RequestParam("conversationId") String conversationId) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .result(chatbotService.getMessages(conversationId))
                .build();
    }

    @DeleteMapping("/conversations/delete/{id}")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<Void> deleteConversation(@PathVariable("id") String conversationId) {
        chatbotService.deleteConversation(conversationId);
        return ApiResponse.<Void>builder().build();
    }

    @PatchMapping("/messages/update/{id}/feedback")
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ApiResponse<Void> setFeedback(@PathVariable("id") String messageId, @RequestBody @Valid ChatFeedbackRequest request) {
        chatbotService.setFeedback(messageId, request.getValue());
        return ApiResponse.<Void>builder().build();
    }

}
