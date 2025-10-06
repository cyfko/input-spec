package io.github.cyfko.inputspec.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValidationResult {
    private final boolean isValid;
    private final List<ValidationError> errors;

    @JsonCreator
    public ValidationResult(
            @JsonProperty("isValid") boolean isValid,
            @JsonProperty("errors") List<ValidationError> errors) {
        this.isValid = isValid;
        this.errors = errors;
    }

    @JsonProperty("isValid")
    public boolean isValid() { return isValid; }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
