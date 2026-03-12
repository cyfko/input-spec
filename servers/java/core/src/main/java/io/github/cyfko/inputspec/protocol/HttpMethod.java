package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * HTTP method for {@link io.github.cyfko.inputspec.model.ValuesEndpoint}
 * and {@link io.github.cyfko.inputspec.model.SubmitEndpoint} (§2.2, §2.4).
 *
 * <p>Specifies which HTTP verb the client should use when fetching values
 * from a remote endpoint or submitting form data.</p>
 *
 * @see io.github.cyfko.inputspec.model.ValuesEndpoint
 * @see io.github.cyfko.inputspec.model.SubmitEndpoint
 */
public enum HttpMethod {

    /** Standard retrieval — used by default for ValuesEndpoint calls. */
    GET,

    /** Standard creation — used by default for SubmitEndpoint calls. */
    POST,

    /** Full resource replacement — alternative submission method for updates. */
    PUT;

    /**
     * Deserializes a JSON string to the corresponding {@code HttpMethod}.
     *
     * <p>Case-insensitive. Returns {@link #GET} as the default
     * for {@code null} or unrecognized values.</p>
     *
     * @param v the JSON string value (may be {@code null})
     * @return the matching enum constant, or {@link #GET} as default
     */
    @JsonCreator
    public static HttpMethod fromJson(String v) {
        if (v == null) return GET;
        return switch (v.toUpperCase()) {
            case "POST" -> POST;
            case "PUT"  -> PUT;
            default     -> GET;
        };
    }
}