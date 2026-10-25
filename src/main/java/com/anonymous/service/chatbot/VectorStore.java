package com.anonymous.service.chatbot;

import com.anonymous.service.chatbot.SparseVectorEncoder.SparseVector;

import java.util.Collection;
import java.util.List;
import java.util.Set;

// Where chunk vectors are searched. Only ids, document id, the allowed roles and the index version are
// stored next to the vectors; texts stay in MySQL.
public interface VectorStore {

    // indexVersion: how the vectors were made (KnowledgeIndexer.indexVersion), to spot points left from an older encoding
    record Point(String chunkId, String documentId, int chunkIndex, Set<String> roleCodes,
                 float[] dense, SparseVector sparse, String indexVersion) {
    }

    record Hit(String chunkId, double score) {
    }

    // Dense and keyword results are returned separately with their own scores, so the caller can both
    // fuse the rankings and judge how good the best match really is
    record SearchResult(List<Hit> dense, List<Hit> sparse) {
    }

    // Creates the collection when missing; true when it had to be created (so everything must be re-indexed)
    boolean ensureCollection();

    void upsert(List<Point> points);

    void deleteDocument(String documentId);

    void deleteChunks(Collection<String> chunkIds);

    void updateRoles(String documentId, Set<String> roleCodes);

    // Number of points not made with the given index version (they need re-indexing)
    long countOutdated(String indexVersion);

    // Only chunks whose roles intersect roleCodes can come back; dense may be null (no embedding available)
    SearchResult search(float[] dense, SparseVector sparse, Set<String> roleCodes, int limit);

}
