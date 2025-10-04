package io.github.cyfko.inputspec.client;

/**
 * Cache provider abstraction for caching values according to the protocol's cache strategies.
 * 
 * From the specification:
 * - NONE: No caching
 * - SESSION: Session-based caching (no expiration)
 * - SHORT_TERM: 5 minutes cache
 * - LONG_TERM: 1 hour cache
 */
public interface CacheProvider {
    
    /**
     * Gets a cached value
     * 
     * @param key the cache key
     * @param <T> the type of the cached value
     * @return the cached value or null if not found or expired
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * Sets a cached value with optional TTL
     * 
     * @param key the cache key
     * @param value the value to cache
     * @param ttlMs time to live in milliseconds (null means no expiration)
     * @param <T> the type of the value
     */
    <T> void set(String key, T value, Long ttlMs);
    
    /**
     * Sets a cached value without expiration
     * 
     * @param key the cache key
     * @param value the value to cache
     * @param <T> the type of the value
     */
    default <T> void set(String key, T value) {
        set(key, value, null);
    }
    
    /**
     * Removes a cached value
     * 
     * @param key the cache key
     */
    void delete(String key);
    
    /**
     * Clears all cached values
     */
    void clear();
    
    /**
     * Checks if a key exists in cache and is not expired
     * 
     * @param key the cache key
     * @return true if key exists and is valid
     */
    boolean exists(String key);
}