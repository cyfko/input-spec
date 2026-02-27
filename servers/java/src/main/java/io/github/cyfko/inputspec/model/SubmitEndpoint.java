package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.cyfko.inputspec.protocol.EndpointProtocol;
import io.github.cyfko.inputspec.protocol.HttpMethod;

/**
 * Runtime representation of the form submission endpoint.
 *
 * <p>Corresponds to the {@code SubmitEndpoint} entity of the DIFSP protocol (§2.4).
 * Describes where and how the client should submit the completed form data.</p>
 *
 * <p>The annotation processor populates this record from the {@code @FormSpec}
 * annotation's {@code submitUri}, {@code submitProtocol}, and {@code submitMethod}
 * attributes.</p>
 *
 * <p><b>Example JSON:</b></p>
 * <pre>
 * {
 *   "protocol": "HTTPS",
 *   "uri": "/api/bookings",
 *   "method": "POST"
 * }
 * </pre>
 *
 * @param protocol the submission protocol: {@link EndpointProtocol#HTTPS} (default),
 *                 {@link EndpointProtocol#HTTP}, or {@link EndpointProtocol#GRPC}
 * @param uri      the submission URI (e.g. {@code "/api/bookings"})
 * @param method   the HTTP method: {@link HttpMethod#POST} (default) or {@link HttpMethod#PUT}
 *
 * @see FormSpecModel
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitEndpoint(
    EndpointProtocol protocol,
    String uri,
    HttpMethod method
) {
    /**
     * Compact constructor — applies sensible defaults.
     *
     * <ul>
     *   <li>{@code protocol} defaults to {@link EndpointProtocol#HTTPS}</li>
     *   <li>{@code method} defaults to {@link HttpMethod#POST}</li>
     * </ul>
     */
    public SubmitEndpoint {
        protocol = protocol != null ? protocol : EndpointProtocol.HTTPS;
        method   = method   != null ? method   : HttpMethod.POST;
    }
}