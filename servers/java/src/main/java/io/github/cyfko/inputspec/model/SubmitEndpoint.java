package io.github.cyfko.inputspec.model;// ─── SubmitEndpoint ───────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.cyfko.inputspec.protocol.EndpointProtocol;
import io.github.cyfko.inputspec.protocol.HttpMethod;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitEndpoint(
    EndpointProtocol protocol,
    String uri,
    HttpMethod method
) {
    public SubmitEndpoint {
        protocol = protocol != null ? protocol : EndpointProtocol.HTTPS;
        method   = method   != null ? method   : HttpMethod.POST;
    }
}