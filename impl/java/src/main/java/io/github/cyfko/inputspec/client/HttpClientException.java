package io.github.cyfko.inputspec.client;

/**
 * Exception thrown when HTTP client operations fail
 */
public class HttpClientException extends Exception {
    
    private final int statusCode;
    
    /**
     * Constructor with message
     * 
     * @param message error message
     */
    public HttpClientException(String message) {
        super(message);
        this.statusCode = -1;
    }
    
    /**
     * Constructor with message and cause
     * 
     * @param message error message
     * @param cause the cause
     */
    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }
    
    /**
     * Constructor with message and HTTP status code
     * 
     * @param message error message
     * @param statusCode HTTP status code
     */
    public HttpClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    /**
     * Gets the HTTP status code if available
     * 
     * @return status code or -1 if not available
     */
    public int getStatusCode() {
        return statusCode;
    }
}