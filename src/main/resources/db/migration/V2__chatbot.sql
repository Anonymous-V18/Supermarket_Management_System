-- =====================================================================
-- Internal chatbot: knowledge base (text only, vectors live in Qdrant) and conversation history
-- =====================================================================

-- One uploaded document; the original file is not kept, only its text split into chunks
CREATE TABLE `kb_document` (
    `id` VARCHAR(36) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `source_type` VARCHAR(10) NOT NULL,
    `source_name` VARCHAR(255) NULL,
    `content_hash` CHAR(64) NOT NULL,
    `chunk_count` INT NOT NULL DEFAULT 0,
    -- Sync state of the document's vectors in Qdrant
    `index_status` VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    `index_error` VARCHAR(500) NULL,
    `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
    `deleted_date` DATETIME(6) NULL,
    `deleted_by` VARCHAR(255) NULL,
    `created_by` VARCHAR(255) NOT NULL,
    `created_date` DATETIME(6) NOT NULL,
    `modified_by` VARCHAR(255) NULL,
    `modified_date` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `ck_kb_document_source_type` CHECK (`source_type` IN ('MD', 'TXT', 'PDF', 'HTML')),
    CONSTRAINT `ck_kb_document_index_status` CHECK (`index_status` IN ('PENDING', 'INDEXED', 'FAILED')),
    INDEX `idx_kb_document_index_status` (`index_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Role codes allowed to read a document (same codes as role.code)
CREATE TABLE `kb_document_role` (
    `document_id` VARCHAR(36) NOT NULL,
    `role_code` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`document_id`, `role_code`),
    INDEX `idx_kb_document_role_code` (`role_code`),
    CONSTRAINT `fk_kb_document_role_document` FOREIGN KEY (`document_id`) REFERENCES `kb_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chunk id is also the Qdrant point id; the ngram full-text index is the fallback when Qdrant is down
CREATE TABLE `kb_chunk` (
    `id` VARCHAR(36) NOT NULL,
    `document_id` VARCHAR(36) NOT NULL,
    `chunk_index` INT NOT NULL,
    `heading_path` VARCHAR(500) NULL,
    `content` TEXT NOT NULL,
    `content_hash` CHAR(64) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_kb_chunk_document_index` UNIQUE (`document_id`, `chunk_index`),
    CONSTRAINT `fk_kb_chunk_document` FOREIGN KEY (`document_id`) REFERENCES `kb_document` (`id`) ON DELETE CASCADE,
    FULLTEXT INDEX `ft_kb_chunk_text` (`heading_path`, `content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- owner_id is the JWT subject (employee id); a conversation is only ever visible to its owner
CREATE TABLE `chat_conversation` (
    `id` VARCHAR(36) NOT NULL,
    `owner_id` VARCHAR(36) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `created_date` DATETIME(6) NOT NULL,
    `modified_date` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_chat_conversation_owner` (`owner_id`, `modified_date`),
    INDEX `idx_chat_conversation_modified` (`modified_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- citations holds kb_chunk ids only (no copied text); feedback: 1 helpful, -1 not helpful
CREATE TABLE `chat_message` (
    `id` VARCHAR(36) NOT NULL,
    `conversation_id` VARCHAR(36) NOT NULL,
    `role` VARCHAR(10) NOT NULL,
    `content` TEXT NOT NULL,
    `intent` VARCHAR(50) NULL,
    `citations` JSON NULL,
    `latency_ms` INT NULL,
    `feedback` TINYINT NULL,
    `created_date` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `ck_chat_message_role` CHECK (`role` IN ('USER', 'ASSISTANT')),
    CONSTRAINT `ck_chat_message_feedback` CHECK (`feedback` IS NULL OR `feedback` IN (-1, 1)),
    INDEX `idx_chat_message_conversation` (`conversation_id`, `created_date`),
    CONSTRAINT `fk_chat_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `chat_conversation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
