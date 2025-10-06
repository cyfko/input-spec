package io.github.cyfko.inputspec.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValidationError {
    private final String constraintName; // required
    private final String message; // required
    private final Object value; // offending value (optional)
    private final Integer index; // present for multi-value element errors

    private ValidationError(Builder b) {
        this.constraintName = b.constraintName;
        this.message = b.message;
        this.value = b.value;
        this.index = b.index;
    }

    @JsonCreator
    public ValidationError(
            @JsonProperty(value = "constraintName", required = true) String constraintName,
            @JsonProperty(value = "message", required = true) String message,
            @JsonProperty("value") Object value,
            @JsonProperty("index") Integer index) {
        this.constraintName = constraintName;
        this.message = message;
        this.value = value;
        this.index = index;
    }

    public String getConstraintName() { return constraintName; }
    public String getMessage() { return message; }
    public Object getValue() { return value; }
    public Integer getIndex() { return index; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String constraintName;
        private String message;
        private Object value;
        private Integer index;
        public Builder constraintName(String c) { this.constraintName = c; return this; }
        public Builder message(String m) { this.message = m; return this; }
        public Builder value(Object v) { this.value = v; return this; }
        public Builder index(Integer i) { this.index = i; return this; }
        public ValidationError build() { return new ValidationError(this); }
    }
}
