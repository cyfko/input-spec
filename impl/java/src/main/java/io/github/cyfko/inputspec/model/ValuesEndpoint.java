package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValuesEndpoint {
    public enum Protocol { INLINE, HTTPS, HTTP, GRPC }
    public enum Mode { CLOSED, SUGGESTIONS }
    public enum PaginationStrategy { NONE, PAGE_NUMBER }
    public enum CacheStrategy { NONE, SESSION, SHORT_TERM, LONG_TERM }

    private final Protocol protocol; // default HTTPS if remote, INLINE for static
    private final Mode mode; // default CLOSED
    private final List<ValueAlias> items; // required if protocol INLINE
    private final String uri; // required if remote
    public enum HttpMethod { GET, POST }
    private final HttpMethod method; // default GET
    private final String searchField;
    private final PaginationStrategy paginationStrategy; // default NONE
    private final ResponseMapping responseMapping; // optional
    private final RequestParams requestParams; // optional
    private final CacheStrategy cacheStrategy; // optional
    private final Integer debounceMs; // optional
    private final Integer minSearchLength; // optional

    private ValuesEndpoint(Builder b) {
        this.protocol = b.protocol;
        this.mode = b.mode;
        this.items = b.items;
        this.uri = b.uri;
    this.method = b.method == null ? HttpMethod.GET : b.method;
        this.searchField = b.searchField;
        this.paginationStrategy = b.paginationStrategy;
        this.responseMapping = b.responseMapping;
        this.requestParams = b.requestParams;
        this.cacheStrategy = b.cacheStrategy;
        this.debounceMs = b.debounceMs;
        this.minSearchLength = b.minSearchLength;
    }

    @JsonCreator
    public ValuesEndpoint(
            @JsonProperty("protocol") Protocol protocol,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("items") List<ValueAlias> items,
            @JsonProperty("uri") String uri,
            @JsonProperty("method") HttpMethod method,
            @JsonProperty("searchField") String searchField,
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
        this.paginationStrategy = paginationStrategy;
        this.responseMapping = responseMapping;
        this.requestParams = requestParams;
        this.cacheStrategy = cacheStrategy;
        this.debounceMs = debounceMs;
        this.minSearchLength = minSearchLength;
    }

    public Protocol getProtocol() { return protocol; }
    public Mode getMode() { return mode; }
    public List<ValueAlias> getItems() { return items; }
    public String getUri() { return uri; }
    public HttpMethod getMethod() { return method; }
    public String getSearchField() { return searchField; }
    public PaginationStrategy getPaginationStrategy() { return paginationStrategy; }
    public ResponseMapping getResponseMapping() { return responseMapping; }
    public RequestParams getRequestParams() { return requestParams; }
    public CacheStrategy getCacheStrategy() { return cacheStrategy; }
    public Integer getDebounceMs() { return debounceMs; }
    public Integer getMinSearchLength() { return minSearchLength; }

    public static Builder builder() { return new Builder(); }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class ResponseMapping {
        private final String dataField;
        private final String pageField;
        private final String pageSizeField;
        private final String totalField;
        private final String hasNextField;

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
        public String getDataField() { return dataField; }
        public String getPageField() { return pageField; }
        public String getPageSizeField() { return pageSizeField; }
        public String getTotalField() { return totalField; }
        public String getHasNextField() { return hasNextField; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class RequestParams {
        private final String pageParam;
        private final String limitParam;
        private final String searchParam;
        private final Integer defaultLimit;

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
        public String getPageParam() { return pageParam; }
        public String getLimitParam() { return limitParam; }
        public String getSearchParam() { return searchParam; }
        public Integer getDefaultLimit() { return defaultLimit; }
    }

    public static final class Builder {
        private Protocol protocol;
        private Mode mode;
        private List<ValueAlias> items;
        private String uri;
    private HttpMethod method;
        private String searchField;
        private PaginationStrategy paginationStrategy;
        private ResponseMapping responseMapping;
        private RequestParams requestParams;
        private CacheStrategy cacheStrategy;
        private Integer debounceMs;
        private Integer minSearchLength;

        public Builder protocol(Protocol protocol) { this.protocol = protocol; return this; }
        public Builder mode(Mode mode) { this.mode = mode; return this; }
        public Builder items(List<ValueAlias> items) { this.items = items; return this; }
        public Builder uri(String uri) { this.uri = uri; return this; }
    public Builder method(HttpMethod method) { this.method = method; return this; }
        public Builder searchField(String searchField) { this.searchField = searchField; return this; }
        public Builder paginationStrategy(PaginationStrategy paginationStrategy) { this.paginationStrategy = paginationStrategy; return this; }
        public Builder responseMapping(ResponseMapping responseMapping) { this.responseMapping = responseMapping; return this; }
        public Builder requestParams(RequestParams requestParams) { this.requestParams = requestParams; return this; }
        public Builder cacheStrategy(CacheStrategy cacheStrategy) { this.cacheStrategy = cacheStrategy; return this; }
        public Builder debounceMs(Integer debounceMs) { this.debounceMs = debounceMs; return this; }
        public Builder minSearchLength(Integer minSearchLength) { this.minSearchLength = minSearchLength; return this; }
        public ValuesEndpoint build() { return new ValuesEndpoint(this); }
    }
}
