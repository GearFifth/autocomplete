package intellijautocompleteplugin.cache;

public class CacheEntry {
    private final String value;
    private final long timestamp;

    public CacheEntry(String value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
