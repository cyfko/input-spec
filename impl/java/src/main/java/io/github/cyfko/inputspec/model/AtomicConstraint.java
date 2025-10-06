package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Minimal representation of a single constraint pairing a {@link ConstraintType} with an
 * optional raw {@code value}. Used in simplified contexts or migrations where the richer
 * {@link ConstraintDescriptor} structure is not required.
 * @since 2.0.0
 */
public class AtomicConstraint {
    /** Constraint semantic category. */
    private final ConstraintType type;
    /** Raw value / parameter for this constraint (nullable). */
    private final Object value;

    private AtomicConstraint(Builder builder) {
        this.type = builder.type;
        this.value = builder.value;
    }

    /**
     * Jackson / programmatic constructor.
     * @since 2.0.0
     */
    @JsonCreator
    public AtomicConstraint(
            @JsonProperty("type") ConstraintType type,
            @JsonProperty("value") Object value) {
        this.type = type;
        this.value = value;
    }
    /** @return constraint type. @since 2.0.0 */
    public ConstraintType getType() { return type; }
    /** @return raw constraint value. @since 2.0.0 */
    public Object getValue() { return value; }

    /** Create a new builder. @since 2.0.0 */
    public static Builder builder() { return new Builder(); }

    /** Fluent builder for {@link AtomicConstraint}. @since 2.0.0 */
    public static class Builder {
        private ConstraintType type;
        private Object value;
        /** @since 2.0.0 */ public Builder type(ConstraintType type) { this.type = type; return this; }
        /** @since 2.0.0 */ public Builder value(Object value) { this.value = value; return this; }
        /** @since 2.0.0 */ public AtomicConstraint build() { return new AtomicConstraint(this); }
    }
}
