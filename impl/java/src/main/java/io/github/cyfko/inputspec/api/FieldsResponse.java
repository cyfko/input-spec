package io.github.cyfko.inputspec.api;

import io.github.cyfko.inputspec.InputFieldSpec;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * API response for GET /api/fields according to the protocol specification.
 * 
 * Contains a list of InputFieldSpec and version information.
 */
public class FieldsResponse {
    
    @JsonProperty("fields")
    private List<InputFieldSpec> fields;
    
    @JsonProperty("version")
    private String version;
    
    /**
     * Default constructor for JSON deserialization
     */
    public FieldsResponse() {
    }
    
    /**
     * Constructor with fields and version
     * 
     * @param fields list of input field specifications
     * @param version protocol version
     */
    public FieldsResponse(List<InputFieldSpec> fields, String version) {
        this.fields = fields;
        this.version = version;
    }
    
    /**
     * Gets the list of input field specifications
     * 
     * @return list of fields
     */
    public List<InputFieldSpec> getFields() {
        return fields;
    }
    
    /**
     * Sets the list of input field specifications
     * 
     * @param fields list of fields
     */
    public void setFields(List<InputFieldSpec> fields) {
        this.fields = fields;
    }
    
    /**
     * Gets the protocol version
     * 
     * @return version string
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Sets the protocol version
     * 
     * @param version version string
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FieldsResponse that = (FieldsResponse) obj;
        return Objects.equals(fields, that.fields) &&
               Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fields, version);
    }
    
    @Override
    public String toString() {
        return "FieldsResponse{" +
                "fields=" + fields +
                ", version='" + version + '\'' +
                '}';
    }
}