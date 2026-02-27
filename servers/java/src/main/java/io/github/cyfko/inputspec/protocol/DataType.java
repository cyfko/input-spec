/**
 * DIFSP protocol value sets — single source of truth.
 *
 * These enums are the canonical representation of every closed value set
 * defined in the protocol specification. They are used at three levels:
 *
 *   1. Annotations     (@FormSpec, @ValuesSource, …)  — SOURCE retention
 *   2. Runtime models  (InputFieldSpec, ValuesEndpoint, …) — Jackson deserialization
 *   3. Validator       (FormSpecValidator switch expressions) — exhaustiveness check
 *
 * Jackson deserialization uses @JsonCreator on each enum to handle:
 *   - case-insensitive matching
 *   - camelCase JSON values that differ from the Java UPPER_SNAKE name
 *     (e.g. "fieldComparison" → FIELD_COMPARISON)
 *   - unknown values gracefully via an UNKNOWN sentinel where the protocol
 *     mandates tolerance of future extensions
 */
package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// ─── DataType ─────────────────────────────────────────────────────────────────

/**
 * The primitive type of a field value (§2.1 — InputFieldSpec.dataType).
 * OBJECT fields carry subFields; all others are scalar or array-of-scalar.
 */
public enum DataType {
    STRING, NUMBER, BOOLEAN, DATE, OBJECT;

    @JsonCreator
    public static DataType fromJson(String v) {
        if (v == null) return STRING;
        return switch (v.toUpperCase()) {
            case "STRING"  -> STRING;
            case "NUMBER"  -> NUMBER;
            case "BOOLEAN" -> BOOLEAN;
            case "DATE"    -> DATE;
            case "OBJECT"  -> OBJECT;
            default        -> STRING; // safe fallback — unknown type treated as opaque string
        };
    }
}

