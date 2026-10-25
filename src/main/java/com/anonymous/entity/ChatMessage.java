package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ASSISTANT = "ASSISTANT";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "conversation_id", nullable = false, length = 36)
    String conversationId;

    // USER or ASSISTANT
    @Column(name = "role", nullable = false, length = 10)
    String role;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    String content;

    // Tool name, "docs", "refused", ... (assistant messages only)
    @Column(name = "intent", length = 50)
    String intent;

    // JSON array of kb_chunk ids the answer was built from
    @Column(name = "citations", columnDefinition = "JSON")
    String citations;

    @Column(name = "latency_ms")
    Integer latencyMs;

    // 1 helpful, -1 not helpful
    @Column(name = "feedback", columnDefinition = "TINYINT")
    Integer feedback;

    @Column(name = "created_date", nullable = false, updatable = false)
    Instant createdDate;

}
