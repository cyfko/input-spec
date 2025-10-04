package io.github.cyfko.inputspec;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Configuration for request parameters as defined in the protocol specification.
 * 
 * Describes how to send pagination and search parameters in the request.
 */
public class RequestParams {
    
    @JsonProperty("pageParam")
    private String pageParam;
    
    @JsonProperty("limitParam")
    private String limitParam;
    
    @JsonProperty("searchParam")
    private String searchParam;
    
    @JsonProperty("defaultLimit")
    private Integer defaultLimit;
    
    /**
     * Default constructor for JSON deserialization
     */
    public RequestParams() {
    }
    
    /**
     * Gets the parameter name for page number.
     * Required for PAGE_NUMBER pagination strategy.
     * 
     * @return the page parameter name
     */
    public String getPageParam() {
        return pageParam;
    }
    
    /**
     * Sets the parameter name for page number
     * 
     * @param pageParam the page parameter name
     */
    public void setPageParam(String pageParam) {
        this.pageParam = pageParam;
    }
    
    /**
     * Gets the parameter name for page size
     * 
     * @return the limit parameter name
     */
    public String getLimitParam() {
        return limitParam;
    }
    
    /**
     * Sets the parameter name for page size
     * 
     * @param limitParam the limit parameter name
     */
    public void setLimitParam(String limitParam) {
        this.limitParam = limitParam;
    }
    
    /**
     * Gets the parameter name for search query.
     * Examples: "search", "q", "filter"
     * 
     * @return the search parameter name
     */
    public String getSearchParam() {
        return searchParam;
    }
    
    /**
     * Sets the parameter name for search query
     * 
     * @param searchParam the search parameter name
     */
    public void setSearchParam(String searchParam) {
        this.searchParam = searchParam;
    }
    
    /**
     * Gets the default page size if not specified
     * 
     * @return the default limit
     */
    public Integer getDefaultLimit() {
        return defaultLimit;
    }
    
    /**
     * Sets the default page size
     * 
     * @param defaultLimit the default limit
     */
    public void setDefaultLimit(Integer defaultLimit) {
        this.defaultLimit = defaultLimit;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RequestParams that = (RequestParams) obj;
        return Objects.equals(pageParam, that.pageParam) &&
               Objects.equals(limitParam, that.limitParam) &&
               Objects.equals(searchParam, that.searchParam) &&
               Objects.equals(defaultLimit, that.defaultLimit);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(pageParam, limitParam, searchParam, defaultLimit);
    }
    
    @Override
    public String toString() {
        return "RequestParams{" +
                "pageParam='" + pageParam + '\'' +
                ", limitParam='" + limitParam + '\'' +
                ", searchParam='" + searchParam + '\'' +
                ", defaultLimit=" + defaultLimit +
                '}';
    }
}