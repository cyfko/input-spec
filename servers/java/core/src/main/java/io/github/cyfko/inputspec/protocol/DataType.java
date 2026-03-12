package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The primitive type of a field value (§2.1 — {@code InputFieldSpec.dataType}).
 *
 * <p>Determines how the client should render the field and how the validator
 * interprets constraint parameters. {@code OBJECT} fields carry
 * {@link io.github.cyfko.inputspec.model.InputFieldSpec#subFields() subFields};
 * all other types are scalar or array-of-scalar when
 * {@link io.github.cyfko.inputspec.model.InputFieldSpec#expectMultipleValues() expectMultipleValues}
 * is {@code true}.</p>
 *
 * @see io.github.cyfko.inputspec.model.InputFieldSpec
 */
public enum DataType {

    /** Free-form text — the default type for most fields. */
    STRING,

    /** Numeric value — integer or decimal, validated by {@code minValue}/{@code maxValue}. */
    NUMBER,

    /** True/false value — rendered as a checkbox or toggle. */
    BOOLEAN,

    /** Calendar date — validated by {@code minDate}/{@code maxDate} constraints. */
    DATE,

    /** Composite type — the field contains nested {@code subFields} (recursive structure). */
    OBJECT;

    /**
     * Deserializes a JSON string to the corresponding {@code DataType}.
     *
     * <p>Case-insensitive. Returns {@link #STRING} as a safe fallback
     * for {@code null} or unrecognized values — an unknown type is
     * treated as an opaque string by the client.</p>
     *
     * @param v the JSON string value (may be {@code null})
     * @return the matching enum constant, or {@link #STRING} as default
     */
    @JsonCreator
    public static DataType fromJson(String v) {
        if (v == null) return STRING;
        return switch (v.toUpperCase()) {
            case "STRING"  -> STRING;
            case "NUMBER"  -> NUMBER;
            case "BOOLEAN" -> BOOLEAN;
            case "DATE"    -> DATE;
            case "OBJECT"  -> OBJECT;
            default        -> STRING;
        };
    }
}
