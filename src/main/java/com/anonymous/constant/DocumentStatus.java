package com.anonymous.constant;

import java.util.Set;

// Codes of status_invoice, shared by invoices and stock documents.
// Rules: only PENDING documents can be edited; stock counts for every status except CANCELLED;
// CANCELLED is final.
public final class DocumentStatus {

    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private static final Set<String> ALLOWED_TRANSITIONS = Set.of(
            PENDING + ">" + COMPLETED,
            PENDING + ">" + CANCELLED,
            COMPLETED + ">" + CANCELLED
    );

    private DocumentStatus() {
        throw new UnsupportedOperationException("Cannot be instantiated !");
    }

    public static boolean affectsStock(String statusCode) {
        return !CANCELLED.equals(statusCode);
    }

    public static boolean isEditable(String statusCode) {
        return PENDING.equals(statusCode);
    }

    public static boolean canChange(String fromCode, String toCode) {
        return fromCode.equals(toCode) || ALLOWED_TRANSITIONS.contains(fromCode + ">" + toCode);
    }

}
