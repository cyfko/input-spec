package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValueAlias {
    private final Object value; // canonical value
    private final String label; // user visible label

    private ValueAlias(Builder b) {
        this.value = b.value;
        this.label = b.label;
    }

    @JsonCreator
    public ValueAlias(
            @JsonProperty(value = "value", required = true) Object value,
            @JsonProperty(value = "label", required = true) String label) {
        this.value = value;
        this.label = label;
    }

    public Object getValue() { return value; }
    public String getLabel() { return label; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Object value;
        private String label;
        public Builder value(Object value) { this.value = value; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public ValueAlias build() { return new ValueAlias(this); }
    }
}
