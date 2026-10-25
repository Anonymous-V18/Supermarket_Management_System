package com.anonymous.repository;

import com.anonymous.entity.KbChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface IKbChunkRepository extends JpaRepository<KbChunk, String> {

    List<KbChunk> findAllByDocumentIdOrderByChunkIndexAsc(String documentId);

    @Modifying
    @Query("DELETE FROM KbChunk c WHERE c.documentId = :documentId")
    void deleteAllByDocumentId(@Param("documentId") String documentId);

    interface ChunkHit {
        String getId();

        Double getScore();
    }

    // Fallback search when Qdrant is unreachable: ngram full-text, already limited to documents the roles may read
    @Query(value = """
            SELECT c.id AS id, MATCH(c.heading_path, c.content) AGAINST (:query IN NATURAL LANGUAGE MODE) AS score
            FROM kb_chunk c
            JOIN kb_document d ON d.id = c.document_id AND d.is_deleted = FALSE
            WHERE EXISTS (SELECT 1 FROM kb_document_role r WHERE r.document_id = d.id AND r.role_code IN (:roleCodes))
              AND MATCH(c.heading_path, c.content) AGAINST (:query IN NATURAL LANGUAGE MODE)
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ChunkHit> fullTextSearch(@Param("query") String query,
                                  @Param("roleCodes") Collection<String> roleCodes,
                                  @Param("limit") int limit);

}
