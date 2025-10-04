package io.github.cyfko.inputspec.validation;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a validation result with status and errors
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<ValidationError> errors;
    
    /**
     * Constructor for validation result
     * 
     * @param valid true if validation passed
     * @param errors list of validation errors (empty if valid)
     */
    public ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : new ArrayList<>();
    }
    
    /**
     * Gets whether validation passed
     * 
     * @return true if valid
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Gets the list of validation errors
     * 
     * @return list of errors (empty if valid)
     */
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", errors=" + errors +
                '}';
    }
}