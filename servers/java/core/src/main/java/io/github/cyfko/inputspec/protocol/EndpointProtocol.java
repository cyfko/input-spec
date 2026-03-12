package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Transport protocol for a {@link io.github.cyfko.inputspec.model.ValuesEndpoint}
 * or {@link io.github.cyfko.inputspec.model.SubmitEndpoint} (§2.2, §2.4).
 *
 * <p>Determines how the client communicates with the endpoint. {@link #INLINE}
 * is a special case that means "no network call" — the values are embedded
 * directly in the specification.</p>
 *
 * @see io.github.cyfko.inputspec.model.ValuesEndpoint
 * @see io.github.cyfko.inputspec.model.SubmitEndpoint
 */
public enum EndpointProtocol {

    /** Values are embedded in the spec — no network call needed. */
    INLINE,

    /** Secure HTTP endpoint (default for remote calls). */
    HTTPS,

    /** Plain HTTP endpoint (use only in development/internal networks). */
    HTTP,

    /** gRPC endpoint — for high-performance binary communication. */
    GRPC;

    /**
     * Deserializes a JSON string to the corresponding {@code EndpointProtocol}.
     *
     * <p>Case-insensitive. Returns {@link #HTTPS} as the default
     * for {@code null} or unrecognized values — remote endpoints
     * default to secure transport.</p>
     *
     * @param v the JSON string value (may be {@code null})
     * @return the matching enum constant, or {@link #HTTPS} as default
     */
    @JsonCreator
    public static EndpointProtocol fromJson(String v) {
        if (v == null) return HTTPS;
        return switch (v.toUpperCase()) {
            case "INLINE" -> INLINE;
            case "HTTPS"  -> HTTPS;
            case "HTTP"   -> HTTP;
            case "GRPC"   -> GRPC;
            default       -> HTTPS;
        };
    }
}