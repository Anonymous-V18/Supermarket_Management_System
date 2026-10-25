package com.anonymous.service.chatbot;

import com.anonymous.configuration.ChatbotProperties;
import com.anonymous.service.chatbot.SparseVectorEncoder.SparseVector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Qdrant over its REST API (port 6333). Collection layout:
//  - "dense": dense vectors, cosine; originals on disk, an int8 copy kept in RAM (4x smaller)
//  - "bm25":  sparse keyword vector, IDF applied by Qdrant
//  - payload: document_id, chunk_index, allowed_roles, index_version (keyword indexes on document_id and allowed_roles)
@Slf4j
@Component
public class QdrantVectorStore implements VectorStore {

    private static final String DENSE = "dense";
    private static final String SPARSE = "bm25";
    private static final int UPSERT_BATCH = 64;

    private final ChatbotProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile HttpClient httpClient;

    public QdrantVectorStore(ChatbotProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean ensureCollection() {
        String collection = collectionPath();
        JsonNode exists = call("GET", collection + "/exists", null);
        if (exists.path("result").path("exists").asBoolean(false)) {
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vectors", Map.of(DENSE, Map.of(
                "size", TextEmbedder.DIMENSIONS,
                "distance", "Cosine",
                "on_disk", true)));
        body.put("sparse_vectors", Map.of(SPARSE, Map.of("modifier", "idf")));
        body.put("quantization_config", Map.of("scalar", Map.of("type", "int8", "quantile", 0.99, "always_ram", true)));
        call("PUT", collection, body);
        for (String field : List.of("document_id", "allowed_roles")) {
            call("PUT", collection + "/index?wait=true", Map.of("field_name", field, "field_schema", "keyword"));
        }
        log.info("Created Qdrant collection {}", properties.getQdrant().getCollection());
        return true;
    }

    @Override
    public void upsert(List<Point> points) {
        for (int from = 0; from < points.size(); from += UPSERT_BATCH) {
            List<Map<String, Object>> batch = new ArrayList<>();
            for (Point point : points.subList(from, Math.min(from + UPSERT_BATCH, points.size()))) {
                Map<String, Object> vectors = new LinkedHashMap<>();
                if (point.dense() != null) {
                    vectors.put(DENSE, point.dense());
                }
                vectors.put(SPARSE, sparse(point.sparse()));
                batch.add(Map.of(
                        "id", point.chunkId(),
                        "vector", vectors,
                        "payload", Map.of(
                                "document_id", point.documentId(),
                                "chunk_index", point.chunkIndex(),
                                "allowed_roles", point.roleCodes(),
                                "index_version", point.indexVersion())));
            }
            call("PUT", collectionPath() + "/points?wait=true", Map.of("points", batch));
        }
    }

    @Override
    public void deleteDocument(String documentId) {
        call("POST", collectionPath() + "/points/delete?wait=true", Map.of("filter", documentFilter(documentId)));
    }

    @Override
    public void deleteChunks(Collection<String> chunkIds) {
        if (chunkIds.isEmpty()) {
            return;
        }
        call("POST", collectionPath() + "/points/delete?wait=true", Map.of("points", chunkIds));
    }

    @Override
    public void updateRoles(String documentId, Set<String> roleCodes) {
        call("POST", collectionPath() + "/points/payload?wait=true", Map.of(
                "payload", Map.of("allowed_roles", roleCodes),
                "filter", documentFilter(documentId)));
    }

    @Override
    public long countOutdated(String indexVersion) {
        Map<String, Object> filter = Map.of("must_not", List.of(
                Map.of("key", "index_version", "match", Map.of("value", indexVersion))));
        return call("POST", collectionPath() + "/points/count", Map.of("filter", filter, "exact", true))
                .path("result").path("count").asLong();
    }

    @Override
    public SearchResult search(float[] dense, SparseVector sparse, Set<String> roleCodes, int limit) {
        Map<String, Object> roleFilter = Map.of("must", List.of(
                Map.of("key", "allowed_roles", "match", Map.of("any", roleCodes))));
        List<Map<String, Object>> searches = new ArrayList<>();
        if (dense != null) {
            searches.add(Map.of("query", dense, "using", DENSE, "filter", roleFilter, "limit", limit,
                    "params", Map.of("quantization", Map.of("rescore", true))));
        }
        if (!sparse.isEmpty()) {
            searches.add(Map.of("query", sparse(sparse), "using", SPARSE, "filter", roleFilter, "limit", limit));
        }
        if (searches.isEmpty()) {
            return new SearchResult(List.of(), List.of());
        }
        JsonNode results = call("POST", collectionPath() + "/points/query/batch", Map.of("searches", searches)).path("result");
        int next = 0;
        List<Hit> denseHits = dense != null ? hitsOf(results.path(next++)) : List.of();
        List<Hit> sparseHits = !sparse.isEmpty() ? hitsOf(results.path(next)) : List.of();
        return new SearchResult(denseHits, sparseHits);
    }

    private static List<Hit> hitsOf(JsonNode result) {
        List<Hit> hits = new ArrayList<>();
        for (JsonNode point : result.path("points")) {
            hits.add(new Hit(point.path("id").asString(), point.path("score").asDouble()));
        }
        return hits;
    }

    private static Map<String, Object> sparse(SparseVector vector) {
        return Map.of("indices", vector.indices(), "values", vector.values());
    }

    private static Map<String, Object> documentFilter(String documentId) {
        return Map.of("must", List.of(Map.of("key", "document_id", "match", Map.of("value", documentId))));
    }

    private String collectionPath() {
        return "/collections/" + properties.getQdrant().getCollection();
    }

    private JsonNode call(String method, String path, Object body) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(properties.getQdrant().getUrl() + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json");
            String apiKey = properties.getQdrant().getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                request.header("api-key", apiKey);
            }
            request.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            HttpResponse<String> response = http().send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new VectorStoreException("Qdrant " + method + " " + path + " -> " + response.statusCode()
                        + ": " + abbreviate(response.body()));
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new VectorStoreException("Qdrant unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while calling Qdrant", e);
        }
    }

    private HttpClient http() {
        HttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
                }
                client = httpClient;
            }
        }
        return client;
    }

    private static String abbreviate(String text) {
        return text == null || text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }

    public static class VectorStoreException extends RuntimeException {

        public VectorStoreException(String message) {
            super(message);
        }

        public VectorStoreException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
