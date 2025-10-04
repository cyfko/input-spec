package io.github.cyfko.inputspec.validation;

import java.util.Objects;

/**
 * Represents a validation error with constraint name and message
 */
public class ValidationError {
    
    private final String constraintName;
    private final String message;
    private final Object value;
    
    /**
     * Constructor for validation error
     * 
     * @param constraintName name of the constraint that failed
     * @param message error message
     * @param value the value that failed validation
     */
    public ValidationError(String constraintName, String message, Object value) {
        this.constraintName = constraintName;
        this.message = message;
        this.value = value;
    }
    
    /**
     * Constructor for validation error without value
     * 
     * @param constraintName name of the constraint that failed
     * @param message error message
     */
    public ValidationError(String constraintName, String message) {
        this(constraintName, message, null);
    }
    
    /**
     * Gets the name of the constraint that failed
     * 
     * @return constraint name
     */
    public String getConstraintName() {
        return constraintName;
    }
    
    /**
     * Gets the error message
     * 
     * @return error message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the value that failed validation
     * 
     * @return the value (may be null)
     */
    public Object getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ValidationError that = (ValidationError) obj;
        return Objects.equals(constraintName, that.constraintName) &&
               Objects.equals(message, that.message) &&
               Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(constraintName, message, value);
    }
    
    @Override
    public String toString() {
        return "ValidationError{" +
                "constraintName='" + constraintName + '\'' +
                ", message='" + message + '\'' +
                ", value=" + value +
                '}';
    }
}