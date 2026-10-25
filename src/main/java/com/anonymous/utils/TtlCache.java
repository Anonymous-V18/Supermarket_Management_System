package com.anonymous.utils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

// Small in-process LRU cache with expiry; enough for the chatbot's repeated answers
// without adding a cache server.
public final class TtlCache<K, V> {

    private record Slot<V>(V value, long expiresAt) {
    }

    private final long ttlMillis;
    private final Map<K, Slot<V>> entries;

    public TtlCache(int maxEntries, Duration ttl) {
        this.ttlMillis = ttl.toMillis();
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Slot<V>> eldest) {
                return size() > maxEntries;
            }
        };
    }

    public synchronized V get(K key) {
        Slot<V> entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt() < System.currentTimeMillis()) {
            entries.remove(key);
            return null;
        }
        return entry.value();
    }

    public synchronized void put(K key, V value) {
        entries.put(key, new Slot<>(value, System.currentTimeMillis() + ttlMillis));
    }

    // The loader runs outside the lock (it may call a remote API); a concurrent miss just loads twice
    public V computeIfAbsent(K key, Function<K, V> loader) {
        V cached = get(key);
        if (cached != null) {
            return cached;
        }
        V loaded = loader.apply(key);
        if (loaded != null) {
            put(key, loaded);
        }
        return loaded;
    }

    public synchronized void clear() {
        entries.clear();
    }

}
