package com.anonymous.repository;

import com.anonymous.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IChatMessageRepository extends JpaRepository<ChatMessage, String> {

    List<ChatMessage> findAllByConversationIdOrderByCreatedDateAsc(String conversationId);

    // Newest first; callers reverse it to get the recent turns in order
    List<ChatMessage> findAllByConversationIdOrderByCreatedDateDesc(String conversationId, Pageable pageable);

}
