package io.github.cyfko.inputspec.model;
// ─── ValuesEndpoint ───────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.protocol.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ValuesEndpoint(
    EndpointProtocol protocol,
    ValuesMode mode,
    List<ValueAlias> items,
    String uri,
    HttpMethod method,
    ResponseMapping responseMapping,
    RequestParams requestParams,
    @JsonProperty("searchParams") JsonNode searchParams,
    @JsonProperty("searchParamsSchema") JsonNode searchParamsSchema,
    PaginationStrategy paginationStrategy,
    CacheStrategy cacheStrategy,
    Integer debounceMs,
    Integer minSearchLength
) {
    public ValuesEndpoint {
        items              = items              != null ? List.copyOf(items) : List.of();
        protocol           = protocol           != null ? protocol           : EndpointProtocol.HTTPS;
        mode               = mode               != null ? mode               : ValuesMode.CLOSED;
        method             = method             != null ? method             : HttpMethod.GET;
        paginationStrategy = paginationStrategy != null ? paginationStrategy : PaginationStrategy.NONE;
        cacheStrategy      = cacheStrategy      != null ? cacheStrategy      : CacheStrategy.NONE;
    }

    public boolean isInline() { return protocol == EndpointProtocol.INLINE; }
    public boolean isClosed() { return mode == ValuesMode.CLOSED; }
}