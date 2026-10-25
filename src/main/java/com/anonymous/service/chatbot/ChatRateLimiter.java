package com.anonymous.service.chatbot;

import com.anonymous.configuration.ChatbotProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Questions per user per minute (sliding window); protects against spam and high database load
@Component
public class ChatRateLimiter {

    private static final long WINDOW_MILLIS = 60_000;

    private final ChatbotProperties properties;
    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    public ChatRateLimiter(ChatbotProperties properties) {
        this.properties = properties;
    }

    public boolean tryAcquire(String userId) {
        long now = System.currentTimeMillis();
        Deque<Long> times = requests.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > WINDOW_MILLIS) {
                times.pollFirst();
            }
            if (times.size() >= properties.getRequestsPerMinute()) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

}
