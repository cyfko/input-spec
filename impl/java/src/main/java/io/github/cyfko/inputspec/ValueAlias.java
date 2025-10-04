package io.github.cyfko.inputspec;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Represents a single value option as defined in the protocol specification.
 * 
 * From the specification:
 * - value: Actual value to send back to server (used as-is)
 * - label: Display text (shown to user without transformation)
 * 
 * Important: 
 * - value is returned to server exactly as received (no transformation)
 * - label is displayed to user without any transformation
 */
public class ValueAlias {
    
    @JsonProperty("value")
    private Object value;
    
    @JsonProperty("label")
    private String label;
    
    /**
     * Default constructor for JSON deserialization
     */
    public ValueAlias() {
    }
    
    /**
     * Constructor with value and label
     * 
     * @param value Actual value to send back to server
     * @param label Display text shown to user
     */
    public ValueAlias(Object value, String label) {
        this.value = value;
        this.label = label;
    }
    
    /**
     * Gets the actual value to send back to server.
     * This value is used exactly as received with no transformation.
     * 
     * @return the value
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * Sets the actual value to send back to server
     * 
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }
    
    /**
     * Gets the display text shown to user.
     * This text is displayed without any transformation.
     * 
     * @return the label
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Sets the display text
     * 
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ValueAlias that = (ValueAlias) obj;
        return Objects.equals(value, that.value) &&
               Objects.equals(label, that.label);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value, label);
    }
    
    @Override
    public String toString() {
        return "ValueAlias{" +
                "value=" + value +
                ", label='" + label + '\'' +
                '}';
    }
}