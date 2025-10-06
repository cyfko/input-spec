package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumerates the known atomic constraint categories used inside a field's constraint list.
 * <p>Values are serialized to / from their wire names (lower camel / specific tokens) and any
 * unrecognized future value is mapped to {@link #UNKNOWN} so that clients can still process
 * the remainder of a specification without failing hard.</p>
 * <p>Type semantics summary:
 * <ul>
 *   <li>{@link #PATTERN}: value must match a regular expression;</li>
 *   <li>{@link #MIN_LENGTH}/{@link #MAX_LENGTH}: size bounds applied to multi-valued inputs only (single values ignored);</li>
 *   <li>{@link #MIN_VALUE}/{@link #MAX_VALUE}: numeric lower / upper bound;</li>
 *   <li>{@link #MIN_DATE}/{@link #MAX_DATE}: inclusive temporal bounds (ISO-8601 date literal);</li>
 *   <li>{@link #RANGE}: combined inclusive lower/upper numeric bounds;</li>
 *   <li>{@link #CUSTOM}: user defined semantics, params shape agreed out-of-band;</li>
 *   <li>{@link #UNKNOWN}: forward compatibility bucket.</li>
 * </ul>
 * @since 2.0.0
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

    /**
     * Serialize to canonical wire token.
     * @since 2.0.0
     */
    @JsonValue
    public String toWire() { return wire; }

    /**
     * Lenient factory from a wire token (case insensitive). Unknown tokens -> {@link #UNKNOWN}.
     * @param v raw wire value (may be null)
     * @return resolved enum constant or {@link #UNKNOWN}
     * @since 2.0.0
     */
    @JsonCreator
    public static ConstraintType fromWire(String v) {
        if (v == null) return UNKNOWN;
        for (ConstraintType ct : values()) {
            if (ct.wire.equalsIgnoreCase(v)) return ct;
        }
        return UNKNOWN;
    }
}
