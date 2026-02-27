package io.github.cyfko.inputspec.protocol;// ─── HttpMethod ───────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * HTTP method for ValuesEndpoint and SubmitEndpoint (§2.2, §2.9).
 */
public enum HttpMethod {
    GET, POST, PUT;

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