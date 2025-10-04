package io.github.cyfko.inputspec;

/**
 * Communication protocol hints for client implementations in the Dynamic Input Field Specification Protocol v1.0.
 * 
 * These values serve as indicators to clients about which protocol an endpoint expects,
 * but do not enforce transport security or handle authentication.
 * 
 * As specified in the protocol:
 * - HTTPS: Secure HTTP protocol hint (recommended default)
 * - HTTP: Standard HTTP protocol hint  
 * - GRPC: Google Remote Procedure Call protocol hint
 */
public enum Protocol {
    /**
     * Secure HTTP protocol hint - recommended default for production environments
     */
    HTTPS,
    
    /**
     * Standard HTTP protocol hint - typically for development or secure internal networks
     */
    HTTP,
    
    /**
     * Google Remote Procedure Call protocol hint - for high-performance binary communication
     */
    GRPC
}