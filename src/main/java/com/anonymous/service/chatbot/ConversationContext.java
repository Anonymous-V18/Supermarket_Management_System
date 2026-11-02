package com.anonymous.service.chatbot;

import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record ConversationContext(
        String previousIntent,
        String lastPersonName,
        String lastProductKeyword,
        String lastDocumentCode,
        String lastWarehouseId,
        String lastWarehouseName,
        LocalDate lastFrom,
        LocalDate lastTo
) {

    public static ConversationContext empty() {
        return ConversationContext.builder().build();
    }

    public static ConversationContext ofIntent(String previousIntent) {
        return ConversationContext.builder().previousIntent(previousIntent).build();
    }

}
