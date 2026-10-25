package com.anonymous.service.chatbot;

import lombok.Builder;

@Builder(toBuilder = true)
public record ConversationContext(
        String previousIntent,
        String lastPersonName,
        String lastProductKeyword,
        String lastWarehouseId,
        String lastWarehouseName
) {

    public static ConversationContext empty() {
        return ConversationContext.builder().build();
    }

    public static ConversationContext ofIntent(String previousIntent) {
        return ConversationContext.builder().previousIntent(previousIntent).build();
    }

}
