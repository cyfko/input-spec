package io.github.cyfko.inputspec;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Describes where to find information in the endpoint response
 * as defined in the protocol specification.
 * 
 * If dataField is absent, the root response is assumed to be the array of ValueAlias.
 */
public class ResponseMapping {
    
    @JsonProperty("dataField")
    private String dataField;
    
    @JsonProperty("pageField")
    private String pageField;
    
    @JsonProperty("pageSizeField")
    private String pageSizeField;
    
    @JsonProperty("totalField")
    private String totalField;
    
    @JsonProperty("hasNextField")
    private String hasNextField;
    
    /**
     * Default constructor for JSON deserialization
     */
    public ResponseMapping() {
    }
    
    /**
     * Constructor with required dataField
     * 
     * @param dataField field containing the array of ValueAlias
     */
    public ResponseMapping(String dataField) {
        this.dataField = dataField;
    }
    
    /**
     * Gets the field containing the array of ValueAlias.
     * Required field according to the specification.
     * 
     * @return the data field name
     */
    public String getDataField() {
        return dataField;
    }
    
    /**
     * Sets the field containing the array of ValueAlias
     * 
     * @param dataField the data field name
     */
    public void setDataField(String dataField) {
        this.dataField = dataField;
    }
    
    /**
     * Gets the field containing current page number
     * 
     * @return the page field name
     */
    public String getPageField() {
        return pageField;
    }
    
    /**
     * Sets the field containing current page number
     * 
     * @param pageField the page field name
     */
    public void setPageField(String pageField) {
        this.pageField = pageField;
    }
    
    /**
     * Gets the field containing number of items in this page
     * 
     * @return the page size field name
     */
    public String getPageSizeField() {
        return pageSizeField;
    }
    
    /**
     * Sets the field containing number of items in this page
     * 
     * @param pageSizeField the page size field name
     */
    public void setPageSizeField(String pageSizeField) {
        this.pageSizeField = pageSizeField;
    }
    
    /**
     * Gets the field containing total count across all pages
     * 
     * @return the total field name
     */
    public String getTotalField() {
        return totalField;
    }
    
    /**
     * Sets the field containing total count across all pages
     * 
     * @param totalField the total field name
     */
    public void setTotalField(String totalField) {
        this.totalField = totalField;
    }
    
    /**
     * Gets the field indicating if there's a next page (boolean)
     * 
     * @return the has next field name
     */
    public String getHasNextField() {
        return hasNextField;
    }
    
    /**
     * Sets the field indicating if there's a next page
     * 
     * @param hasNextField the has next field name
     */
    public void setHasNextField(String hasNextField) {
        this.hasNextField = hasNextField;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ResponseMapping that = (ResponseMapping) obj;
        return Objects.equals(dataField, that.dataField) &&
               Objects.equals(pageField, that.pageField) &&
               Objects.equals(pageSizeField, that.pageSizeField) &&
               Objects.equals(totalField, that.totalField) &&
               Objects.equals(hasNextField, that.hasNextField);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(dataField, pageField, pageSizeField, totalField, hasNextField);
    }
    
    @Override
    public String toString() {
        return "ResponseMapping{" +
                "dataField='" + dataField + '\'' +
                ", pageField='" + pageField + '\'' +
                ", pageSizeField='" + pageSizeField + '\'' +
                ", totalField='" + totalField + '\'' +
                ", hasNextField='" + hasNextField + '\'' +
                '}';
    }
}