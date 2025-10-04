package io.github.cyfko.inputspec.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.cyfko.inputspec.ValueAlias;
import java.util.List;
import java.util.Objects;

/**
 * API response for values endpoints according to the protocol specification.
 * 
 * Used for responses from ValuesEndpoint URIs with pagination support.
 * The response structure depends on the ResponseMapping configuration.
 */
public class ValuesResponse {
    
    @JsonProperty("data")
    private List<ValueAlias> data;
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("pageSize")
    private Integer pageSize;
    
    @JsonProperty("total")
    private Integer total;
    
    @JsonProperty("hasNext")
    private Boolean hasNext;
    
    /**
     * Default constructor for JSON deserialization
     */
    public ValuesResponse() {
    }
    
    /**
     * Constructor with data only (for responses without pagination)
     * 
     * @param data list of value aliases
     */
    public ValuesResponse(List<ValueAlias> data) {
        this.data = data;
    }
    
    /**
     * Constructor with all pagination fields
     * 
     * @param data list of value aliases
     * @param page current page number
     * @param pageSize number of items in this page
     * @param total total count across all pages
     * @param hasNext whether there's a next page
     */
    public ValuesResponse(List<ValueAlias> data, Integer page, Integer pageSize, 
                         Integer total, Boolean hasNext) {
        this.data = data;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.hasNext = hasNext;
    }
    
    /**
     * Gets the list of value aliases.
     * This is the main data field containing the values.
     * 
     * @return the data list
     */
    public List<ValueAlias> getData() {
        return data;
    }
    
    /**
     * Sets the list of value aliases
     * 
     * @param data the data list
     */
    public void setData(List<ValueAlias> data) {
        this.data = data;
    }
    
    /**
     * Gets the current page number
     * 
     * @return the page number
     */
    public Integer getPage() {
        return page;
    }
    
    /**
     * Sets the current page number
     * 
     * @param page the page number
     */
    public void setPage(Integer page) {
        this.page = page;
    }
    
    /**
     * Gets the number of items in this page
     * 
     * @return the page size
     */
    public Integer getPageSize() {
        return pageSize;
    }
    
    /**
     * Sets the number of items in this page
     * 
     * @param pageSize the page size
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
    /**
     * Gets the total count across all pages
     * 
     * @return the total count
     */
    public Integer getTotal() {
        return total;
    }
    
    /**
     * Sets the total count across all pages
     * 
     * @param total the total count
     */
    public void setTotal(Integer total) {
        this.total = total;
    }
    
    /**
     * Gets whether there's a next page
     * 
     * @return true if there's a next page
     */
    public Boolean getHasNext() {
        return hasNext;
    }
    
    /**
     * Sets whether there's a next page
     * 
     * @param hasNext true if there's a next page
     */
    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ValuesResponse that = (ValuesResponse) obj;
        return Objects.equals(data, that.data) &&
               Objects.equals(page, that.page) &&
               Objects.equals(pageSize, that.pageSize) &&
               Objects.equals(total, that.total) &&
               Objects.equals(hasNext, that.hasNext);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(data, page, pageSize, total, hasNext);
    }
    
    @Override
    public String toString() {
        return "ValuesResponse{" +
                "data=" + data +
                ", page=" + page +
                ", pageSize=" + pageSize +
                ", total=" + total +
                ", hasNext=" + hasNext +
                '}';
    }
}