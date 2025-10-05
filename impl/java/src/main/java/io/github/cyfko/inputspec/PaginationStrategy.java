package io.github.cyfko.inputspec;

/**
 * Pagination strategies supported by the protocol.
 * 
 * From the specification:
 * - NONE: No pagination, returns all values (small, static datasets &lt; 100 items)
 * - PAGE_NUMBER: Page-based pagination (page 1, 2, 3...)
 */
public enum PaginationStrategy {
    /**
     * No pagination - returns all values.
     * Use for small, static datasets (&lt; 100 items)
     */
    NONE,
    
    /**
     * Page-based pagination using page numbers.
     * Traditional pagination approach
     */
    PAGE_NUMBER
}