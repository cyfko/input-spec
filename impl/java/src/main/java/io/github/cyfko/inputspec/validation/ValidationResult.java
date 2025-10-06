package io.github.cyfko.inputspec.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Immutable outcome of a single field validation run.
 * <p>Contains a boolean success flag and the (possibly empty) ordered list of
 * {@link ValidationError} instances describing each failure. A successful validation MAY still
 * carry an empty list instance (never {@code null}) for ergonomic iteration, but the model does not
 * guarantee non-null here to remain lenient with external JSON producers. Callers should treat
 * {@code errors == null} as no errors.</p>
 * <p>Serialization keeps a simple, flat shape suitable for transport to a client.</p>
 *
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValidationResult {
    /** Overall success indicator (true when no constraint violations recorded). */
    private final boolean isValid;
    /** Ordered list of validation errors (may be null or empty when {@link #isValid()} is true). */
    private final List<ValidationError> errors;

    /**
     * Jackson / programmatic constructor.
     * @param isValid success flag
     * @param errors ordered list of errors (may be null)
     * @since 2.0.0
     */
    @JsonCreator
    public ValidationResult(
            @JsonProperty("isValid") boolean isValid,
            @JsonProperty("errors") List<ValidationError> errors) {
        this.isValid = isValid;
        this.errors = errors;
    }

    /**
     * @return true if validation produced no failures.
     * @since 2.0.0
     */
    @JsonProperty("isValid")
    public boolean isValid() { return isValid; }

    /**
     * @return ordered list of validation errors or null.
     * @since 2.0.0
     */
    public List<ValidationError> getErrors() {
        return errors;
    }
}
