package io.github.cyfko.inputspec.client;

import io.github.cyfko.inputspec.ValueAlias;
import java.util.List;

/**
 * Result of fetching values from endpoints
 */
public class FetchValuesResult {
    
    private final List<ValueAlias> values;
    private final boolean hasNext;
    private final Integer total;
    private final Integer page;
    
    /**
     * Constructor for fetch values result
     * 
     * @param values list of value aliases
     * @param hasNext whether there are more pages
     * @param total total number of items across all pages
     * @param page current page number
     */
    public FetchValuesResult(List<ValueAlias> values, boolean hasNext, Integer total, Integer page) {
        this.values = values;
        this.hasNext = hasNext;
        this.total = total;
        this.page = page;
    }
    
    /**
     * Gets the list of values
     * 
     * @return list of value aliases
     */
    public List<ValueAlias> getValues() {
        return values;
    }
    
    /**
     * Gets whether there are more pages available
     * 
     * @return true if there are more pages
     */
    public boolean isHasNext() {
        return hasNext;
    }
    
    /**
     * Gets the total number of items across all pages
     * 
     * @return total count (may be null if not provided)
     */
    public Integer getTotal() {
        return total;
    }
    
    /**
     * Gets the current page number
     * 
     * @return page number (may be null if not applicable)
     */
    public Integer getPage() {
        return page;
    }
}