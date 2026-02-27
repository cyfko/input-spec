package io.github.cyfko.inputspec.protocol;
// ─── ConstraintType ───────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The registered constraint handlers (§2.7 — ConstraintDescriptor.type).
 *
 * UNKNOWN is the tolerance sentinel for forward-compatibility:
 * the validator silently ignores constraints whose type it does not recognise,
 * allowing the protocol to be extended without breaking older validators.
 */
public enum ConstraintType {
    PATTERN      ("pattern"),
    MIN_LENGTH   ("minLength"),
    MAX_LENGTH   ("maxLength"),
    MIN_VALUE    ("minValue"),
    MAX_VALUE    ("maxValue"),
    MIN_DATE     ("minDate"),
    MAX_DATE     ("maxDate"),
    RANGE        ("range"),
    CUSTOM       ("custom"),
    UNKNOWN      (null);          // forward-compatibility sentinel

    /** The camelCase string used in the JSON spec. */
    public final String jsonValue;

    ConstraintType(String jsonValue) { this.jsonValue = jsonValue; }

    @JsonCreator
    public static ConstraintType fromJson(String v) {
        if (v == null) return UNKNOWN;
        for (ConstraintType t : values()) {
            if (v.equals(t.jsonValue)) return t;
        }
        return UNKNOWN; // tolerate future extensions
    }

    @JsonValue
    public String toJson() { return jsonValue; }
}