package io.github.cyfko.inputspec.client;

import java.util.Map;

/**
 * HTTP client abstraction for making requests to values endpoints.
 * This interface allows for different HTTP client implementations.
 */
public interface HttpClient {
    
    /**
     * Makes an HTTP request to the specified URL with parameters
     * 
     * @param url the URL to request
     * @param method the HTTP method (GET, POST, etc.)
     * @param params query parameters or request body parameters
     * @param headers HTTP headers (optional)
     * @return the response body as string
     * @throws HttpClientException if the request fails
     */
    String request(String url, String method, Map<String, Object> params, Map<String, String> headers) 
            throws HttpClientException;
    
    /**
     * Makes an HTTP GET request with query parameters
     * 
     * @param url the URL to request
     * @param params query parameters
     * @return the response body as string
     * @throws HttpClientException if the request fails
     */
    default String get(String url, Map<String, Object> params) throws HttpClientException {
        return request(url, "GET", params, null);
    }
    
    /**
     * Makes an HTTP POST request with body parameters
     * 
     * @param url the URL to request
     * @param params body parameters
     * @return the response body as string
     * @throws HttpClientException if the request fails
     */
    default String post(String url, Map<String, Object> params) throws HttpClientException {
        return request(url, "POST", params, null);
    }
}