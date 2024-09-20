package intellijautocompleteplugin.cache;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, V> cache;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<K, V>(capacity) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.capacity;
            }
        };
    }

    public synchronized  V get(K key) {
        return cache.getOrDefault(key, null);
    }

    public synchronized void put(K key, V value) {
        cache.put(key, value);
    }

    public synchronized boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    public synchronized Collection<K> keySet() {
        return cache.keySet();
    }

    public synchronized Set<Map.Entry<K, V>> entrySet() {
        return cache.entrySet();
    }
}