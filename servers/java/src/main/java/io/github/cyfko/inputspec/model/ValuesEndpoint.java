package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.protocol.*;

import java.util.List;

/**
 * Runtime representation of a value source endpoint for an input field.
 *
 * <p>Corresponds to the {@code ValuesEndpoint} entity of the DIFSP protocol (§2.2).
 * A ValuesEndpoint describes how a client should obtain the set of acceptable values
 * for a field — either inline (static list embedded in the spec) or from a remote
 * service with optional pagination, search, and caching.</p>
 *
 * <p><b>Two families of endpoints:</b></p>
 * <ul>
 *   <li><b>INLINE</b> — the {@link #items()} list contains all possible values directly
 *       in the spec. Typical for enums and short static lists.</li>
 *   <li><b>Remote (HTTPS / HTTP / GRPC)</b> — the client fetches values from {@link #uri()}
 *       at runtime. Supports {@link #paginationStrategy()}, {@link #searchParamsSchema()},
 *       {@link #cacheStrategy()}, and {@link #responseMapping()}.</li>
 * </ul>
 *
 * <p><b>Modes (§2.2):</b></p>
 * <ul>
 *   <li>{@link ValuesMode#CLOSED} — user must pick one (or more) of the listed values</li>
 *   <li>{@link ValuesMode#SUGGESTIONS} — the list is hints only; the user may type a free-form value</li>
 * </ul>
 *
 * @param protocol           the endpoint protocol: {@link EndpointProtocol#INLINE}, {@link EndpointProtocol#HTTPS},
 *                           {@link EndpointProtocol#HTTP}, or {@link EndpointProtocol#GRPC}
 * @param mode               how the values constrain user input: {@link ValuesMode#CLOSED} or {@link ValuesMode#SUGGESTIONS}
 * @param items              the static value list — non-empty only for {@code INLINE} protocol.
 *                           Each item is a {@link ValueAlias} with a canonical value and a display label
 * @param uri                the endpoint URI — required for remote protocols, ignored for INLINE
 * @param method             the HTTP method for remote calls: {@link HttpMethod#GET} (default) or {@link HttpMethod#POST}
 * @param responseMapping    describes how to extract data from the remote response body:
 *                           which field contains the items array, total count, and hasNext flag
 * @param requestParams      pagination parameters: page param name, limit param name, and default limit
 * @param searchParams       concrete default search parameter values (JSON object)
 * @param searchParamsSchema JSON Schema describing the search parameters accepted by the remote endpoint
 * @param paginationStrategy the pagination strategy: {@link PaginationStrategy#NONE} or {@link PaginationStrategy#PAGE_NUMBER}
 * @param cacheStrategy      client-side caching hint: {@link CacheStrategy#NONE}, {@link CacheStrategy#SESSION},
 *                           {@link CacheStrategy#SHORT_TERM}, or {@link CacheStrategy#LONG_TERM}
 * @param debounceMs         client-side debounce delay in milliseconds before triggering a remote search call
 * @param minSearchLength    minimum number of characters typed before triggering a remote search call
 *
 * @see InputFieldSpec
 * @see ValueAlias
 * @see ResponseMapping
 * @see RequestParams
 */
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
    /**
     * Compact constructor — applies defensive copying and sensible defaults.
     *
     * <ul>
     *   <li>{@code items} is copied via {@link List#copyOf} (immutable)</li>
     *   <li>Enum fields default to their most common value when {@code null}</li>
     * </ul>
     */
    public ValuesEndpoint {
        items              = items              != null ? List.copyOf(items) : List.of();
        protocol           = protocol           != null ? protocol           : EndpointProtocol.HTTPS;
        mode               = mode               != null ? mode               : ValuesMode.CLOSED;
        method             = method             != null ? method             : HttpMethod.GET;
        paginationStrategy = paginationStrategy != null ? paginationStrategy : PaginationStrategy.NONE;
        cacheStrategy      = cacheStrategy      != null ? cacheStrategy      : CacheStrategy.NONE;
    }

    /** Returns {@code true} if this endpoint uses the INLINE protocol (static value list). */
    public boolean isInline() { return protocol == EndpointProtocol.INLINE; }

    /** Returns {@code true} if the values mode is CLOSED (user must pick from the list). */
    public boolean isClosed() { return mode == ValuesMode.CLOSED; }
}