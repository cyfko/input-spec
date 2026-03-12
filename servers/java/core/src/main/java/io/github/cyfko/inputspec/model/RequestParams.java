package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration for pagination request parameters on a remote {@link ValuesEndpoint}.
 *
 * <p>Corresponds to the {@code requestParams} section of the DIFSP protocol's
 * ValuesEndpoint entity (§2.2). Tells the client which query parameter names
 * to use for pagination and what the default page size should be.</p>
 *
 * <p><b>Example JSON:</b></p>
 * <pre>
 * {
 *   "pageParam": "page",
 *   "limitParam": "limit",
 *   "defaultLimit": 50
 * }
 * </pre>
 *
 * <p>This would cause the client to issue requests like:
 * {@code GET /api/users?page=2&limit=50}</p>
 *
 * @param pageParam    the query parameter name for the page number (default: {@code "page"})
 * @param limitParam   the query parameter name for the page size (default: {@code "limit"})
 * @param defaultLimit the default number of items per page (default: {@code 50})
 *
 * @see ValuesEndpoint
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestParams(
    String  pageParam,
    String  limitParam,
    Integer defaultLimit
) {}