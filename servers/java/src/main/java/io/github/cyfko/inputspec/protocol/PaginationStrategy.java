package io.github.cyfko.inputspec.protocol;// ─── PaginationStrategy ───────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * How the remote endpoint is paginated (§2.2 — ValuesEndpoint.paginationStrategy).
 */
public enum PaginationStrategy {
    NONE, PAGE_NUMBER;

    @JsonCreator
    public static PaginationStrategy fromJson(String v) {
        if (v == null) return NONE;
        return switch (v.toUpperCase()) {
            case "PAGE_NUMBER" -> PAGE_NUMBER;
            default            -> NONE;
        };
    }
}