package io.github.cyfko.inputspec;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Represents a smart input field with constraints and value sources
 * as defined in the Dynamic Input Field Specification Protocol v1.0.
 * 
 * Key points from the specification:
 * - dataType describes the singleton element type only
 * - If expectMultipleValues is true, the field works with arrays of this type
 * - Constraints are executed in order, allowing for logical sequencing of validation rules
 * - The required field has been moved to the top-level for better API ergonomics
 */
public class InputFieldSpec {
    
    @JsonProperty("displayName")
    private String displayName;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("dataType")
    private DataType dataType;
    
    @JsonProperty("expectMultipleValues")
    private boolean expectMultipleValues;
    
    @JsonProperty("required")
    private boolean required;
    
    @JsonProperty("constraints")
    private List<ConstraintDescriptor> constraints;
    
    /**
     * Default constructor for JSON deserialization
     */
    public InputFieldSpec() {
    }
    
    /**
     * Constructor with required fields
     * 
     * @param displayName human-readable field label
     * @param dataType data type of the field
     * @param expectMultipleValues whether field accepts array of values
     * @param required whether this field is required
     * @param constraints array of constraints with ordered execution
     */
    public InputFieldSpec(String displayName, DataType dataType, boolean expectMultipleValues, 
                         boolean required, List<ConstraintDescriptor> constraints) {
        this.displayName = displayName;
        this.dataType = dataType;
        this.expectMultipleValues = expectMultipleValues;
        this.required = required;
        this.constraints = constraints;
    }
    
    /**
     * Gets the human-readable field label.
     * Required field according to the specification.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Sets the human-readable field label
     * 
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gets the detailed explanation of field purpose
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the detailed explanation of field purpose
     * 
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the data type of the field.
     * Describes the singleton element type only.
     * Required field according to the specification.
     * 
     * @return the data type (STRING, NUMBER, DATE, BOOLEAN)
     */
    public DataType getDataType() {
        return dataType;
    }
    
    /**
     * Sets the data type of the field
     * 
     * @param dataType the data type
     */
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }
    
    /**
     * Gets whether field accepts array of values.
     * If true, the field works with arrays of the specified dataType.
     * Required field according to the specification.
     * 
     * @return true if field accepts multiple values
     */
    public boolean isExpectMultipleValues() {
        return expectMultipleValues;
    }
    
    /**
     * Sets whether field accepts array of values
     * 
     * @param expectMultipleValues true if field accepts multiple values
     */
    public void setExpectMultipleValues(boolean expectMultipleValues) {
        this.expectMultipleValues = expectMultipleValues;
    }
    
    /**
     * Gets whether this field is required.
     * Moved from constraints for better API design.
     * Required field according to the specification.
     * 
     * @return true if field is required
     */
    public boolean isRequired() {
        return required;
    }
    
    /**
     * Sets whether this field is required
     * 
     * @param required true if field is required
     */
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    /**
     * Gets the array of constraints with ordered execution.
     * Constraints are executed in order, enabling deterministic validation sequencing.
     * Required field according to the specification.
     * 
     * @return the constraints list
     */
    public List<ConstraintDescriptor> getConstraints() {
        return constraints;
    }
    
    /**
     * Sets the array of constraints
     * 
     * @param constraints the constraints list
     */
    public void setConstraints(List<ConstraintDescriptor> constraints) {
        this.constraints = constraints;
    }
    
    /**
     * Creates a new Builder for InputFieldSpec with required fields.
     * 
     * @param displayName human-readable field label (required)
     * @param dataType data type of the field (required)
     * @return a new Builder instance
     */
    public static Builder builder(String displayName, DataType dataType) {
        return new Builder(displayName, dataType);
    }
    
    /**
     * Builder pattern for fluent InputFieldSpec creation.
     * 
     * Example usage:
     * <pre>
     * InputFieldSpec field = InputFieldSpec.builder("Username", DataType.STRING)
     *     .description("Enter your username")
     *     .required(true)
     *     .expectMultipleValues(false)
     *     .constraints(Arrays.asList(constraint))
     *     .build();
     * </pre>
     */
    public static class Builder {
        private final InputFieldSpec field;
        
        /**
         * Creates a new Builder with required fields.
         * 
         * @param displayName human-readable field label (required)
         * @param dataType data type of the field (required)
         */
        public Builder(String displayName, DataType dataType) {
            if (displayName == null || displayName.trim().isEmpty()) {
                throw new IllegalArgumentException("Display name cannot be null or empty");
            }
            if (dataType == null) {
                throw new IllegalArgumentException("Data type cannot be null");
            }
            
            this.field = new InputFieldSpec();
            this.field.setDisplayName(displayName);
            this.field.setDataType(dataType);
            // Set sensible defaults
            this.field.setExpectMultipleValues(false);
            this.field.setRequired(false);
        }
        
        /**
         * Sets the detailed explanation of field purpose.
         * 
         * @param description the description
         * @return this builder for chaining
         */
        public Builder description(String description) {
            field.setDescription(description);
            return this;
        }
        
        /**
         * Sets whether field accepts array of values.
         * 
         * @param expectMultipleValues true if field accepts multiple values
         * @return this builder for chaining
         */
        public Builder expectMultipleValues(boolean expectMultipleValues) {
            field.setExpectMultipleValues(expectMultipleValues);
            return this;
        }
        
        /**
         * Sets whether this field is required.
         * 
         * @param required true if field is required
         * @return this builder for chaining
         */
        public Builder required(boolean required) {
            field.setRequired(required);
            return this;
        }
        
        /**
         * Sets the array of constraints with ordered execution.
         * 
         * @param constraints the constraints list
         * @return this builder for chaining
         */
        public Builder constraints(List<ConstraintDescriptor> constraints) {
            field.setConstraints(constraints);
            return this;
        }
        
        /**
         * Builds and returns the configured InputFieldSpec.
         * 
         * @return the constructed InputFieldSpec
         */
        public InputFieldSpec build() {
            // Additional validation could be added here if needed
            return field;
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
        InputFieldSpec that = (InputFieldSpec) obj;
        return expectMultipleValues == that.expectMultipleValues &&
               required == that.required &&
               Objects.equals(displayName, that.displayName) &&
               Objects.equals(description, that.description) &&
               dataType == that.dataType &&
               Objects.equals(constraints, that.constraints);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(displayName, description, dataType, expectMultipleValues, required, constraints);
    }
    
    @Override
    public String toString() {
        return "InputFieldSpec{" +
                "displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", dataType=" + dataType +
                ", expectMultipleValues=" + expectMultipleValues +
                ", required=" + required +
                ", constraints=" + constraints +
                '}';
    }
}