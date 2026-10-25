package com.anonymous.service.chatbot.tool;

import lombok.Builder;

import java.time.LocalDate;

// Parameters taken from the question (by the rule router). Every field is optional;
// each tool states its own defaults in the answer so the user sees what was actually looked up.
@Builder(toBuilder = true)
public record ToolArgs(
        LocalDate from,
        LocalDate to,
        // Resolved against the real warehouse list; null = every warehouse
        String warehouseId,
        String warehouseName,
        // Product name / code / barcode, or a document code such as HD000001
        String keyword,
        Integer limit,
        Integer withinDays,
        Boolean lowStockOnly,
        Boolean chart,
        Boolean byWarehouse
) {

    public static ToolArgs empty() {
        return ToolArgs.builder().build();
    }

}
