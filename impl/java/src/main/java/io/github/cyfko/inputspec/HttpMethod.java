package io.github.cyfko.inputspec;

/**
 * HTTP methods supported by the Dynamic Input Field Specification Protocol v1.0.
 * 
 * As specified in the protocol:
 * - GET: Standard HTTP GET method (default for read operations)
 * - POST: Standard HTTP POST method (for complex queries or data submission)
 */
public enum HttpMethod {
    /**
     * HTTP GET method - standard for read operations and simple queries
     */
    GET,
    
    /**
     * HTTP POST method - for complex queries, search with body, or data submission
     */
    POST
}