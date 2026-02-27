package io.github.cyfko.inputspec.protocol;
// ─── CrossConstraintType ─────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The cross-field constraint handlers (§2.10 — CrossConstraintDescriptor.type).
 * UNKNOWN tolerates future protocol extensions.
 */
public enum CrossConstraintType {
    FIELD_COMPARISON  ("fieldComparison"),
    AT_LEAST_ONE      ("atLeastOne"),
    MUTUALLY_EXCLUSIVE("mutuallyExclusive"),
    DEPENDS_ON        ("dependsOn"),
    CUSTOM            ("custom"),
    UNKNOWN           (null);

    public final String jsonValue;

    CrossConstraintType(String jsonValue) { this.jsonValue = jsonValue; }

    @JsonCreator
    public static CrossConstraintType fromJson(String v) {
        if (v == null) return UNKNOWN;
        for (CrossConstraintType t : values()) {
            if (v.equals(t.jsonValue)) return t;
        }
        return UNKNOWN;
    }

    @JsonValue
    public String toJson() { return jsonValue; }
}