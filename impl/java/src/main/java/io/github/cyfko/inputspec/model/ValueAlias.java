package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pairing of a canonical machine value with a human friendly label used in enumerations or
 * suggestion domains.
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValueAlias {
    /** Canonical value transmitted in payloads. */
    private final Object value; // canonical value
    /** User facing label for display. */
    private final String label; // user visible label

    private ValueAlias(Builder b) {
        this.value = b.value;
        this.label = b.label;
    }

    /**
     * Jackson / programmatic constructor.
     * @since 2.0.0
     */
    @JsonCreator
    public ValueAlias(
            @JsonProperty(value = "value", required = true) Object value,
            @JsonProperty(value = "label", required = true) String label) {
        this.value = value;
        this.label = label;
    }
    /** @return canonical value. @since 2.0.0 */
    public Object getValue() { return value; }
    /** @return display label. @since 2.0.0 */
    public String getLabel() { return label; }

    /** @since 2.0.0 */
    public static Builder builder() { return new Builder(); }

    /** Fluent builder for {@link ValueAlias}. @since 2.0.0 */
    public static final class Builder {
        private Object value;
        private String label;
        /** @since 2.0.0 */ public Builder value(Object value) { this.value = value; return this; }
        /** @since 2.0.0 */ public Builder label(String label) { this.label = label; return this; }
        /** @since 2.0.0 */ public ValueAlias build() { return new ValueAlias(this); }
    }
}
