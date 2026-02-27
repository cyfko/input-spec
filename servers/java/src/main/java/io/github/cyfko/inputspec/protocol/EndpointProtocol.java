package io.github.cyfko.inputspec.protocol;// ─── EndpointProtocol ─────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Transport protocol for a ValuesEndpoint or SubmitEndpoint (§2.2).
 * INLINE means no network call — items are embedded in the spec.
 */
public enum EndpointProtocol {
    INLINE, HTTPS, HTTP, GRPC;

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