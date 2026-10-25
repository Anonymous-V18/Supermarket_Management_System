package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

// A chatbot knowledge-base document. Only its text (split into kb_chunk rows) is kept, not the uploaded file;
// the chunk vectors live in Qdrant and index_status tells whether they are in sync.
@Entity
@Table(name = "kb_document")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KbDocument extends SoftDeletableEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_INDEXED = "INDEXED";
    public static final String STATUS_FAILED = "FAILED";

    @Column(name = "title", nullable = false)
    String title;

    // MD, TXT, PDF, HTML
    @Column(name = "source_type", nullable = false, length = 10)
    String sourceType;

    @Column(name = "source_name")
    String sourceName;

    @Column(name = "content_hash", nullable = false, columnDefinition = "CHAR(64)")
    String contentHash;

    @Column(name = "chunk_count", nullable = false)
    Integer chunkCount;

    @Column(name = "index_status", nullable = false, length = 10)
    String indexStatus;

    @Column(name = "index_error", length = 500)
    String indexError;

    // Role codes (role.code) allowed to read this document through the chatbot
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "kb_document_role", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "role_code", nullable = false, length = 50)
    @Builder.Default
    Set<String> roleCodes = new HashSet<>();

}
