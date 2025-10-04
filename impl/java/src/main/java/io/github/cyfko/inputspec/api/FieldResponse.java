package io.github.cyfko.inputspec.api;

import io.github.cyfko.inputspec.InputFieldSpec;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * API response for GET /api/fields/{fieldName} according to the protocol specification.
 * 
 * Contains a single InputFieldSpec.
 */
public class FieldResponse {
    
    @JsonProperty("field")
    private InputFieldSpec field;
    
    /**
     * Default constructor for JSON deserialization
     */
    public FieldResponse() {
    }
    
    /**
     * Constructor with field
     * 
     * @param field the input field specification
     */
    public FieldResponse(InputFieldSpec field) {
        this.field = field;
    }
    
    /**
     * Gets the input field specification
     * 
     * @return the field
     */
    public InputFieldSpec getField() {
        return field;
    }
    
    /**
     * Sets the input field specification
     * 
     * @param field the field
     */
    public void setField(InputFieldSpec field) {
        this.field = field;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FieldResponse that = (FieldResponse) obj;
        return Objects.equals(field, that.field);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(field);
    }
    
    @Override
    public String toString() {
        return "FieldResponse{" +
                "field=" + field +
                '}';
    }
}