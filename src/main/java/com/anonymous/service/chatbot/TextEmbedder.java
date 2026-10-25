package com.anonymous.service.chatbot;

import com.anonymous.configuration.ChatbotProperties;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Dense side of the hybrid search: turns texts into meaning vectors, so "mật khẩu ban đầu" finds a passage
// that says "mật khẩu mặc định". Calls an OpenAI-compatible POST {url}/v1/embeddings, which TEI, Ollama,
// vLLM and LM Studio all serve. Disabled while chatbot.embedding.url is blank.
@Slf4j
@Component
public class TextEmbedder {

    // Size of the "dense" vectors of the Qdrant collection; the model must produce exactly this many
    public static final int DIMENSIONS = 768;
    private static final int BATCH = 32;

    private final ChatbotProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile HttpClient httpClient;

    public TextEmbedder(ChatbotProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        String url = properties.getEmbedding().getUrl();
        return url != null && !url.isBlank();
    }

    // Identifies the vectors this embedder makes: a change of model means every passage must be embedded again
    public String version() {
        return isEnabled() ? "dense:" + properties.getEmbedding().getModel() : "dense:off";
    }

    public float[] embedQuery(String question) {
        return embed(List.of(prefixed(properties.getEmbedding().getQueryPrefix(), question))).getFirst();
    }

    public List<float[]> embedPassages(List<String> texts) {
        String prefix = properties.getEmbedding().getPassagePrefix();
        List<float[]> vectors = new ArrayList<>();
        for (int from = 0; from < texts.size(); from += BATCH) {
            vectors.addAll(embed(texts.subList(from, Math.min(from + BATCH, texts.size())).stream()
                    .map(text -> prefixed(prefix, text))
                    .toList()));
        }
        return vectors;
    }

    private static String prefixed(String prefix, String text) {
        return prefix == null ? text : prefix + text;
    }

    private List<float[]> embed(List<String> inputs) {
        ChatbotProperties.Embedding config = properties.getEmbedding();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("input", inputs);
        if (config.getModel() != null && !config.getModel().isBlank()) {
            body.put("model", config.getModel());
        }
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(stripSlash(config.getUrl()) + "/v1/embeddings"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                request.header("Authorization", "Bearer " + config.getApiKey());
            }
            HttpResponse<String> response = http().send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new EmbeddingException("Embedding service -> " + response.statusCode() + ": "
                        + abbreviate(response.body()));
            }
            return vectorsOf(objectMapper.readTree(response.body()), inputs.size());
        } catch (IOException e) {
            throw new EmbeddingException("Embedding service unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("Interrupted while calling the embedding service", e);
        }
    }

    // {"data": [{"index": 0, "embedding": [...]}, ...]}, put back in input order
    static List<float[]> vectorsOf(JsonNode response, int expected) {
        float[][] vectors = new float[expected][];
        for (JsonNode item : response.path("data")) {
            int index = item.path("index").asInt(-1);
            JsonNode values = item.path("embedding");
            if (index < 0 || index >= expected) {
                throw new EmbeddingException("Embedding service returned an unexpected index " + index);
            }
            if (values.size() != DIMENSIONS) {
                throw new EmbeddingException("Embedding model returns " + values.size() + " dimensions, the collection needs "
                        + DIMENSIONS);
            }
            float[] vector = new float[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] = (float) values.get(i).asDouble();
            }
            vectors[index] = vector;
        }
        for (float[] vector : vectors) {
            if (vector == null) {
                throw new EmbeddingException("Embedding service returned fewer vectors than texts");
            }
        }
        return List.of(vectors);
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

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String abbreviate(String text) {
        return text == null || text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }

    public static class EmbeddingException extends RuntimeException {

        public EmbeddingException(String message) {
            super(message);
        }

        public EmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
