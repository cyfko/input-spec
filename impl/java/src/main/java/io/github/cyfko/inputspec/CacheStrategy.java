package io.github.cyfko.inputspec;

/**
 * Cache strategies as defined in the protocol specification.
 * 
 * From the specification:
 * - NONE: No caching
 * - SESSION: Session-based caching (no expiration)
 * - SHORT_TERM: 5 minutes cache
 * - LONG_TERM: 1 hour cache
 */
public enum CacheStrategy {
    /**
     * No caching - always fetch fresh data
     */
    NONE,
    
    /**
     * Session-based caching - cache for the duration of the session
     */
    SESSION,
    
    /**
     * Short-term caching - 5 minutes TTL
     */
    SHORT_TERM,
    
    /**
     * Long-term caching - 1 hour TTL
     */
    LONG_TERM
}