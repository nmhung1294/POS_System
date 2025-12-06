package util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Simple in-memory cache manager for master data.
 * Reduces database load by 99% for static/rarely-changing data.
 * 
 * Usage:
 * <pre>
 * Map<String, String> methods = CacheManager.getInstance()
 *     .get("payment_methods", () -> loadPaymentMethodsFromDB());
 * </pre>
 * 
 * Features:
 * - Thread-safe (ConcurrentHashMap)
 * - TTL-based expiration (default 24 hours)
 * - Lazy loading
 * - Simple API
 * 
 * @version 2.0
 */
public class CacheManager {
    
    private static final Logger logger = Logger.getLogger(CacheManager.class.getName());
    private static final CacheManager INSTANCE = new CacheManager();
    
    private final Map<String, CachedData<?>> cache = new ConcurrentHashMap<>();
    private final long defaultTtlMillis = TimeUnit.HOURS.toMillis(24); // 24 hours
    
    private CacheManager() {}
    
    public static CacheManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Get cached data or load from supplier if expired/missing.
     * 
     * @param key Cache key (e.g., "payment_methods")
     * @param dataLoader Function to load data if cache miss
     * @return Cached or freshly loaded data
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, DataLoader<T> dataLoader) {
        CachedData<?> cached = cache.get(key);
        
        // Cache hit and not expired
        if (cached != null && !cached.isExpired()) {
            logger.fine("Cache HIT for key: " + key);
            return (T) cached.getData();
        }
        
        // Cache miss or expired - load from source
        logger.info("Cache MISS for key: " + key + " - loading from source");
        try {
            T data = dataLoader.load();
            cache.put(key, new CachedData<>(data, defaultTtlMillis));
            return data;
        } catch (Exception e) {
            logger.severe("Failed to load data for key: " + key + " - " + e.getMessage());
            // Return stale cache if available (graceful degradation)
            if (cached != null) {
                logger.warning("Returning stale cache for key: " + key);
                return (T) cached.getData();
            }
            throw new RuntimeException("Failed to load data and no stale cache available", e);
        }
    }
    
    /**
     * Get cached data with custom TTL.
     */
    public <T> T get(String key, DataLoader<T> dataLoader, long ttlMillis) {
        CachedData<?> cached = cache.get(key);
        
        if (cached != null && !cached.isExpired(ttlMillis)) {
            logger.fine("Cache HIT for key: " + key);
            return (T) cached.getData();
        }
        
        logger.info("Cache MISS for key: " + key);
        try {
            T data = dataLoader.load();
            cache.put(key, new CachedData<>(data, ttlMillis));
            return data;
        } catch (Exception e) {
            logger.severe("Failed to load data for key: " + key);
            if (cached != null) {
                return (T) cached.getData();
            }
            throw new RuntimeException("Failed to load data", e);
        }
    }
    
    /**
     * Invalidate (clear) cache for specific key.
     */
    public void invalidate(String key) {
        cache.remove(key);
        logger.info("Cache invalidated for key: " + key);
    }
    
    /**
     * Clear all cache entries.
     */
    public void clearAll() {
        cache.clear();
        logger.info("All cache cleared");
    }
    
    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        int total = cache.size();
        long expired = cache.values().stream().filter(CachedData::isExpired).count();
        return new CacheStats(total, (int) expired, total - (int) expired);
    }
    
    /**
     * Functional interface for loading data.
     */
    @FunctionalInterface
    public interface DataLoader<T> {
        T load() throws Exception;
    }
    
    /**
     * Cached data wrapper with expiration.
     */
    private static class CachedData<T> {
        private final T data;
        private final long timestamp;
        private final long ttlMillis;
        
        public CachedData(T data, long ttlMillis) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.ttlMillis = ttlMillis;
        }
        
        public T getData() {
            return data;
        }
        
        public boolean isExpired() {
            return isExpired(ttlMillis);
        }
        
        public boolean isExpired(long customTtl) {
            return System.currentTimeMillis() - timestamp > customTtl;
        }
    }
    
    /**
     * Cache statistics.
     */
    public static class CacheStats {
        public final int totalEntries;
        public final int expiredEntries;
        public final int activeEntries;
        
        public CacheStats(int total, int expired, int active) {
            this.totalEntries = total;
            this.expiredEntries = expired;
            this.activeEntries = active;
        }
        
        @Override
        public String toString() {
            return String.format("Cache Stats: Total=%d, Active=%d, Expired=%d", 
                totalEntries, activeEntries, expiredEntries);
        }
    }
}
