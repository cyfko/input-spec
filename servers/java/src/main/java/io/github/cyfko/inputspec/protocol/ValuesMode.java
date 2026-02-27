package io.github.cyfko.inputspec.protocol;// ─── ValuesMode ───────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Whether the ValuesEndpoint defines a closed domain (membership enforced)
 * or suggestions only (free input permitted — §2.2).
 */
public enum ValuesMode {
    CLOSED, SUGGESTIONS;

    @JsonCreator
    public static ValuesMode fromJson(String v) {
        if (v == null) return CLOSED;
        return switch (v.toUpperCase()) {
            case "SUGGESTIONS" -> SUGGESTIONS;
            default            -> CLOSED;
        };
    }
}