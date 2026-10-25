package com.anonymous.repository;

import com.anonymous.entity.ChatConversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IChatConversationRepository extends JpaRepository<ChatConversation, String> {

    List<ChatConversation> findAllByOwnerIdOrderByModifiedDateDesc(String ownerId, Pageable pageable);

    Optional<ChatConversation> findByIdAndOwnerId(String id, String ownerId);

    // Messages go with their conversation (ON DELETE CASCADE)
    @Modifying
    @Query(value = "DELETE FROM chat_conversation WHERE modified_date < :before", nativeQuery = true)
    int deleteAllModifiedBefore(@Param("before") Instant before);

}
