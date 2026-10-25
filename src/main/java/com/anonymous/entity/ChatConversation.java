package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

// Chat history belongs to one person only: owner_id is the JWT subject (employee id)
@Entity
@Table(name = "chat_conversation")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "owner_id", nullable = false, length = 36)
    String ownerId;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "created_date", nullable = false, updatable = false)
    Instant createdDate;

    // Bumped on every new message; drives the history order and the retention clean-up
    @Column(name = "modified_date", nullable = false)
    Instant modifiedDate;

}
