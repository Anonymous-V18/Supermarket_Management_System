package com.anonymous.service.chatbot;

import com.anonymous.configuration.ChatbotProperties;
import com.anonymous.entity.KbChunk;
import com.anonymous.entity.KbDocument;
import com.anonymous.repository.IKbChunkRepository;
import com.anonymous.repository.IKbDocumentRepository;
import com.anonymous.service.chatbot.SparseVectorEncoder.SparseVector;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Replays rag-eval/retrieval-gold.jsonl against the seed knowledge base (the functional spec, cut into
// documents and chunks exactly as at start-up) and reports Recall@k and MRR of the retriever.
// Qdrant is replaced by an in-memory store that scores like Qdrant: BM25 with the "modifier: idf" sparse
// search, cosine for dense vectors. Keyword-only by default; set RAG_EVAL_EMBEDDING_URL (plus optionally
// RAG_EVAL_EMBEDDING_MODEL, RAG_EVAL_QUERY_PREFIX, RAG_EVAL_PASSAGE_PREFIX, RAG_EVAL_MIN_SCORE) to measure
// the hybrid search with a real embedding service, e.g. to calibrate chatbot.embedding.min-score.
// Each gold line: q (question), heading (a text the expected chunk's heading path contains), or none=true
// for a question the knowledge base cannot answer (nothing may come back); after: heading path of the previous
// answer, for a follow-up question.
// Add a line per question that was answered wrongly; the metrics may never drop below the minimums.
class RetrievalGoldSetTest {

    // Keyword-only figures on 2026-10-25 (recall 0.860, MRR 0.822); raise them as misses get fixed
    private static final double MIN_RECALL = 0.85;
    private static final double MIN_MRR = 0.8;
    private static final double MIN_REJECTION = 1.0;
    private static final String ROLE = "ADMIN";

    private static KnowledgeRetriever retriever;
    private static ChatbotProperties properties;
    private static List<KbChunk> corpus;

    // Qdrant sparse search with the idf modifier: sum of query weight * document weight * idf(term);
    // dense search: cosine similarity
    static final class InMemoryStore implements VectorStore {

        private final Map<String, Map<Integer, Float>> documents = new LinkedHashMap<>();
        private final Map<String, float[]> denseVectors = new LinkedHashMap<>();

        @Override
        public boolean ensureCollection() {
            return false;
        }

        @Override
        public void upsert(List<Point> points) {
            for (Point point : points) {
                Map<Integer, Float> weights = new HashMap<>();
                for (int i = 0; i < point.sparse().indices().length; i++) {
                    weights.put(point.sparse().indices()[i], point.sparse().values()[i]);
                }
                documents.put(point.chunkId(), weights);
                if (point.dense() != null) {
                    denseVectors.put(point.chunkId(), point.dense());
                }
            }
        }

        @Override
        public void deleteDocument(String documentId) {
        }

        @Override
        public void deleteChunks(Collection<String> chunkIds) {
            chunkIds.forEach(documents::remove);
        }

        @Override
        public void updateRoles(String documentId, Set<String> roleCodes) {
        }

        @Override
        public long countOutdated(String indexVersion) {
            return 0;
        }

        @Override
        public SearchResult search(float[] dense, SparseVector sparse, Set<String> roleCodes, int limit) {
            int n = documents.size();
            Map<String, Double> scores = new HashMap<>();
            for (int i = 0; i < sparse.indices().length; i++) {
                int index = sparse.indices()[i];
                long df = documents.values().stream().filter(weights -> weights.containsKey(index)).count();
                if (df == 0) {
                    continue;
                }
                double idf = Math.log((n - df + 0.5) / (df + 0.5) + 1);
                double queryWeight = sparse.values()[i];
                documents.forEach((id, weights) -> {
                    Float weight = weights.get(index);
                    if (weight != null) {
                        scores.merge(id, queryWeight * weight * idf, Double::sum);
                    }
                });
            }
            Map<String, Double> denseScores = new HashMap<>();
            if (dense != null) {
                denseVectors.forEach((id, vector) -> denseScores.put(id, cosine(dense, vector)));
            }
            return new SearchResult(top(denseScores, limit), top(scores, limit));
        }

        private static List<Hit> top(Map<String, Double> scores, int limit) {
            return scores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(limit)
                    .map(entry -> new Hit(entry.getKey(), entry.getValue()))
                    .toList();
        }

        private static double cosine(float[] a, float[] b) {
            double dot = 0;
            double normA = 0;
            double normB = 0;
            for (int i = 0; i < a.length; i++) {
                dot += a[i] * b[i];
                normA += a[i] * a[i];
                normB += b[i] * b[i];
            }
            return dot / (Math.sqrt(normA) * Math.sqrt(normB));
        }

    }

    @BeforeAll
    static void indexSeed() throws Exception {
        String html;
        try (InputStream in = RetrievalGoldSetTest.class.getResourceAsStream("/kb/seed/dac-ta-chuc-nang.html")) {
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        properties = new ChatbotProperties();
        ChatbotProperties.Embedding embedding = properties.getEmbedding();
        embedding.setUrl(System.getenv("RAG_EVAL_EMBEDDING_URL"));
        embedding.setModel(System.getenv("RAG_EVAL_EMBEDDING_MODEL"));
        embedding.setQueryPrefix(java.util.Objects.requireNonNullElse(System.getenv("RAG_EVAL_QUERY_PREFIX"), ""));
        embedding.setPassagePrefix(java.util.Objects.requireNonNullElse(System.getenv("RAG_EVAL_PASSAGE_PREFIX"), ""));
        if (System.getenv("RAG_EVAL_MIN_SCORE") != null) {
            embedding.setMinScore(Double.parseDouble(System.getenv("RAG_EVAL_MIN_SCORE")));
        }
        TextEmbedder embedder = new TextEmbedder(properties);
        DocumentChunker chunker = new DocumentChunker();
        SparseVectorEncoder encoder = new SparseVectorEncoder();
        InMemoryStore store = new InMemoryStore();
        Map<String, KbChunk> chunks = new LinkedHashMap<>();
        Map<String, KbDocument> documents = new LinkedHashMap<>();
        int documentNo = 0;
        for (Map.Entry<String, String> chapter : chapters(DocumentTextExtractor.htmlText(html)).entrySet()) {
            KbDocument document = KbDocument.builder().title(chapter.getKey()).roleCodes(Set.of(ROLE)).build();
            document.setId("doc-" + documentNo++);
            documents.put(document.getId(), document);
            List<DocumentChunker.Chunk> pieces = chunker.chunk(chapter.getKey(), chapter.getValue());
            List<VectorStore.Point> points = new ArrayList<>();
            List<KbChunk> documentChunks = new ArrayList<>();
            for (int i = 0; i < pieces.size(); i++) {
                KbChunk chunk = KbChunk.builder()
                        .id(document.getId() + "-" + i)
                        .documentId(document.getId())
                        .chunkIndex(i)
                        .headingPath(pieces.get(i).headingPath())
                        .content(pieces.get(i).content())
                        .build();
                chunks.put(chunk.getId(), chunk);
                documentChunks.add(chunk);
            }
            List<String> texts = documentChunks.stream().map(KnowledgeIndexer::searchableText).toList();
            List<float[]> dense = embedder.isEnabled() ? embedder.embedPassages(texts) : null;
            for (int i = 0; i < documentChunks.size(); i++) {
                KbChunk chunk = documentChunks.get(i);
                points.add(new VectorStore.Point(chunk.getId(), document.getId(), i, document.getRoleCodes(),
                        dense == null ? null : dense.get(i), encoder.encodeDocument(texts.get(i)), "test"));
            }
            store.upsert(points);
        }
        corpus = List.copyOf(chunks.values());

        IKbChunkRepository chunkRepository = mock(IKbChunkRepository.class);
        when(chunkRepository.findAllById(anyCollection())).thenAnswer(call -> {
            Collection<String> ids = call.getArgument(0);
            return ids.stream().map(chunks::get).filter(java.util.Objects::nonNull).toList();
        });
        IKbDocumentRepository documentRepository = mock(IKbDocumentRepository.class);
        when(documentRepository.findAllByIdInAndIsDeletedFalse(anyCollection())).thenAnswer(call -> {
            Collection<String> ids = call.getArgument(0);
            return ids.stream().map(documents::get).filter(java.util.Objects::nonNull).toList();
        });
        retriever = new KnowledgeRetriever(properties, store, encoder, embedder, chunkRepository, documentRepository);
    }

    // Same split as KnowledgeBaseService.splitChapters: one document per "## " chapter
    private static Map<String, String> chapters(String text) {
        Map<String, String> chapters = new LinkedHashMap<>();
        String current = null;
        StringBuilder body = new StringBuilder();
        for (String line : text.split("\n")) {
            if (line.startsWith("## ")) {
                if (current != null) {
                    chapters.put(current, body.toString());
                }
                current = line.substring(3).strip();
                body.setLength(0);
            } else if (current != null) {
                body.append(line).append('\n');
            }
        }
        if (current != null) {
            chapters.put(current, body.toString());
        }
        chapters.values().removeIf(String::isBlank);
        return chapters;
    }

    @Test
    void goldQuestionsFindTheirSection() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<String> misses = new ArrayList<>();
        int answerable = 0;
        int found = 0;
        double reciprocalRanks = 0;
        int unanswerable = 0;
        int rejected = 0;
        try (InputStream in = getClass().getResourceAsStream("/rag-eval/retrieval-gold.jsonl");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode gold = mapper.readTree(line);
                String question = gold.get("q").asString();
                String searched = FollowUpQuestion.standalone(question, gold.has("after") ? gold.get("after").asString() : null);
                List<String> headings = retriever.retrieve(searched, Set.of(ROLE)).passages().stream()
                        .map(passage -> passage.chunk().getHeadingPath())
                        .toList();
                if (gold.path("none").asBoolean(false)) {
                    unanswerable++;
                    if (headings.isEmpty()) {
                        rejected++;
                    } else {
                        misses.add(question + " -> " + headings + " (expected nothing)");
                    }
                    continue;
                }
                answerable++;
                String expected = gold.get("heading").asString();
                int rank = -1;
                for (int i = 0; i < headings.size(); i++) {
                    if (headings.get(i).contains(expected)) {
                        rank = i + 1;
                        break;
                    }
                }
                if (rank > 0) {
                    found++;
                    reciprocalRanks += 1.0 / rank;
                }
                if (rank != 1) {
                    misses.add(question + " -> rank " + rank + " " + headings + " (expected " + expected + ")");
                }
            }
        }
        double recall = answerable == 0 ? 0 : (double) found / answerable;
        double mrr = answerable == 0 ? 0 : reciprocalRanks / answerable;
        double rejection = unanswerable == 0 ? 1 : (double) rejected / unanswerable;
        System.out.printf("retrieval gold (%s): recall@%d=%.3f (%d/%d) mrr=%.3f rejection=%.3f (%d/%d)%n",
                properties.getEmbedding().getUrl() == null ? "keyword" : "hybrid", properties.getTopK(), recall, found, answerable, mrr, rejection, rejected, unanswerable);
        misses.forEach(miss -> System.out.println("  MISS " + miss));
        assertThat(answerable).isPositive();
        assertThat(recall).as("misses: %s", misses).isGreaterThanOrEqualTo(MIN_RECALL);
        assertThat(mrr).as("misses: %s", misses).isGreaterThanOrEqualTo(MIN_MRR);
        assertThat(rejection).as("misses: %s", misses).isGreaterThanOrEqualTo(MIN_REJECTION);
    }

    // Prints how long the indexed chunks are in search terms, the figure BM25 length normalisation needs
    @Test
    void reportsChunkLengthInTerms() {
        List<Integer> lengths = corpus.stream()
                .map(chunk -> com.anonymous.utils.VietnameseTextUtil.terms(KnowledgeIndexer.searchableText(chunk)).size())
                .sorted(Comparator.naturalOrder())
                .toList();
        double average = lengths.stream().mapToInt(Integer::intValue).average().orElse(0);
        System.out.printf("seed chunks: %d, terms per chunk avg=%.0f median=%d max=%d%n",
                lengths.size(), average, lengths.get(lengths.size() / 2), lengths.getLast());
        corpus.forEach(chunk -> System.out.println("  CHUNK " + chunk.getHeadingPath()));
        assertThat(lengths).isNotEmpty();
    }

}
