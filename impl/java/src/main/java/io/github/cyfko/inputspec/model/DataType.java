package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Primitive data types supported by protocol v2 field specifications.
 * <p>These describe the shape of a single atomic value. Multi-valued fields reuse the
 * same atomic {@link DataType} for each element.</p>
 * @since 2.0.0
 */
public enum DataType {
    STRING,
    NUMBER,
    DATE,
    BOOLEAN;

    /**
     * Create from case-insensitive string; returns {@code null} for null input.
     * @since 2.0.0
     */
    @JsonCreator
    public static DataType fromString(String value) {
        return value == null ? null : DataType.valueOf(value.toUpperCase());
    }
    /**
     * Serialize using canonical uppercase token.
     * @since 2.0.0
     */
    @JsonValue
    public String toValue() {
        return name(); // keep canonical uppercase for wire format
    }
}
