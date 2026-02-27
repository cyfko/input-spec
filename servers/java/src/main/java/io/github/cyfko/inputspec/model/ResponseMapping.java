package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Describes how to extract data from a remote {@link ValuesEndpoint} response.
 *
 * <p>Corresponds to the {@code responseMapping} section of the DIFSP protocol's
 * ValuesEndpoint entity (§2.2). When the values endpoint is a remote service
 * (HTTPS / HTTP / GRPC), the response body typically wraps the actual items
 * inside a data envelope. This record tells the client which JSON fields
 * contain the items array, total count, and hasNext pagination flag.</p>
 *
 * <p><b>Example JSON:</b></p>
 * <pre>
 * {
 *   "dataField": "data",
 *   "totalField": "totalCount",
 *   "hasNextField": "hasMore"
 * }
 * </pre>
 *
 * <p>Given a server response:</p>
 * <pre>
 * {
 *   "data": [ { "value": "FR", "label": "France" }, ... ],
 *   "totalCount": 195,
 *   "hasMore": true
 * }
 * </pre>
 *
 * @param dataField    JSON field name containing the items array (e.g. {@code "data"})
 * @param totalField   JSON field name containing the total count across all pages (e.g. {@code "totalCount"})
 * @param hasNextField JSON field name containing the boolean hasNext flag (e.g. {@code "hasMore"})
 *
 * @see ValuesEndpoint
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseMapping(
    String dataField,
    String totalField,
    String hasNextField
) {}