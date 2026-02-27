package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Pagination strategy for a remote {@link io.github.cyfko.inputspec.model.ValuesEndpoint} (§2.2).
 *
 * <p>Determines how the client should request additional pages of values
 * from a paginated endpoint. Combined with
 * {@link io.github.cyfko.inputspec.model.RequestParams} to specify the
 * pagination query parameter names and defaults.</p>
 *
 * @see io.github.cyfko.inputspec.model.ValuesEndpoint
 * @see io.github.cyfko.inputspec.model.RequestParams
 */
public enum PaginationStrategy {

    /** No pagination — the endpoint returns all values in a single response. */
    NONE,

    /**
     * Classic page-number pagination — the client sends a page index
     * (e.g. {@code ?page=2&limit=50}) and receives a fixed-size page.
     */
    PAGE_NUMBER;

    /**
     * Deserializes a JSON string to the corresponding {@code PaginationStrategy}.
     *
     * <p>Case-insensitive. Returns {@link #NONE} as the default
     * for {@code null} or unrecognized values.</p>
     *
     * @param v the JSON string value (may be {@code null})
     * @return the matching enum constant, or {@link #NONE} as default
     */
    @JsonCreator
    public static PaginationStrategy fromJson(String v) {
        if (v == null) return NONE;
        return switch (v.toUpperCase()) {
            case "PAGE_NUMBER" -> PAGE_NUMBER;
            default            -> NONE;
        };
    }
}