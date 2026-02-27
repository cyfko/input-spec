package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Comparison operators for {@link CrossConstraintType#FIELD_COMPARISON}
 * cross-constraints (§2.10).
 *
 * <p>When a cross-constraint of type {@code FIELD_COMPARISON} is declared with
 * {@code fields = {"endDate", "startDate"}} and {@code operator = GT}, the rule is:
 * <em>"endDate must be greater than startDate"</em>.</p>
 *
 * <p>JSON values are lowercase two-letter or three-letter codes:
 * {@code "lt"}, {@code "lte"}, {@code "gt"}, {@code "gte"}, {@code "eq"}, {@code "neq"}.</p>
 *
 * @see CrossConstraintType#FIELD_COMPARISON
 * @see io.github.cyfko.inputspec.model.CrossConstraintDescriptor
 */
public enum ComparisonOperator {

    /** Strictly less than ({@code fieldA < fieldB}). */
    LT ("lt"),

    /** Less than or equal ({@code fieldA <= fieldB}). */
    LTE("lte"),

    /** Strictly greater than ({@code fieldA > fieldB}) — the default operator. */
    GT ("gt"),

    /** Greater than or equal ({@code fieldA >= fieldB}). */
    GTE("gte"),

    /** Equality ({@code fieldA == fieldB}). */
    EQ ("eq"),

    /** Inequality ({@code fieldA != fieldB}). */
    NEQ("neq");

    /** The lowercase JSON representation (e.g. {@code "gt"}, {@code "neq"}). */
    public final String jsonValue;

    ComparisonOperator(String jsonValue) { this.jsonValue = jsonValue; }

    /**
     * Deserializes a JSON string to the corresponding {@code ComparisonOperator}.
     *
     * <p>Case-insensitive. Returns {@link #GT} as the default
     * for {@code null} or unrecognized values.</p>
     *
     * @param v the JSON string value (may be {@code null})
     * @return the matching enum constant, or {@link #GT} as default
     */
    @JsonCreator
    public static ComparisonOperator fromJson(String v) {
        if (v == null) return GT;
        for (ComparisonOperator op : values()) {
            if (op.jsonValue.equalsIgnoreCase(v)) return op;
        }
        return GT;
    }

    /** Serializes this constant to its JSON representation. */
    @JsonValue
    public String toJson() { return jsonValue; }
}