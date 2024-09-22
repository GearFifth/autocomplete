package intellijautocompleteplugin.cache;

import java.util.Collection;
import java.util.Optional;

public class CompletionCache {
    private static final int CACHE_CAPACITY = 20;
    public static final int CACHE_TTL = 10 * 60 * 1000; // 10 minutes
    private static final LRUCache<String, CacheEntry> cache = new LRUCache<>(CACHE_CAPACITY);

    public static Optional<CacheEntry> get(String key) {
        return Optional.ofNullable(cache.get(key));
    }

    public static void put(String key, CacheEntry entry) {
        cache.put(key, entry);
    }

    public static boolean isExpired(CacheEntry entry) {
        return System.currentTimeMillis() - entry.getTimestamp() > CACHE_TTL;
    }

    public static boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    public static Collection<String> getKeySet() {
        return cache.keySet();
    }
}
