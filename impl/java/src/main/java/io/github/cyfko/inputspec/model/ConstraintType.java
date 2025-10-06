package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumerates known atomic constraint types. Unknown types map to UNKNOWN to allow forward compatibility.
 */
public enum ConstraintType {
    PATTERN("pattern"),
    MIN_LENGTH("minLength"),
    MAX_LENGTH("maxLength"),
    MIN_VALUE("minValue"),
    MAX_VALUE("maxValue"),
    MIN_DATE("minDate"),
    MAX_DATE("maxDate"),
    RANGE("range"),
    CUSTOM("custom"), // semantic custom hook (params should include a key)
    UNKNOWN("__unknown__");

    private final String wire;
    ConstraintType(String wire) { this.wire = wire; }

    @JsonValue
    public String toWire() { return wire; }

    @JsonCreator
    public static ConstraintType fromWire(String v) {
        if (v == null) return UNKNOWN;
        for (ConstraintType ct : values()) {
            if (ct.wire.equalsIgnoreCase(v)) return ct;
        }
        return UNKNOWN;
    }
}
