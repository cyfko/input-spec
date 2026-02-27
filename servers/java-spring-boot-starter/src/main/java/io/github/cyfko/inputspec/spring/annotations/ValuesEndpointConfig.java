package io.github.cyfko.inputspec.spring.annotations;

import java.lang.annotation.*;

/**
 * Configuration for a remote values endpoint within {@link InputField}.
 * <p>
 * Describes how to fetch allowed/suggested values for a field from a remote API,
 * following the input-spec protocol v2.1 {@link io.github.cyfko.inputspec.model.ValuesEndpoint} specification.
 * </p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Simple GET Endpoint</h3>
 * <pre>{@code
 * @InputField(
 *     displayName = "Category",
 *     valuesEndpoint = @ValuesEndpointConfig(
 *         uri = "/api/categories"
 *     )
 * )
 * private String category;
 * }</pre>
 *
 * <h3>Searchable Endpoint with Debounce</h3>
 * <pre>{@code
 * @InputField(
 *     displayName = "City",
 *     valuesEndpoint = @ValuesEndpointConfig(
 *         uri = "/api/cities/search",
 *         searchable = true,
 *         debounceMs = 300,
 *         minSearchLength = 2
 *     )
 * )
 * private String cityId;
 * }</pre>
 *
 * <h3>POST Endpoint with Pagination</h3>
 * <pre>{@code
 * @InputField(
 *     displayName = "Product",
 *     valuesEndpoint = @ValuesEndpointConfig(
 *         uri = "/api/products/filter",
 *         method = "POST",
 *         paginationStrategy = "OFFSET_LIMIT",
 *         cacheStrategy = "SESSION"
 *     )
 * )
 * private Long productId;
 * }</pre>
 *
 * @author cyfko
 * @since 2.1.0
 * @see InputField
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // Only used as nested annotation
@Documented
public @interface ValuesEndpointConfig {

    /**
     * Remote endpoint URI for fetching values.
     * <p>
     * Can be absolute (https://example.com/api/values) or relative (/api/values).
     * </p>
     *
     * @return endpoint URI
     */
    String uri() default "";

    /**
     * HTTP method for the endpoint.
     * <p>
     * Defaults to GET.
     * </p>
     *
     * @return HTTP method ("GET" or "POST")
     */
    String method() default "GET";

    /**
     * Whether this endpoint supports server-side search/filtering.
     * <p>
     * If true, enables search functionality in the UI (autocomplete, filter input, etc.).
     * </p>
     *
     * @return true if searchable
     */
    boolean searchable() default false;

    /**
     * Pagination strategy used by the endpoint.
     * <p>
     * Possible values:
     * <ul>
     *   <li>{@code NONE} - No pagination (all values returned)</li>
     *   <li>{@code OFFSET_LIMIT} - Uses offset/limit parameters</li>
     *   <li>{@code PAGE_SIZE} - Uses page/pageSize parameters</li>
     *   <li>{@code CURSOR} - Cursor-based pagination</li>
     * </ul>
     * </p>
     *
     * @return pagination strategy
     */
    String paginationStrategy() default "NONE";

    /**
     * Client-side caching strategy hint.
     * <p>
     * Possible values:
     * <ul>
     *   <li>{@code NONE} - No caching</li>
     *   <li>{@code SESSION} - Cache for current session</li>
     *   <li>{@code PERSISTENT} - Long-term cache</li>
     *   <li>{@code SMART} - Intelligent cache with TTL</li>
     * </ul>
     * </p>
     *
     * @return cache strategy
     */
    String cacheStrategy() default "NONE";

    /**
     * Debounce interval in milliseconds before triggering remote search.
     * <p>
     * Recommended for searchable endpoints to reduce API calls (typical: 200-500ms).
     * </p>
     *
     * @return debounce milliseconds (0 = no debounce)
     */
    int debounceMs() default 0;

    /**
     * Minimum number of characters before triggering a search.
     * <p>
     * Only applicable if {@link #searchable()} is true.
     * </p>
     *
     * @return minimum search length (0 = no minimum)
     */
    int minSearchLength() default 0;

    /**
     * Value domain strictness mode.
     * <p>
     * Possible values:
     * <ul>
     *   <li>{@code CLOSED} - Only values from endpoint are allowed (strict validation)</li>
     *   <li>{@code SUGGESTIONS} - Endpoint provides suggestions, but custom values allowed</li>
     * </ul>
     * </p>
     *
     * @return domain mode
     */
    String mode() default "CLOSED";
}
