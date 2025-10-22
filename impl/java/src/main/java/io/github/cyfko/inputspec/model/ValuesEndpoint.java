package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Declarative description of how allowed / suggested values for a field are obtained.
 * <p>Supports both inline (static) enumerations and dynamic remote endpoints with optional
 * pagination, search and caching hints. Only a subset may be applicable depending on the
 * {@link Protocol}.</p>
 * <ul>
 *   <li>{@link Protocol#INLINE}: values resolved locally via {@link #getItems()}.</li>
 *   <li>HTTP(S)/gRPC protocols: resolved remotely via {@link #getUri()} and optional request metadata.</li>
 * </ul>
 * This object is intentionally transport oriented; validation logic consuming it should apply
 * semantics consistent with the protocol spec (e.g. CLOSED vs SUGGESTIONS domain behavior).
 *
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValuesEndpoint {
    /** Underlying transport / retrieval mechanism. */
    public enum Protocol { INLINE, HTTPS, HTTP, GRPC }
    /** Domain strictness mode (mirrors {@link ClosedDomainMode}). */
    public enum Mode { CLOSED, SUGGESTIONS }
    /** Pagination approach for remote queries. */
    public enum PaginationStrategy { NONE, PAGE_NUMBER }
    /** Suggested client caching strategy. */
    public enum CacheStrategy { NONE, SESSION, SHORT_TERM, LONG_TERM }

    /** Retrieval protocol (drives which other fields must be populated). */
    private final Protocol protocol; // default HTTPS if remote, INLINE for static
    /** Closed (only enumerated values accepted) vs suggestions (free-form allowed). */
    private final Mode mode; // default CLOSED
    /** Inline enumeration items (required when protocol INLINE). */
    private final List<ValueAlias> items; // required if protocol INLINE
    /** Remote endpoint URI (required for remote protocols). */
    private final String uri; // required if remote
    public enum HttpMethod { GET, POST }
    /** HTTP method for remote fetching (default GET). */
    private final HttpMethod method; // default GET
    /**
     * Name of a request parameter used for server-side search/filtering.
     * @deprecated As of v2.1, use {@link #searchParams} for advanced multi-criteria search
     */
    @Deprecated
    private final String searchField;
    /**
     * Advanced search parameters (key-value pairs) for multi-criteria search.
     * @since 2.1.0
     */
    private final Map<String, Object> searchParams;
    /**
     * JSON Schema object describing the structure of searchParams.
     * @since 2.1.0
     */
    private final Map<String, Object> searchParamsSchema;
    /** Pagination strategy if paging supported. */
    private final PaginationStrategy paginationStrategy; // default NONE
    /** Mapping hints to extract values from structured response. */
    private final ResponseMapping responseMapping; // optional
    /** Request parameter naming / defaults. */
    private final RequestParams requestParams; // optional
    /** Client-side caching hint. */
    private final CacheStrategy cacheStrategy; // optional
    /** Debounce interval (ms) recommended before firing remote queries. */
    private final Integer debounceMs; // optional
    /** Minimum characters before issuing a search call. */
    private final Integer minSearchLength; // optional

    private ValuesEndpoint(Builder b) {
        this.protocol = b.protocol;
        this.mode = b.mode;
        this.items = b.items;
        this.uri = b.uri;
    this.method = b.method == null ? HttpMethod.GET : b.method;
        this.searchField = b.searchField;
        this.searchParams = b.searchParams;
        this.searchParamsSchema = b.searchParamsSchema;
        this.paginationStrategy = b.paginationStrategy;
        this.responseMapping = b.responseMapping;
        this.requestParams = b.requestParams;
        this.cacheStrategy = b.cacheStrategy;
        this.debounceMs = b.debounceMs;
        this.minSearchLength = b.minSearchLength;
    }

    /**
     * Jackson / programmatic constructor.
     * @since 2.0.0
     */
    @JsonCreator
    public ValuesEndpoint(
            @JsonProperty("protocol") Protocol protocol,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("items") List<ValueAlias> items,
            @JsonProperty("uri") String uri,
            @JsonProperty("method") HttpMethod method,
            @JsonProperty("searchField") String searchField,
            @JsonProperty("searchParams") Map<String, Object> searchParams,
            @JsonProperty("searchParamsSchema") Map<String, Object> searchParamsSchema,
            @JsonProperty("paginationStrategy") PaginationStrategy paginationStrategy,
            @JsonProperty("responseMapping") ResponseMapping responseMapping,
            @JsonProperty("requestParams") RequestParams requestParams,
            @JsonProperty("cacheStrategy") CacheStrategy cacheStrategy,
            @JsonProperty("debounceMs") Integer debounceMs,
            @JsonProperty("minSearchLength") Integer minSearchLength) {
        this.protocol = protocol;
        this.mode = mode;
        this.items = items;
        this.uri = uri;
        this.method = method;
        this.searchField = searchField;
        this.searchParams = searchParams;
        this.searchParamsSchema = searchParamsSchema;
        this.paginationStrategy = paginationStrategy;
        this.responseMapping = responseMapping;
        this.requestParams = requestParams;
        this.cacheStrategy = cacheStrategy;
        this.debounceMs = debounceMs;
        this.minSearchLength = minSearchLength;
    }
    /** @return protocol used to obtain values. */
    public Protocol getProtocol() { return protocol; }
    /** @return domain strictness mode. */
    public Mode getMode() { return mode; }
    /** @return inline enumeration items (null unless protocol INLINE). */
    public List<ValueAlias> getItems() { return items; }
    /** @return remote endpoint URI (null for INLINE). */
    public String getUri() { return uri; }
    /** @return HTTP method if remote. */
    public HttpMethod getMethod() { return method; }
    /**
     * @return request search field name or null.
     * @deprecated As of v2.1, use {@link #getSearchParams()} for advanced multi-criteria search
     */
    @Deprecated
    public String getSearchField() { return searchField; }
    /**
     * @return advanced search parameters (key-value pairs) for multi-criteria search.
     * @since 2.1.0
     */
    public Map<String, Object> getSearchParams() { return searchParams; }
    /**
     * @return JSON Schema object describing the structure of searchParams.
     * @since 2.1.0
     */
    public Map<String, Object> getSearchParamsSchema() { return searchParamsSchema; }
    /** @return pagination strategy hint. */
    public PaginationStrategy getPaginationStrategy() { return paginationStrategy; }
    /** @return response mapping hints structure. */
    public ResponseMapping getResponseMapping() { return responseMapping; }
    /** @return request parameter naming hints. */
    public RequestParams getRequestParams() { return requestParams; }
    /** @return client caching strategy recommendation. */
    public CacheStrategy getCacheStrategy() { return cacheStrategy; }
    /** @return debounce interval in milliseconds before remote fetch. */
    public Integer getDebounceMs() { return debounceMs; }
    /** @return minimum search characters threshold. */
    public Integer getMinSearchLength() { return minSearchLength; }

    /**
     * Create a new builder.
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Mapping hints for extracting structured pagination + data information from a remote response body.
     * All fields are optional; absent values mean the client should rely on sensible defaults.
     * @since 2.0.0
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class ResponseMapping {
        private final String dataField;
        private final String pageField;
        private final String pageSizeField;
        private final String totalField;
        private final String hasNextField;

    /**
     * @since 2.0.0
     */
    @JsonCreator
    public ResponseMapping(
        @JsonProperty("dataField") String dataField,
        @JsonProperty("pageField") String pageField,
        @JsonProperty("pageSizeField") String pageSizeField,
        @JsonProperty("totalField") String totalField,
        @JsonProperty("hasNextField") String hasNextField) {
            this.dataField = dataField;
            this.pageField = pageField;
            this.pageSizeField = pageSizeField;
            this.totalField = totalField;
            this.hasNextField = hasNextField;
        }
    /** @since 2.0.0 */ public String getDataField() { return dataField; }
    /** @since 2.0.0 */ public String getPageField() { return pageField; }
    /** @since 2.0.0 */ public String getPageSizeField() { return pageSizeField; }
    /** @since 2.0.0 */ public String getTotalField() { return totalField; }
    /** @since 2.0.0 */ public String getHasNextField() { return hasNextField; }

        /**
         * Create a new fluent builder for {@link ResponseMapping}.
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Fluent builder for {@link ResponseMapping}. All fields optional.
         * @since 2.0.0
         */
        public static final class Builder {
            private String dataField;
            private String pageField;
            private String pageSizeField;
            private String totalField;
            private String hasNextField;

            /** @since 2.0.0 */ public Builder dataField(String v) { this.dataField = v; return this; }
            /** @since 2.0.0 */ public Builder pageField(String v) { this.pageField = v; return this; }
            /** @since 2.0.0 */ public Builder pageSizeField(String v) { this.pageSizeField = v; return this; }
            /** @since 2.0.0 */ public Builder totalField(String v) { this.totalField = v; return this; }
            /** @since 2.0.0 */ public Builder hasNextField(String v) { this.hasNextField = v; return this; }

            /**
             * Build immutable mapping instance.
             * @since 2.0.0
             */
            public ResponseMapping build() {
                return new ResponseMapping(dataField, pageField, pageSizeField, totalField, hasNextField);
            }
        }
    }

    /**
     * Request parameter naming hints and defaults for remote pagination / search endpoints.
     * @since 2.0.0
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class RequestParams {
        private final String pageParam;
        private final String limitParam;
        private final String searchParam;
        private final Integer defaultLimit;

    /**
     * @since 2.0.0
     */
    @JsonCreator
    public RequestParams(
        @JsonProperty("pageParam") String pageParam,
        @JsonProperty("limitParam") String limitParam,
        @JsonProperty("searchParam") String searchParam,
        @JsonProperty("defaultLimit") Integer defaultLimit) {
            this.pageParam = pageParam;
            this.limitParam = limitParam;
            this.searchParam = searchParam;
            this.defaultLimit = defaultLimit;
        }
    /** @since 2.0.0 */ public String getPageParam() { return pageParam; }
    /** @since 2.0.0 */ public String getLimitParam() { return limitParam; }
    /** @since 2.0.0 */ public String getSearchParam() { return searchParam; }
    /** @since 2.0.0 */ public Integer getDefaultLimit() { return defaultLimit; }

        /**
         * Create a new fluent builder for {@link RequestParams}.
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Fluent builder for {@link RequestParams}. All fields optional.
         * @since 2.0.0
         */
        public static final class Builder {
            private String pageParam;
            private String limitParam;
            private String searchParam;
            private Integer defaultLimit;

            /** @since 2.0.0 */ public Builder pageParam(String v) { this.pageParam = v; return this; }
            /** @since 2.0.0 */ public Builder limitParam(String v) { this.limitParam = v; return this; }
            /** @since 2.0.0 */ public Builder searchParam(String v) { this.searchParam = v; return this; }
            /** @since 2.0.0 */ public Builder defaultLimit(Integer v) { this.defaultLimit = v; return this; }

            /** Build immutable instance. @since 2.0.0 */
            public RequestParams build() { return new RequestParams(pageParam, limitParam, searchParam, defaultLimit); }
        }
    }

    /**
     * Fluent builder for {@link ValuesEndpoint} instances. Optional values not provided default
     * according to protocol rules / common heuristics.
     * @since 2.0.0
     */
    public static final class Builder {
        private Protocol protocol;
        private Mode mode;
        private List<ValueAlias> items;
        private String uri;
    private HttpMethod method;
        private String searchField;
        private Map<String, Object> searchParams;
        private Map<String, Object> searchParamsSchema;
        private PaginationStrategy paginationStrategy;
        private ResponseMapping responseMapping;
        private RequestParams requestParams;
        private CacheStrategy cacheStrategy;
        private Integer debounceMs;
        private Integer minSearchLength;

        /** @since 2.0.0 */ public Builder protocol(Protocol protocol) { this.protocol = protocol; return this; }
        /** @since 2.0.0 */ public Builder mode(Mode mode) { this.mode = mode; return this; }
        /** @since 2.0.0 */ public Builder items(List<ValueAlias> items) { this.items = items; return this; }
        /** @since 2.0.0 */ public Builder uri(String uri) { this.uri = uri; return this; }
        /** @since 2.0.0 */ public Builder method(HttpMethod method) { this.method = method; return this; }
        /**
         * @deprecated As of v2.1, use {@link #searchParams(Map)} for advanced multi-criteria search
         * @since 2.0.0
         */
        @Deprecated
        public Builder searchField(String searchField) { this.searchField = searchField; return this; }
        /** @since 2.1.0 */ public Builder searchParams(Map<String, Object> searchParams) { this.searchParams = searchParams; return this; }
        /** @since 2.1.0 */ public Builder searchParamsSchema(Map<String, Object> searchParamsSchema) { this.searchParamsSchema = searchParamsSchema; return this; }
        /** @since 2.0.0 */ public Builder paginationStrategy(PaginationStrategy paginationStrategy) { this.paginationStrategy = paginationStrategy; return this; }
        /** @since 2.0.0 */ public Builder responseMapping(ResponseMapping responseMapping) { this.responseMapping = responseMapping; return this; }
        /** @since 2.0.0 */ public Builder requestParams(RequestParams requestParams) { this.requestParams = requestParams; return this; }
        /** @since 2.0.0 */ public Builder cacheStrategy(CacheStrategy cacheStrategy) { this.cacheStrategy = cacheStrategy; return this; }
        /** @since 2.0.0 */ public Builder debounceMs(Integer debounceMs) { this.debounceMs = debounceMs; return this; }
        /** @since 2.0.0 */ public Builder minSearchLength(Integer minSearchLength) { this.minSearchLength = minSearchLength; return this; }
        /**
         * Build immutable endpoint instance.
         * @since 2.0.0
         */
        public ValuesEndpoint build() { return new ValuesEndpoint(this); }
    }
}
