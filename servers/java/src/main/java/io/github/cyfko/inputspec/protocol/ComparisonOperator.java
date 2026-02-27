package io.github.cyfko.inputspec.protocol;// ─── ComparisonOperator ───────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Comparison operators for FIELD_COMPARISON cross-constraints (§2.10).
 * JSON values are lowercase two-letter codes: "lt", "lte", "gt", "gte", "eq", "neq".
 */
public enum ComparisonOperator {
    LT ("lt"),
    LTE("lte"),
    GT ("gt"),
    GTE("gte"),
    EQ ("eq"),
    NEQ("neq");

    public final String jsonValue;

    ComparisonOperator(String jsonValue) { this.jsonValue = jsonValue; }

    @JsonCreator
    public static ComparisonOperator fromJson(String v) {
        if (v == null) return GT;
        for (ComparisonOperator op : values()) {
            if (op.jsonValue.equalsIgnoreCase(v)) return op;
        }
        return GT; // safe default
    }

    @JsonValue
    public String toJson() { return jsonValue; }
}