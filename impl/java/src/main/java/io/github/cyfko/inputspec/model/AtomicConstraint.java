package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AtomicConstraint {
    private final ConstraintType type;
    private final Object value;

    private AtomicConstraint(Builder builder) {
        this.type = builder.type;
        this.value = builder.value;
    }

    @JsonCreator
    public AtomicConstraint(
            @JsonProperty("type") ConstraintType type,
            @JsonProperty("value") Object value) {
        this.type = type;
        this.value = value;
    }

    public ConstraintType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ConstraintType type;
        private Object value;

        public Builder type(ConstraintType type) {
            this.type = type;
            return this;
        }

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public AtomicConstraint build() {
            return new AtomicConstraint(this);
        }
    }
}
