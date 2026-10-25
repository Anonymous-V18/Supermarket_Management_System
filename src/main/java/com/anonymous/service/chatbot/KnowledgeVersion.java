package com.anonymous.service.chatbot;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

// Bumped on every knowledge-base change; part of the cache key of document answers, so a cached answer
// never outlives the documents (or role assignments) it was built from
@Component
public class KnowledgeVersion {

    private final AtomicLong version = new AtomicLong(System.currentTimeMillis());

    public long current() {
        return version.get();
    }

    public void bump() {
        version.incrementAndGet();
    }

}
