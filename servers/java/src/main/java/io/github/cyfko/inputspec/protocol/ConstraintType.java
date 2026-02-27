package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The registered constraint handlers (§2.7 — {@code ConstraintDescriptor.type}).
 *
 * <p>Each constant maps to a specific validation rule that the
 * {@link io.github.cyfko.inputspec.validation.FormSpecValidator} knows how to evaluate.
 * The annotation processor maps Jakarta Validation annotations to these types:</p>
 *
 * <ul>
 *   <li>{@code @Pattern}              → {@link #PATTERN}</li>
 *   <li>{@code @Size(min=...)}        → {@link #MIN_LENGTH}</li>
 *   <li>{@code @Size(max=...)}        → {@link #MAX_LENGTH}</li>
 *   <li>{@code @Size(min=..,max=..)}  → {@link #RANGE}</li>
 *   <li>{@code @Min}                  → {@link #MIN_VALUE}</li>
 *   <li>{@code @Max}                  → {@link #MAX_VALUE}</li>
 *   <li>{@code @Past}                 → {@link #MAX_DATE}</li>
 *   <li>{@code @Future}               → {@link #MIN_DATE}</li>
 * </ul>
 *
 * <p>{@link #UNKNOWN} is the forward-compatibility sentinel: the validator silently
 * ignores constraints whose type it does not recognize, allowing the protocol
 * to be extended without breaking older validators.</p>
 *
 * @see io.github.cyfko.inputspec.model.ConstraintDescriptor
 * @see io.github.cyfko.inputspec.validation.FormSpecValidator
 */
public enum ConstraintType {

    /** Validates that the value matches a regular expression. Params: {@code {"value": "regex"}} */
    PATTERN      ("pattern"),

    /** Validates a minimum string length. Params: {@code {"value": N}} */
    MIN_LENGTH   ("minLength"),

    /** Validates a maximum string length. Params: {@code {"value": N}} */
    MAX_LENGTH   ("maxLength"),

    /** Validates a minimum numeric value. Params: {@code {"value": N}} */
    MIN_VALUE    ("minValue"),

    /** Validates a maximum numeric value. Params: {@code {"value": N}} */
    MAX_VALUE    ("maxValue"),

    /** Validates that a date is not before a minimum date. Params: {@code {"value": "ISO-8601"}} */
    MIN_DATE     ("minDate"),

    /** Validates that a date is not after a maximum date. Params: {@code {"value": "ISO-8601"}} */
    MAX_DATE     ("maxDate"),

    /** Validates that a value falls within a range. Params: {@code {"min": N, "max": M}} */
    RANGE        ("range"),

    /** Delegates to a registered {@link io.github.cyfko.inputspec.validation.CustomConstraintHandler}. */
    CUSTOM       ("custom"),

    /** Forward-compatibility sentinel — unknown types are silently ignored by the validator. */
    UNKNOWN      (null);

    /** The camelCase string used in the JSON spec (e.g. {@code "minLength"}, {@code "maxValue"}). */
    public final String jsonValue;

    ConstraintType(String jsonValue) { this.jsonValue = jsonValue; }

    /**
     * Deserializes a JSON string to the corresponding {@code ConstraintType}.
     *
     * <p>Matches against the {@link #jsonValue} of each constant.
     * Returns {@link #UNKNOWN} for {@code null} or unrecognized values,
     * enabling forward-compatible protocol extensions.</p>
     *
     * @param v the JSON string value (may be {@code null})
     * @return the matching enum constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static ConstraintType fromJson(String v) {
        if (v == null) return UNKNOWN;
        for (ConstraintType t : values()) {
            if (v.equals(t.jsonValue)) return t;
        }
        return UNKNOWN;
    }

    /** Serializes this constant to its JSON representation. */
    @JsonValue
    public String toJson() { return jsonValue; }
}