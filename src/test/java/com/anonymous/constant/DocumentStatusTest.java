package com.anonymous.constant;

import org.junit.jupiter.api.Test;

import static com.anonymous.constant.DocumentStatus.CANCELLED;
import static com.anonymous.constant.DocumentStatus.COMPLETED;
import static com.anonymous.constant.DocumentStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentStatusTest {

    @Test
    void pendingCanBeCompletedOrCancelled() {
        assertThat(DocumentStatus.canChange(PENDING, COMPLETED)).isTrue();
        assertThat(DocumentStatus.canChange(PENDING, CANCELLED)).isTrue();
    }

    @Test
    void completedCanOnlyBeCancelled() {
        assertThat(DocumentStatus.canChange(COMPLETED, CANCELLED)).isTrue();
        assertThat(DocumentStatus.canChange(COMPLETED, PENDING)).isFalse();
    }

    @Test
    void cancelledIsFinal() {
        assertThat(DocumentStatus.canChange(CANCELLED, PENDING)).isFalse();
        assertThat(DocumentStatus.canChange(CANCELLED, COMPLETED)).isFalse();
        assertThat(DocumentStatus.canChange(CANCELLED, CANCELLED)).isTrue();
    }

    @Test
    void onlyCancelledDocumentsLeaveStockAlone() {
        assertThat(DocumentStatus.affectsStock(PENDING)).isTrue();
        assertThat(DocumentStatus.affectsStock(COMPLETED)).isTrue();
        assertThat(DocumentStatus.affectsStock(CANCELLED)).isFalse();
    }

    @Test
    void onlyPendingDocumentsAreEditable() {
        assertThat(DocumentStatus.isEditable(PENDING)).isTrue();
        assertThat(DocumentStatus.isEditable(COMPLETED)).isFalse();
        assertThat(DocumentStatus.isEditable(CANCELLED)).isFalse();
    }

}
