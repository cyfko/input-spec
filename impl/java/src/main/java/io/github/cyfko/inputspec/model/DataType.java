package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Data types supported by the protocol v2.
 */
public enum DataType {
    STRING,
    NUMBER,
    DATE,
    BOOLEAN;

    @JsonCreator
    public static DataType fromString(String value) {
        return value == null ? null : DataType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name(); // keep canonical uppercase for wire format
    }
}
