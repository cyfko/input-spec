package io.github.cyfko.inputspec.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Single validation failure detail produced during constraint evaluation.
 * <p>Captures the logical {@code constraintName} that produced the error, a human readable
 * {@code message}, the offending {@code value} (if relevant) and optionally an {@code index}
 * when the error pertains to a specific element in a multi-valued input.</p>
 * <p>Errors are intentionally lightweight and serializable as part of {@link ValidationResult}.</p>
 *
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValidationError {
    /** Logical name of the violated constraint (e.g. pattern, minLength, custom id). */
    private final String constraintName; // required
    /** Human readable diagnostic message (may originate from spec override). */
    private final String message; // required
    /** Offending value or contextual value (optional). */
    private final Object value; // offending value (optional)
    /** Element index for multi-valued inputs (null if not applicable). */
    private final Integer index; // present for multi-value element errors

    private ValidationError(Builder b) {
        this.constraintName = b.constraintName;
        this.message = b.message;
        this.value = b.value;
        this.index = b.index;
    }

    /**
     * Jackson / programmatic constructor.
     * @since 2.0.0
     */
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

    /** @return logical constraint name that failed. @since 2.0.0 */
    public String getConstraintName() { return constraintName; }
    /** @return human readable diagnostic message. @since 2.0.0 */
    public String getMessage() { return message; }
    /** @return offending value (may be null). @since 2.0.0 */
    public Object getValue() { return value; }
    /** @return element index for multi-value errors or null. @since 2.0.0 */
    public Integer getIndex() { return index; }

    /**
     * Create a builder for incremental construction.
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /** Fluent builder for {@link ValidationError}. @since 2.0.0 */
    public static final class Builder {
        private String constraintName;
        private String message;
        private Object value;
        private Integer index;
        /** @since 2.0.0 */ public Builder constraintName(String c) { this.constraintName = c; return this; }
        /** @since 2.0.0 */ public Builder message(String m) { this.message = m; return this; }
        /** @since 2.0.0 */ public Builder value(Object v) { this.value = v; return this; }
        /** @since 2.0.0 */ public Builder index(Integer i) { this.index = i; return this; }
        /** @since 2.0.0 */ public ValidationError build() { return new ValidationError(this); }
    }
}
