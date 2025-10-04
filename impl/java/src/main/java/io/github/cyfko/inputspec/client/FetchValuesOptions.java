package io.github.cyfko.inputspec.client;

/**
 * Options for fetching values from endpoints
 */
public class FetchValuesOptions {
    
    private Integer page;
    private String search;
    private Integer limit;
    
    /**
     * Default constructor
     */
    public FetchValuesOptions() {
    }
    
    /**
     * Constructor with all options
     * 
     * @param page page number (for pagination)
     * @param search search query
     * @param limit page size limit
     */
    public FetchValuesOptions(Integer page, String search, Integer limit) {
        this.page = page;
        this.search = search;
        this.limit = limit;
    }
    
    /**
     * Gets the page number
     * 
     * @return page number
     */
    public Integer getPage() {
        return page;
    }
    
    /**
     * Sets the page number
     * 
     * @param page page number
     * @return this options object for chaining
     */
    public FetchValuesOptions setPage(Integer page) {
        this.page = page;
        return this;
    }
    
    /**
     * Gets the search query
     * 
     * @return search query
     */
    public String getSearch() {
        return search;
    }
    
    /**
     * Sets the search query
     * 
     * @param search search query
     * @return this options object for chaining
     */
    public FetchValuesOptions setSearch(String search) {
        this.search = search;
        return this;
    }
    
    /**
     * Gets the page size limit
     * 
     * @return page size limit
     */
    public Integer getLimit() {
        return limit;
    }
    
    /**
     * Sets the page size limit
     * 
     * @param limit page size limit
     * @return this options object for chaining
     */
    public FetchValuesOptions setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }
}