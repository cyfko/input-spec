package io.github.cyfko.inputspec.protocol;// ─── CacheStrategy ────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Client-side caching behaviour for a ValuesEndpoint (§2.2).
 *
 * SESSION    — cached for the lifetime of the browser session
 * SHORT_TERM — cached for ~5 minutes
 * LONG_TERM  — cached for ~1 hour
 * NONE       — never cached; every interaction triggers a fresh fetch
 */
public enum CacheStrategy {
    NONE, SESSION, SHORT_TERM, LONG_TERM;

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