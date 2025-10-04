package io.github.cyfko.inputspec;

/**
 * Data types supported by the Dynamic Input Field Specification Protocol v1.0.
 * 
 * As specified in the protocol:
 * - STRING: Text value
 * - NUMBER: Numeric value  
 * - DATE: ISO 8601 date/datetime
 * - BOOLEAN: True/false
 */
public enum DataType {
    /**
     * Text value - supports pattern, min/max character count when single value,
     * min/max array length when multiple values
     */
    STRING,
    
    /**
     * Numeric value - supports min/max numeric value when single,
     * min/max array length when multiple values
     */
    NUMBER,
    
    /**
     * ISO 8601 date/datetime - supports min/max date value when single,
     * min/max array length when multiple values
     */
    DATE,
    
    /**
     * True/false value - supports min/max array length when multiple values
     */
    BOOLEAN
}