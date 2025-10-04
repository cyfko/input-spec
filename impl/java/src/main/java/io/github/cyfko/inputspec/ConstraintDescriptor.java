package io.github.cyfko.inputspec;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Describes a single constraint on parameter values as defined in the protocol specification.
 * 
 * Key points from the specification:
 * - Constraints are executed in order within the constraints array
 * - The semantics of min/max depend on the field's dataType and expectMultipleValues
 * - pattern and format apply per element (whether singleton or in array)
 * - All constraints present must be satisfied (logical AND)
 */
public class ConstraintDescriptor {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("defaultValue")
    private Object defaultValue;
    
    @JsonProperty("min")
    private Object min; // number for STRING length/NUMBER value, string for DATE
    
    @JsonProperty("max")
    private Object max; // number for STRING length/NUMBER value, string for DATE
    
    @JsonProperty("pattern")
    private String pattern;
    
    @JsonProperty("format")
    private String format;
    
    @JsonProperty("enumValues")
    private List<ValueAlias> enumValues;
    
    @JsonProperty("valuesEndpoint")
    private ValuesEndpoint valuesEndpoint;
    
    /**
     * Default constructor for JSON deserialization
     */
    public ConstraintDescriptor() {
    }
    
    /**
     * Constructor with required name
     * 
     * @param name unique identifier for this constraint
     */
    public ConstraintDescriptor(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Constraint name cannot be null or empty");
        }
        this.name = name;
    }
    
    /**
     * Gets the unique identifier for this constraint.
     * Used for validation ordering.
     * Required field according to the specification.
     * 
     * @return the constraint name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the unique identifier for this constraint
     * 
     * @param name the constraint name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the human-readable explanation of this constraint
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the human-readable explanation
     * 
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the error message if constraint not satisfied
     * 
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Sets the error message if constraint not satisfied
     * 
     * @param errorMessage the error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * Gets the default value if not provided
     * 
     * @return the default value
     */
    public Object getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * Sets the default value if not provided
     * 
     * @param defaultValue the default value
     */
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    /**
     * Gets the context-dependent minimum value.
     * 
     * Semantics depend on dataType and expectMultipleValues:
     * - STRING + single: minimum character count
     * - STRING + multiple: minimum number of elements in array
     * - NUMBER + single: minimum numeric value
     * - NUMBER + multiple: minimum number of elements in array
     * - DATE + single: minimum date value (ISO 8601)
     * - DATE + multiple: minimum number of elements in array
     * - BOOLEAN + multiple: minimum number of elements in array
     * 
     * @return the minimum value (number or string)
     */
    public Object getMin() {
        return min;
    }
    
    /**
     * Sets the context-dependent minimum value
     * 
     * @param min the minimum value
     */
    public void setMin(Object min) {
        this.min = min;
    }
    
    /**
     * Gets the context-dependent maximum value.
     * 
     * Semantics are the same as min but for maximum bounds.
     * 
     * @return the maximum value (number or string)
     */
    public Object getMax() {
        return max;
    }
    
    /**
     * Sets the context-dependent maximum value
     * 
     * @param max the maximum value
     */
    public void setMax(Object max) {
        this.max = max;
    }
    
    /**
     * Gets the regex pattern (STRING only, applies per element)
     * 
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }
    
    /**
     * Sets the regex pattern
     * 
     * @param pattern the pattern
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    /**
     * Gets the format hint (e.g., 'email', 'url', 'uuid', 'iso8601').
     * Applies per element.
     * 
     * @return the format
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * Sets the format hint
     * 
     * @param format the format
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Gets the fixed set of allowed values
     * 
     * @return the enum values
     */
    public List<ValueAlias> getEnumValues() {
        return enumValues;
    }
    
    /**
     * Sets the fixed set of allowed values
     * 
     * @param enumValues the enum values
     */
    public void setEnumValues(List<ValueAlias> enumValues) {
        this.enumValues = enumValues;
    }
    
    /**
     * Gets the configuration for fetching values dynamically
     * 
     * @return the values endpoint
     */
    public ValuesEndpoint getValuesEndpoint() {
        return valuesEndpoint;
    }
    
    /**
     * Sets the configuration for fetching values dynamically
     * 
     * @param valuesEndpoint the values endpoint
     */
    public void setValuesEndpoint(ValuesEndpoint valuesEndpoint) {
        this.valuesEndpoint = valuesEndpoint;
    }
    
    /**
     * Creates a new Builder instance for fluent constraint creation.
     * 
     * @param name unique identifier for this constraint
     * @return a new Builder instance
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }
    
    /**
     * Builder pattern for fluent ConstraintDescriptor creation.
     * 
     * Example usage:
     * <pre>
     * ConstraintDescriptor constraint = ConstraintDescriptor.builder("username")
     *     .description("Username validation")
     *     .min(3)
     *     .max(20)
     *     .pattern("^[a-zA-Z0-9_]+$")
     *     .errorMessage("Username must be 3-20 characters, alphanumeric only")
     *     .build();
     * </pre>
     */
    public static class Builder {
        private final ConstraintDescriptor constraint;
        
        /**
         * Creates a new Builder with the required constraint name.
         * 
         * @param name unique identifier for this constraint
         */
        public Builder(String name) {
            this.constraint = new ConstraintDescriptor(name);
        }
        
        /**
         * Sets the human-readable explanation of this constraint.
         * 
         * @param description the description
         * @return this builder for chaining
         */
        public Builder description(String description) {
            constraint.setDescription(description);
            return this;
        }
        
        /**
         * Sets the error message if constraint not satisfied.
         * 
         * @param errorMessage the error message
         * @return this builder for chaining
         */
        public Builder errorMessage(String errorMessage) {
            constraint.setErrorMessage(errorMessage);
            return this;
        }
        
        /**
         * Sets the default value if not provided.
         * 
         * @param defaultValue the default value
         * @return this builder for chaining
         */
        public Builder defaultValue(Object defaultValue) {
            constraint.setDefaultValue(defaultValue);
            return this;
        }
        
        /**
         * Sets the context-dependent minimum value.
         * 
         * @param min the minimum value
         * @return this builder for chaining
         */
        public Builder min(Object min) {
            constraint.setMin(min);
            return this;
        }
        
        /**
         * Sets the context-dependent maximum value.
         * 
         * @param max the maximum value
         * @return this builder for chaining
         */
        public Builder max(Object max) {
            constraint.setMax(max);
            return this;
        }
        
        /**
         * Sets the regex pattern (STRING only, applies per element).
         * 
         * @param pattern the regex pattern
         * @return this builder for chaining
         */
        public Builder pattern(String pattern) {
            constraint.setPattern(pattern);
            return this;
        }
        
        /**
         * Sets the format hint (e.g., 'email', 'url', 'uuid', 'iso8601').
         * 
         * @param format the format hint
         * @return this builder for chaining
         */
        public Builder format(String format) {
            constraint.setFormat(format);
            return this;
        }
        
        /**
         * Sets the fixed set of allowed values.
         * 
         * @param enumValues the enum values
         * @return this builder for chaining
         */
        public Builder enumValues(List<ValueAlias> enumValues) {
            constraint.setEnumValues(enumValues);
            return this;
        }
        
        /**
         * Sets the configuration for fetching values dynamically.
         * 
         * @param valuesEndpoint the values endpoint
         * @return this builder for chaining
         */
        public Builder valuesEndpoint(ValuesEndpoint valuesEndpoint) {
            constraint.setValuesEndpoint(valuesEndpoint);
            return this;
        }
        
        /**
         * Builds and returns the configured ConstraintDescriptor.
         * 
         * @return the constructed ConstraintDescriptor
         */
        public ConstraintDescriptor build() {
            return constraint;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConstraintDescriptor that = (ConstraintDescriptor) obj;
        return Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(errorMessage, that.errorMessage) &&
               Objects.equals(defaultValue, that.defaultValue) &&
               Objects.equals(min, that.min) &&
               Objects.equals(max, that.max) &&
               Objects.equals(pattern, that.pattern) &&
               Objects.equals(format, that.format) &&
               Objects.equals(enumValues, that.enumValues) &&
               Objects.equals(valuesEndpoint, that.valuesEndpoint);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, description, errorMessage, defaultValue, min, max,
                pattern, format, enumValues, valuesEndpoint);
    }
    
    @Override
    public String toString() {
        return "ConstraintDescriptor{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", defaultValue=" + defaultValue +
                ", min=" + min +
                ", max=" + max +
                ", pattern='" + pattern + '\'' +
                ", format='" + format + '\'' +
                ", enumValues=" + enumValues +
                ", valuesEndpoint=" + valuesEndpoint +
                '}';
    }
}