package io.github.cyfko.inputspec;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Configuration for fetching values dynamically from a remote source with search capabilities
 * as defined in the protocol specification.
 * 
 * The protocol field serves as a hint to client implementations about which communication
 * protocol the endpoint expects, but does not enforce transport security.
 */
public class ValuesEndpoint {
    
    @JsonProperty("protocol")
    private Protocol protocol = Protocol.HTTPS; // Default: HTTPS (recommended)
    
    @JsonProperty("uri")
    private String uri;
    
    @JsonProperty("method")
    private HttpMethod method = HttpMethod.GET; // Default: GET
    
    @JsonProperty("searchField")
    private String searchField;
    
    @JsonProperty("paginationStrategy")
    private PaginationStrategy paginationStrategy;
    
    @JsonProperty("responseMapping")
    private ResponseMapping responseMapping;
    
    @JsonProperty("requestParams")
    private RequestParams requestParams;
    
    @JsonProperty("cacheStrategy")
    private CacheStrategy cacheStrategy;
    
    @JsonProperty("debounceMs")
    private int debounceMs = 300; // Default: 300ms
    
    @JsonProperty("minSearchLength")
    private int minSearchLength = 0; // Default: 0
    
    /**
     * Default constructor for JSON deserialization
     */
    public ValuesEndpoint() {
    }
    
    /**
     * Constructor with required fields
     * 
     * @param uri the endpoint path or full URL
     * @param responseMapping configuration for parsing the response
     */
    public ValuesEndpoint(String uri, ResponseMapping responseMapping) {
        this.uri = uri;
        this.responseMapping = responseMapping;
    }
    
    /**
     * Gets the protocol hint for client communication.
     * Supported: HTTPS, HTTP, GRPC
     * 
     * @return the protocol hint (default: HTTPS)
     */
    public Protocol getProtocol() {
        return protocol;
    }
    
    /**
     * Sets the protocol hint for client communication
     * 
     * @param protocol the protocol hint (HTTPS, HTTP, GRPC)
     */
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }
    
    /**
     * Gets the endpoint path or full URL.
     * Required field according to the specification.
     * 
     * @return the URI
     */
    public String getUri() {
        return uri;
    }
    
    /**
     * Sets the endpoint path or full URL
     * 
     * @param uri the URI
     */
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    /**
     * Gets the HTTP method.
     * Supported: GET, POST
     * 
     * @return the method (default: GET)
     */
    public HttpMethod getMethod() {
        return method;
    }
    
    /**
     * Sets the HTTP method
     * 
     * @param method the method (GET, POST)
     */
    public void setMethod(HttpMethod method) {
        this.method = method;
    }
    
    /**
     * Gets the server-side field to search/filter on
     * 
     * @return the search field
     */
    public String getSearchField() {
        return searchField;
    }
    
    /**
     * Sets the server-side field to search/filter on
     * 
     * @param searchField the search field
     */
    public void setSearchField(String searchField) {
        this.searchField = searchField;
    }
    
    /**
     * Gets the pagination strategy
     * 
     * @return the pagination strategy
     */
    public PaginationStrategy getPaginationStrategy() {
        return paginationStrategy;
    }
    
    /**
     * Sets the pagination strategy
     * 
     * @param paginationStrategy the pagination strategy
     */
    public void setPaginationStrategy(PaginationStrategy paginationStrategy) {
        this.paginationStrategy = paginationStrategy;
    }
    
    /**
     * Gets the response mapping configuration.
     * Required field according to the specification.
     * 
     * @return the response mapping
     */
    public ResponseMapping getResponseMapping() {
        return responseMapping;
    }
    
    /**
     * Sets the response mapping configuration
     * 
     * @param responseMapping the response mapping
     */
    public void setResponseMapping(ResponseMapping responseMapping) {
        this.responseMapping = responseMapping;
    }
    
    /**
     * Gets the request parameters configuration
     * 
     * @return the request params
     */
    public RequestParams getRequestParams() {
        return requestParams;
    }
    
    /**
     * Sets the request parameters configuration
     * 
     * @param requestParams the request params
     */
    public void setRequestParams(RequestParams requestParams) {
        this.requestParams = requestParams;
    }
    
    /**
     * Gets the cache strategy
     * 
     * @return the cache strategy
     */
    public CacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }
    
    /**
     * Sets the cache strategy
     * 
     * @param cacheStrategy the cache strategy
     */
    public void setCacheStrategy(CacheStrategy cacheStrategy) {
        this.cacheStrategy = cacheStrategy;
    }
    
    /**
     * Gets the milliseconds to wait before sending search request
     * 
     * @return the debounce time in milliseconds (default: 300)
     */
    public int getDebounceMs() {
        return debounceMs;
    }
    
    /**
     * Sets the debounce time in milliseconds
     * 
     * @param debounceMs the debounce time
     */
    public void setDebounceMs(int debounceMs) {
        this.debounceMs = debounceMs;
    }
    
    /**
     * Gets the minimum characters required before triggering search
     * 
     * @return the minimum search length (default: 0)
     */
    public int getMinSearchLength() {
        return minSearchLength;
    }
    
    /**
     * Sets the minimum characters required before triggering search
     * 
     * @param minSearchLength the minimum search length
     */
    public void setMinSearchLength(int minSearchLength) {
        this.minSearchLength = minSearchLength;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ValuesEndpoint that = (ValuesEndpoint) obj;
        return debounceMs == that.debounceMs &&
               minSearchLength == that.minSearchLength &&
               Objects.equals(protocol, that.protocol) &&
               Objects.equals(uri, that.uri) &&
               Objects.equals(method, that.method) &&
               Objects.equals(searchField, that.searchField) &&
               paginationStrategy == that.paginationStrategy &&
               Objects.equals(responseMapping, that.responseMapping) &&
               Objects.equals(requestParams, that.requestParams) &&
               cacheStrategy == that.cacheStrategy;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(protocol, uri, method, searchField, paginationStrategy,
                responseMapping, requestParams, cacheStrategy, debounceMs, minSearchLength);
    }
    
    @Override
    public String toString() {
        return "ValuesEndpoint{" +
                "protocol='" + protocol + '\'' +
                ", uri='" + uri + '\'' +
                ", method='" + method + '\'' +
                ", searchField='" + searchField + '\'' +
                ", paginationStrategy=" + paginationStrategy +
                ", responseMapping=" + responseMapping +
                ", requestParams=" + requestParams +
                ", cacheStrategy=" + cacheStrategy +
                ", debounceMs=" + debounceMs +
                ", minSearchLength=" + minSearchLength +
                '}';
    }
    
    /**
     * Creates a builder for ValuesEndpoint with required fields
     * 
     * @param uri the endpoint path or full URL (required)
     * @param responseMapping configuration for parsing the response (required)
     * @return a new builder instance
     */
    public static Builder builder(String uri, ResponseMapping responseMapping) {
        return new Builder(uri, responseMapping);
    }
    
    /**
     * Builder class for ValuesEndpoint providing a fluent API
     */
    public static class Builder {
        private final String uri;
        private final ResponseMapping responseMapping;
        private Protocol protocol = Protocol.HTTPS;
        private HttpMethod method = HttpMethod.GET;
        private String searchField;
        private PaginationStrategy paginationStrategy;
        private RequestParams requestParams;
        private CacheStrategy cacheStrategy;
        private int debounceMs = 300;
        private int minSearchLength = 0;
        
        /**
         * Constructor with required fields
         * 
         * @param uri the endpoint path or full URL (required)
         * @param responseMapping configuration for parsing the response (required)
         */
        private Builder(String uri, ResponseMapping responseMapping) {
            if (uri == null || uri.trim().isEmpty()) {
                throw new IllegalArgumentException("URI cannot be null or empty");
            }
            if (responseMapping == null) {
                throw new IllegalArgumentException("ResponseMapping cannot be null");
            }
            this.uri = uri;
            this.responseMapping = responseMapping;
        }
        
        /**
         * Sets the protocol hint for client communication
         * 
         * @param protocol the protocol hint (HTTPS, HTTP, GRPC)
         * @return this builder
         */
        public Builder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }
        
        /**
         * Sets the HTTP method
         * 
         * @param method the method (GET, POST)
         * @return this builder
         */
        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }
        
        /**
         * Sets the server-side field to search/filter on
         * 
         * @param searchField the search field
         * @return this builder
         */
        public Builder searchField(String searchField) {
            this.searchField = searchField;
            return this;
        }
        
        /**
         * Sets the pagination strategy
         * 
         * @param paginationStrategy the pagination strategy
         * @return this builder
         */
        public Builder paginationStrategy(PaginationStrategy paginationStrategy) {
            this.paginationStrategy = paginationStrategy;
            return this;
        }
        
        /**
         * Sets the request parameters configuration
         * 
         * @param requestParams the request params
         * @return this builder
         */
        public Builder requestParams(RequestParams requestParams) {
            this.requestParams = requestParams;
            return this;
        }
        
        /**
         * Sets the cache strategy
         * 
         * @param cacheStrategy the cache strategy
         * @return this builder
         */
        public Builder cacheStrategy(CacheStrategy cacheStrategy) {
            this.cacheStrategy = cacheStrategy;
            return this;
        }
        
        /**
         * Sets the debounce time in milliseconds
         * 
         * @param debounceMs the debounce time
         * @return this builder
         */
        public Builder debounceMs(int debounceMs) {
            this.debounceMs = debounceMs;
            return this;
        }
        
        /**
         * Sets the minimum characters required before triggering search
         * 
         * @param minSearchLength the minimum search length
         * @return this builder
         */
        public Builder minSearchLength(int minSearchLength) {
            this.minSearchLength = minSearchLength;
            return this;
        }
        
        /**
         * Builds the ValuesEndpoint instance
         * 
         * @return a new ValuesEndpoint instance
         */
        public ValuesEndpoint build() {
            ValuesEndpoint endpoint = new ValuesEndpoint();
            endpoint.uri = this.uri;
            endpoint.responseMapping = this.responseMapping;
            endpoint.protocol = this.protocol;
            endpoint.method = this.method;
            endpoint.searchField = this.searchField;
            endpoint.paginationStrategy = this.paginationStrategy;
            endpoint.requestParams = this.requestParams;
            endpoint.cacheStrategy = this.cacheStrategy;
            endpoint.debounceMs = this.debounceMs;
            endpoint.minSearchLength = this.minSearchLength;
            return endpoint;
        }
    }
}