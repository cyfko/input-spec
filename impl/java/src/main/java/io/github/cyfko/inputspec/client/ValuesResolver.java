package io.github.cyfko.inputspec.client;

import io.github.cyfko.inputspec.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Values resolver orchestrating the fetching of values from endpoints
 * according to the Dynamic Input Field Specification Protocol v1.0.
 * 
 * Handles: debouncing, cache, pagination, search as specified in the protocol.
 */
public class ValuesResolver {
    
    private final HttpClient httpClient;
    private final CacheProvider cacheProvider;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final Map<String, CompletableFuture<FetchValuesResult>> pendingRequests;
    
    /**
     * Constructor with dependencies injection
     * 
     * @param httpClient HTTP client for making requests
     * @param cacheProvider cache provider for caching results
     */
    public ValuesResolver(HttpClient httpClient, CacheProvider cacheProvider) {
        this.httpClient = httpClient;
        this.cacheProvider = cacheProvider;
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.pendingRequests = new HashMap<>();
    }
    
    /**
     * Resolves values for an endpoint according to the protocol.
     * Handles debouncing, cache, pagination, search.
     * 
     * @param endpoint the values endpoint configuration
     * @param options fetch options (page, search, limit)
     * @return future with fetch values result
     */
    public CompletableFuture<FetchValuesResult> resolveValues(ValuesEndpoint endpoint, FetchValuesOptions options) {
        if (options == null) {
            options = new FetchValuesOptions();
        }
        
        // Debouncing for searches according to the protocol
        if (options.getSearch() != null && !options.getSearch().isEmpty() && endpoint.getDebounceMs() > 0) {
            return debouncedResolve(endpoint, options);
        }
        
        return performResolve(endpoint, options);
    }
    
    /**
     * Convenience method with default options
     * 
     * @param endpoint the values endpoint configuration
     * @return future with fetch values result
     */
    public CompletableFuture<FetchValuesResult> resolveValues(ValuesEndpoint endpoint) {
        return resolveValues(endpoint, new FetchValuesOptions());
    }
    
    /**
     * Debounced resolve implementation
     */
    private CompletableFuture<FetchValuesResult> debouncedResolve(ValuesEndpoint endpoint, FetchValuesOptions options) {
        String debounceKey = buildDebounceKey(endpoint, options);
        
        synchronized (pendingRequests) {
            // Cancel previous pending request for this key
            CompletableFuture<FetchValuesResult> existing = pendingRequests.get(debounceKey);
            if (existing != null && !existing.isDone()) {
                existing.cancel(true);
            }
            
            // Create new debounced request
            CompletableFuture<FetchValuesResult> future = new CompletableFuture<>();
            pendingRequests.put(debounceKey, future);
            
            scheduler.schedule(() -> {
                performResolve(endpoint, options)
                    .whenComplete((result, throwable) -> {
                        synchronized (pendingRequests) {
                            pendingRequests.remove(debounceKey);
                        }
                        if (throwable != null) {
                            future.completeExceptionally(throwable);
                        } else {
                            future.complete(result);
                        }
                    });
            }, endpoint.getDebounceMs(), TimeUnit.MILLISECONDS);
            
            return future;
        }
    }
    
    /**
     * Performs the actual resolve operation
     */
    private CompletableFuture<FetchValuesResult> performResolve(ValuesEndpoint endpoint, FetchValuesOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Check cache according to the protocol cache strategy
                String cacheKey = buildCacheKey(endpoint, options);
                FetchValuesResult cached = getFromCache(cacheKey, endpoint.getCacheStrategy());
                if (cached != null) {
                    return cached;
                }
                
                // 2. Validation search length according to the protocol
                if (options.getSearch() != null && 
                    options.getSearch().length() < endpoint.getMinSearchLength()) {
                    return new FetchValuesResult(new ArrayList<>(), false, 0, null);
                }
                
                // 3. Build request parameters according to the protocol
                Map<String, Object> params = buildRequestParams(endpoint, options);
                
                // 4. Make HTTP request
                String responseBody = httpClient.request(endpoint.getUri(), endpoint.getMethod().name(), params, null);
                
                // 5. Parse response according to response mapping
                FetchValuesResult result = parseResponse(responseBody, endpoint);
                
                // 6. Cache result according to cache strategy
                setCache(cacheKey, result, endpoint.getCacheStrategy());
                
                return result;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve values: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Builds cache key for the request
     */
    private String buildCacheKey(ValuesEndpoint endpoint, FetchValuesOptions options) {
        String[] parts = {
            endpoint.getUri(),
            String.valueOf(options.getPage() != null ? options.getPage() : 1),
            options.getSearch() != null ? options.getSearch() : "",
            String.valueOf(options.getLimit() != null ? options.getLimit() : 
                (endpoint.getRequestParams() != null ? endpoint.getRequestParams().getDefaultLimit() : 50))
        };
        return String.join("|", parts);
    }
    
    /**
     * Builds debounce key for deduplicating requests
     */
    private String buildDebounceKey(ValuesEndpoint endpoint, FetchValuesOptions options) {
        return buildCacheKey(endpoint, options);
    }
    
    /**
     * Builds request parameters according to the protocol specification
     */
    private Map<String, Object> buildRequestParams(ValuesEndpoint endpoint, FetchValuesOptions options) {
        Map<String, Object> params = new HashMap<>();
        
        RequestParams requestParams = endpoint.getRequestParams();
        if (requestParams == null) {
            return params;
        }
        
        // Pagination according to the protocol
        if (endpoint.getPaginationStrategy() == PaginationStrategy.PAGE_NUMBER && 
            options.getPage() != null && requestParams.getPageParam() != null) {
            params.put(requestParams.getPageParam(), options.getPage());
        }
        
        // Limit parameter
        if (requestParams.getLimitParam() != null) {
            int limit = options.getLimit() != null ? options.getLimit() : 
                       (requestParams.getDefaultLimit() != null ? requestParams.getDefaultLimit() : 50);
            params.put(requestParams.getLimitParam(), limit);
        }
        
        // Search parameter
        if (requestParams.getSearchParam() != null && options.getSearch() != null && !options.getSearch().isEmpty()) {
            params.put(requestParams.getSearchParam(), options.getSearch());
        }
        
        return params;
    }
    
    /**
     * Parses response according to response mapping specification
     */
    private FetchValuesResult parseResponse(String responseBody, ValuesEndpoint endpoint) throws Exception {
        JsonNode data = objectMapper.readTree(responseBody);
        ResponseMapping mapping = endpoint.getResponseMapping();
        
        // Extract values according to mapping
        JsonNode valuesNode;
        if (mapping.getDataField() != null && !mapping.getDataField().isEmpty()) {
            valuesNode = data.get(mapping.getDataField());
        } else {
            valuesNode = data; // Root response is assumed to be the array
        }
        
        List<ValueAlias> values = new ArrayList<>();
        if (valuesNode != null && valuesNode.isArray()) {
            for (JsonNode valueNode : valuesNode) {
                ValueAlias valueAlias = objectMapper.treeToValue(valueNode, ValueAlias.class);
                values.add(valueAlias);
            }
        }
        
        // Extract pagination info according to mapping
        boolean hasNext = false;
        if (mapping.getHasNextField() != null) {
            JsonNode hasNextNode = data.get(mapping.getHasNextField());
            hasNext = hasNextNode != null && hasNextNode.asBoolean();
        }
        
        Integer total = null;
        if (mapping.getTotalField() != null) {
            JsonNode totalNode = data.get(mapping.getTotalField());
            total = totalNode != null ? totalNode.asInt() : null;
        }
        
        Integer page = null;
        if (mapping.getPageField() != null) {
            JsonNode pageNode = data.get(mapping.getPageField());
            page = pageNode != null ? pageNode.asInt() : null;
        }
        
        return new FetchValuesResult(values, hasNext, total, page);
    }
    
    /**
     * Gets cached result according to cache strategy
     */
    private FetchValuesResult getFromCache(String key, CacheStrategy strategy) {
        if (strategy == null || strategy == CacheStrategy.NONE) {
            return null;
        }
        
        return cacheProvider.get(key, FetchValuesResult.class);
    }
    
    /**
     * Sets cache according to cache strategy and protocol specification
     */
    private void setCache(String key, FetchValuesResult value, CacheStrategy strategy) {
        if (strategy == null || strategy == CacheStrategy.NONE) {
            return;
        }
        
        Long ttlMs = null;
        switch (strategy) {
            case SESSION:
                ttlMs = null; // No expiration
                break;
            case SHORT_TERM:
                ttlMs = 5L * 60 * 1000; // 5 minutes as specified
                break;
            case LONG_TERM:
                ttlMs = 60L * 60 * 1000; // 1 hour as specified
                break;
            default:
                return;
        }
        
        cacheProvider.set(key, value, ttlMs);
    }
    
    /**
     * Clears cache for a specific endpoint
     * 
     * @param endpoint the endpoint to clear cache for
     */
    public void clearCacheForEndpoint(ValuesEndpoint endpoint) {
        // Simple implementation - clear all cache
        // In a real implementation, we would clear only keys matching the endpoint
        cacheProvider.clear();
    }
    
    /**
     * Shuts down the scheduler
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}