package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Client-side caching behaviour for a remote {@link io.github.cyfko.inputspec.model.ValuesEndpoint} (§2.2).
 *
 * <p>Provides a hint to the UI client on how aggressively to cache the values
 * fetched from a remote endpoint. The actual TTL is determined by the client
 * implementation — these constants represent recommended caching tiers.</p>
 *
 * @see io.github.cyfko.inputspec.model.ValuesEndpoint
 */
public enum CacheStrategy {

    /** Never cached — every user interaction triggers a fresh network call. */
    NONE,

    /** Cached for the lifetime of the browser session (tab/window). */
    SESSION,

    /** Cached for a short period (~5 minutes) — suitable for slowly changing data. */
    SHORT_TERM,

    /** Cached for a longer period (~1 hour) — suitable for rarely changing reference data. */
    LONG_TERM;

    /**
     * Deserializes a JSON string to the corresponding {@code CacheStrategy}.
     *
     * <p>Case-insensitive. Returns {@link #NONE} as the default
     * for {@code null} or unrecognized values — unknown strategies
     * default to no caching for safety.</p>
     *
     * @param v the JSON string value (may be {@code null})
     * @return the matching enum constant, or {@link #NONE} as default
     */
    @JsonCreator
    public static CacheStrategy fromJson(String v) {
        if (v == null) return NONE;
        return switch (v.toUpperCase()) {
            case "SESSION"    -> SESSION;
            case "SHORT_TERM" -> SHORT_TERM;
            case "LONG_TERM"  -> LONG_TERM;
            default           -> NONE;
        };
    }
}