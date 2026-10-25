package com.anonymous.service.chatbot;

import com.anonymous.configuration.ChatbotProperties;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Finds the knowledge-base passages for a question, only among documents the asker's roles may read.
// Role filtering happens twice: inside the Qdrant query, then again against MySQL (the source of truth),
// so a stale vector payload can never leak a passage. A passage must also clearly match the question:
// enough of the question's words in it, or (with embeddings) a meaning close enough to it. Otherwise nothing is
// returned and the chatbot says it has no information instead of guessing.
// With an embedding service the keyword and the dense ranking are fused (reciprocal rank fusion), so a passage
// found by both comes first and one found by meaning only still gets a chance.
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KnowledgeRetriever {

    private static final int CANDIDATES = 20;
    // Usual RRF constant: damps the gap between the first ranks so neither list dominates
    private static final int RRF_K = 60;

    ChatbotProperties properties;
    VectorStore vectorStore;
    SparseVectorEncoder sparseVectorEncoder;
    TextEmbedder embedder;
    IKbChunkRepository chunkRepository;
    IKbDocumentRepository documentRepository;

    public record Passage(KbChunk chunk, String documentTitle, double coverage, Double denseScore) {
    }

    // mode: "hybrid" (keyword + dense), "keyword", "fulltext" (Qdrant unreachable)
    // topic: the question's topic weighted over the candidates, reused to pick the answer lines
    public record Retrieval(List<Passage> passages, String mode, TopicMatcher topic) {
    }

    public Retrieval retrieve(String question, Set<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return new Retrieval(List.of(), "none", new TopicMatcher(question));
        }
        List<String> rankedIds;
        Map<String, Double> denseScores = Map.of();
        String mode;
        try {
            float[] dense = embedQuery(question);
            VectorStore.SearchResult result = vectorStore.search(
                    dense, sparseVectorEncoder.encodeQuery(question), roleCodes, CANDIDATES);
            rankedIds = fuse(result.dense(), result.sparse());
            denseScores = result.dense().stream()
                    .collect(Collectors.toMap(VectorStore.Hit::chunkId, VectorStore.Hit::score, (a, b) -> a));
            mode = dense != null ? "hybrid" : "keyword";
        } catch (RuntimeException e) {
            log.warn("Vector search unavailable, using MySQL full-text: {}", e.getMessage());
            rankedIds = chunkRepository.fullTextSearch(question, roleCodes, CANDIDATES).stream()
                    .map(IKbChunkRepository.ChunkHit::getId).toList();
            mode = "fulltext";
        }
        Retrieval selected = select(question, rankedIds, denseScores, roleCodes);
        return new Retrieval(selected.passages(), mode, selected.topic());
    }

    // null when no embedding service is configured or it fails (search is then keyword-only)
    private float[] embedQuery(String question) {
        if (!embedder.isEnabled()) {
            return null;
        }
        try {
            return embedder.embedQuery(question);
        } catch (RuntimeException e) {
            log.warn("Embedding the question failed, keyword search only: {}", e.getMessage());
            return null;
        }
    }

    // Reciprocal rank fusion: score = sum of 1 / (k + rank) over the lists a chunk appears in
    static List<String> fuse(List<VectorStore.Hit> dense, List<VectorStore.Hit> sparse) {
        if (dense.isEmpty()) {
            return sparse.stream().map(VectorStore.Hit::chunkId).toList();
        }
        Map<String, Double> scores = new LinkedHashMap<>();
        for (List<VectorStore.Hit> hits : List.of(sparse, dense)) {
            for (int rank = 0; rank < hits.size(); rank++) {
                scores.merge(hits.get(rank).chunkId(), 1.0 / (RRF_K + rank + 1), Double::sum);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    private Retrieval select(String question, List<String> rankedIds, Map<String, Double> denseScores,
                             Set<String> roleCodes) {
        if (rankedIds.isEmpty()) {
            return new Retrieval(List.of(), null, new TopicMatcher(question));
        }
        Map<String, KbChunk> chunks = chunkRepository.findAllById(rankedIds).stream()
                .collect(Collectors.toMap(KbChunk::getId, Function.identity()));
        Map<String, KbDocument> documents = documentRepository.findAllByIdInAndIsDeletedFalse(
                        chunks.values().stream().map(KbChunk::getDocumentId).distinct().toList()).stream()
                .collect(Collectors.toMap(KbDocument::getId, Function.identity()));

        // Topic words weighted by how rare they are among the candidates the asker may read
        TopicMatcher topic = new TopicMatcher(question).withCorpus(chunks.values().stream()
                .filter(chunk -> documents.containsKey(chunk.getDocumentId())
                        && !Collections.disjoint(documents.get(chunk.getDocumentId()).getRoleCodes(), roleCodes))
                .map(KnowledgeIndexer::searchableText)
                .toList());

        List<Passage> passages = new ArrayList<>();
        Map<String, Double> rankScores = new HashMap<>();
        for (int rank = 0; rank < rankedIds.size(); rank++) {
            String id = rankedIds.get(rank);
            KbChunk chunk = chunks.get(id);
            KbDocument document = chunk == null ? null : documents.get(chunk.getDocumentId());
            if (document == null || Collections.disjoint(document.getRoleCodes(), roleCodes)) {
                continue;
            }
            Match match = Match.of(topic, chunk);
            Double denseScore = denseScores.get(id);
            boolean relevant = isRelevant(match) || isCloseInMeaning(denseScore);
            // A close meaning counts like a full keyword match of the passage, scaled to how close it is
            double meaning = isCloseInMeaning(denseScore) ? 1 + (denseScore - properties.getEmbedding().getMinScore()) * 10 : 0;
            double rankScore = 2 * match.headingSubject() + match.leafHeading() + 1.2 * match.bestLine() + match.whole() + meaning
                    + 0.2 / (rank + 1);
            log.debug("candidate {} match={} rank={} relevant={}", chunk.getHeadingPath(), match,
                    rankScore, relevant);
            if (relevant) {
                passages.add(new Passage(chunk, document.getTitle(), match.whole(),
                    isCloseInMeaning(denseScore) ? denseScore : null));
                rankScores.put(id, rankScore);
            }
        }
        // Local re-ranking: keyword search alone favours long passages that repeat common words, while a
        // section whose heading names the topic, or with one line matching it closely, is usually the answer
        passages.sort((a, b) -> Double.compare(rankScores.get(b.chunk().getId()), rankScores.get(a.chunk().getId())));
        List<Passage> top = passages.size() <= properties.getTopK() ? passages : passages.subList(0, properties.getTopK());
        return new Retrieval(top, null, topic);
    }

    // How much of the topic the heading, the best single line and the whole passage cover; headingSubject
    // counts common words too, so "Sau khi lập hóa đơn" outranks a passage that only mentions "hủy hóa đơn"
    // in an example (ranking only: relevance still needs the rare words)
    record Match(double heading, double headingSubject, double leafHeading, double bestLine, double whole) {

        static Match of(TopicMatcher topic, KbChunk chunk) {
            String heading = chunk.getHeadingPath() == null ? "" : chunk.getHeadingPath();
            String leaf = heading;
            int lastSep = heading.lastIndexOf(" > ");
            if (lastSep >= 0) {
                leaf = heading.substring(lastSep + 3);
            }
            return new Match(topic.share(heading), topic.headingShare(heading),
                    topic.headingShare(leaf),
                    topic.bestLineShare(chunk.getContent()),
                    topic.share(KnowledgeIndexer.searchableText(chunk)));
        }

    }

    private boolean isCloseInMeaning(Double denseScore) {
        return denseScore != null && denseScore >= properties.getEmbedding().getMinScore();
    }

    // The section is about the topic, one line states most of it, or the passage covers it well
    private boolean isRelevant(Match match) {
        double min = properties.getMinCoverage();
        return match.heading() >= min || match.bestLine() >= min || match.whole() >= min;
    }

}
