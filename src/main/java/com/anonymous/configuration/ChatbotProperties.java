package com.anonymous.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// com.anonymous.chatbot.* in application.yaml
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "com.anonymous.chatbot")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotProperties {

    // Share of the question's words a chunk must contain to count as an answer (sparse / full-text search)
    double minCoverage = 0.5;

    int topK = 4;

    int requestsPerMinute = 20;

    int historyRetentionDays = 90;

    Qdrant qdrant = new Qdrant();

    Embedding embedding = new Embedding();

    Slm slm = new Slm();

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Qdrant {
        String url = "http://localhost:6333";
        String apiKey;
        String collection = "kb_chunks";
    }

    // Optional dense embeddings from an OpenAI-compatible /v1/embeddings endpoint (TEI, Ollama, vLLM...).
    // Off while url is blank: search is then keyword-only, as before
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Embedding {
        String url;
        String apiKey;
        String model;
        // Some models are trained with markers, e.g. "query: " / "passage: " for multilingual-e5
        String queryPrefix = "";
        String passagePrefix = "";
        // Cosine similarity from which a passage counts as an answer even when it uses other words than the
        // question; depends on the model, calibrate it with RetrievalGoldSetTest
        double minScore = 0.82;
    }

    // Optional local or cloud AI Model (Gemini, Groq, OpenRouter, Ollama) for natural language query parsing & synthesis
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Slm {
        boolean enabled = false;
        String provider = "auto";
        String apiKey = "";
        String url = "http://localhost:11434";
        String model = "gemini-2.5-flash";
        int timeoutMs = 8000;
    }

}
