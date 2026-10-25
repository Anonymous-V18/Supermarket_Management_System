package com.anonymous.service.chatbot;

import com.anonymous.entity.KbChunk;
import com.anonymous.entity.KbDocument;
import com.anonymous.repository.IKbChunkRepository;
import com.anonymous.repository.IKbDocumentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// Pushes a document's chunks (already saved in MySQL) to the vector store and records the outcome in
// kb_document.index_status. MySQL is the source of truth: a FAILED document is retried on the next start
// or from the admin "re-index" button, and is meanwhile still found through the full-text fallback.
// With an embedding service configured each chunk also gets a dense vector; when that service fails the
// chunk is still indexed for keyword search and the document carries a warning.
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KnowledgeIndexer {

    // Bump when SparseVectorEncoder changes how it weights terms: every point is then re-indexed at start-up
    private static final String SPARSE_VERSION = "bm25:2";

    VectorStore vectorStore;
    SparseVectorEncoder sparseVectorEncoder;
    TextEmbedder embedder;
    IKbChunkRepository chunkRepository;
    IKbDocumentRepository documentRepository;

    // Text that is keyword-indexed for a chunk: its section path carries the topic
    public static String searchableText(KbChunk chunk) {
        return chunk.getHeadingPath() == null ? chunk.getContent() : chunk.getHeadingPath() + "\n" + chunk.getContent();
    }

    // How the vectors are made now; points written another way are re-indexed at start-up
    public String indexVersion() {
        return SPARSE_VERSION + "|" + embedder.version();
    }

    // Indexes the given chunks (all of them, or only the new ones of an edited document)
    public void index(KbDocument document, Collection<KbChunk> chunks) {
        try {
            List<KbChunk> list = new ArrayList<>(chunks);
            List<String> texts = list.stream().map(KnowledgeIndexer::searchableText).toList();
            List<float[]> dense = null;
            String warning = null;
            if (embedder.isEnabled() && !texts.isEmpty()) {
                try {
                    dense = embedder.embedPassages(texts);
                } catch (RuntimeException e) {
                    log.warn("Embedding document {} failed, indexing it for keyword search only: {}",
                            document.getId(), e.getMessage());
                    warning = "Chỉ tìm theo từ khóa (lỗi embedding): " + e.getMessage();
                }
            }
            // Keyword-only points get another version, so they are embedded again once the service is back
            String version = dense != null || !embedder.isEnabled() ? indexVersion() : SPARSE_VERSION + "|dense:failed";
            List<VectorStore.Point> points = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                KbChunk chunk = list.get(i);
                points.add(new VectorStore.Point(chunk.getId(), document.getId(), chunk.getChunkIndex(),
                        document.getRoleCodes(), dense == null ? null : dense.get(i),
                        sparseVectorEncoder.encodeDocument(texts.get(i)), version));
            }
            vectorStore.upsert(points);
            markIndexed(document, warning);
        } catch (RuntimeException e) {
            log.warn("Indexing document {} failed: {}", document.getId(), e.getMessage());
            markFailed(document, e.getMessage());
        }
    }

    public void reindex(KbDocument document) {
        try {
            vectorStore.deleteDocument(document.getId());
        } catch (RuntimeException e) {
            markFailed(document, e.getMessage());
            return;
        }
        index(document, chunkRepository.findAllByDocumentIdOrderByChunkIndexAsc(document.getId()));
    }

    private void markIndexed(KbDocument document, String warning) {
        document.setIndexStatus(KbDocument.STATUS_INDEXED);
        document.setIndexError(warning == null ? null : warning.substring(0, Math.min(warning.length(), 500)));
        documentRepository.save(document);
    }

    private void markFailed(KbDocument document, String error) {
        document.setIndexStatus(KbDocument.STATUS_FAILED);
        document.setIndexError(error == null ? null : error.substring(0, Math.min(error.length(), 500)));
        documentRepository.save(document);
    }

}
