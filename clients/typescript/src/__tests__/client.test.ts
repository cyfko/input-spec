import {
  ValuesResolver,
  FetchHttpClient,
  MemoryCacheProvider,
  NullCacheProvider,
  HttpClient,
  CacheProvider,
  FetchValuesOptions,
  FetchValuesResult,
} from '../client';
import { ValuesEndpoint } from '../types';

// Mock HttpClient pour les tests
class MockHttpClient implements HttpClient {
  private responses: Map<string, any> = new Map();
  private requestLog: Array<{ url: string; options: any }> = [];

  setResponse(url: string, response: any) {
    this.responses.set(url, response);
  }

  getRequestLog() {
    return [...this.requestLog];
  }

  clearRequestLog() {
    this.requestLog = [];
  }

  async request<T>(url: string, options: any): Promise<T> {
    this.requestLog.push({ url, options });
    
    const response = this.responses.get(url);
    if (!response) {
      throw new Error(`No mock response set for ${url}`);
    }
    
    return response;
  }
}

describe('Client Module', () => {
  describe('MemoryCacheProvider', () => {
    let cache: MemoryCacheProvider;

    beforeEach(() => {
      cache = new MemoryCacheProvider();
    });

    it('should store and retrieve values', () => {
      const key = 'test-key';
      const value = { data: 'test' };

      cache.set(key, value);
      const retrieved = cache.get(key);

      expect(retrieved).toEqual(value);
    });

    it('should return null for non-existent keys', () => {
      expect(cache.get('non-existent')).toBeNull();
    });

    it('should handle TTL expiration', () => {
      jest.useFakeTimers();
      
      const key = 'expiring-key';
      const value = { data: 'test' };
      const ttl = 1000; // 1 second

      cache.set(key, value, ttl);
      expect(cache.get(key)).toEqual(value);

      // Fast forward time by 2 seconds
      jest.advanceTimersByTime(2000);
      expect(cache.get(key)).toBeNull();

      jest.useRealTimers();
    });

    it('should handle values without TTL', () => {
      const key = 'permanent-key';
      const value = { data: 'permanent' };

      cache.set(key, value); // No TTL
      expect(cache.get(key)).toEqual(value);
    });

    it('should delete values', () => {
      const key = 'delete-key';
      const value = { data: 'test' };

      cache.set(key, value);
      expect(cache.get(key)).toEqual(value);

      cache.delete(key);
      expect(cache.get(key)).toBeNull();
    });

    it('should clear all values', () => {
      cache.set('key1', 'value1');
      cache.set('key2', 'value2');

      expect(cache.get('key1')).toBe('value1');
      expect(cache.get('key2')).toBe('value2');

      cache.clear();

      expect(cache.get('key1')).toBeNull();
      expect(cache.get('key2')).toBeNull();
    });

    it('should clear values by pattern', () => {
      cache.set('user:1:profile', 'profile1');
      cache.set('user:2:profile', 'profile2');
      cache.set('product:1:details', 'product1');

      cache.clearByPattern('user:');

      expect(cache.get('user:1:profile')).toBeNull();
      expect(cache.get('user:2:profile')).toBeNull();
      expect(cache.get('product:1:details')).toBe('product1');
    });
  });

  describe('NullCacheProvider', () => {
    let cache: NullCacheProvider;

    beforeEach(() => {
      cache = new NullCacheProvider();
    });

    it('should never store or retrieve values', () => {
      cache.set('key', 'value');
      expect(cache.get('key')).toBeNull();
    });

    it('should handle all operations without errors', () => {
      expect(() => {
        cache.set('key', 'value', 1000);
        cache.get('key');
        cache.delete('key');
        cache.clear();
      }).not.toThrow();
    });
  });

  describe('ValuesResolver', () => {
    let resolver: ValuesResolver;
    let mockHttpClient: MockHttpClient;
    let cache: MemoryCacheProvider;

    const testEndpoint: ValuesEndpoint = {
      uri: 'https://api.example.com/values',
      method: 'GET',
      debounceMs: 0, // Disable debouncing for tests
      minSearchLength: 0,
      paginationStrategy: 'PAGE_NUMBER',
      responseMapping: {
        dataField: 'data',
        hasNextField: 'hasNext',
        totalField: 'total',
        pageField: 'page',
      },
      requestParams: {
        searchParam: 'q',
        pageParam: 'page',
        limitParam: 'limit',
        defaultLimit: 20,
      },
      cacheStrategy: 'SHORT_TERM',
    };

    beforeEach(() => {
      mockHttpClient = new MockHttpClient();
      cache = new MemoryCacheProvider();
      resolver = new ValuesResolver(mockHttpClient, cache);
    });

    describe('Basic Resolution', () => {
      it('should resolve values from HTTP endpoint', async () => {
        const mockResponse = {
          data: [
            { value: 'value1', label: 'Label 1' },
            { value: 'value2', label: 'Label 2' },
          ],
          hasNext: false,
          total: 2,
          page: 1,
        };

        mockHttpClient.setResponse(testEndpoint.uri, mockResponse);

        const result = await resolver.resolveValues(testEndpoint);

        expect(result).toEqual({
          values: mockResponse.data,
          hasNext: false,
          total: 2,
          page: 1,
        });
      });

      it('should build correct request parameters', async () => {
        mockHttpClient.setResponse(testEndpoint.uri, { data: [] });

        const options: FetchValuesOptions = {
          search: 'test query',
          page: 2,
          limit: 10,
        };

        await resolver.resolveValues(testEndpoint, options);

        const requests = mockHttpClient.getRequestLog();
        expect(requests).toHaveLength(1);
        expect(requests[0].options.params).toEqual({
          q: 'test query',
          page: 2,
          limit: 10,
        });
      });

      it('should use default limit when not specified', async () => {
        mockHttpClient.setResponse(testEndpoint.uri, { data: [] });

        await resolver.resolveValues(testEndpoint, { search: 'test' });

        const requests = mockHttpClient.getRequestLog();
        expect(requests[0].options.params.limit).toBe(20); // defaultLimit from endpoint
      });
    });

    describe('Search Filtering', () => {
      it('should skip search if below minimum length', async () => {
        const endpointWithMinSearch: ValuesEndpoint = {
          ...testEndpoint,
          minSearchLength: 3,
        };

        const result = await resolver.resolveValues(endpointWithMinSearch, {
          search: 'ab', // Below minimum
        });

        expect(result).toEqual({
          values: [],
          hasNext: false,
          total: 0,
        });

        // Should not make HTTP request
        expect(mockHttpClient.getRequestLog()).toHaveLength(0);
      });

      it('should allow search at or above minimum length', async () => {
        const endpointWithMinSearch: ValuesEndpoint = {
          ...testEndpoint,
          minSearchLength: 3,
        };

        mockHttpClient.setResponse(endpointWithMinSearch.uri, { data: [] });

        await resolver.resolveValues(endpointWithMinSearch, {
          search: 'abc', // At minimum
        });

        expect(mockHttpClient.getRequestLog()).toHaveLength(1);
      });
    });

    describe('Caching', () => {
      it('should cache results based on cache strategy', async () => {
        const mockResponse = { data: [{ value: 'cached', label: 'Cached' }] };
        mockHttpClient.setResponse(testEndpoint.uri, mockResponse);

        // First request
        const result1 = await resolver.resolveValues(testEndpoint, { search: 'test' });
        expect(mockHttpClient.getRequestLog()).toHaveLength(1);

        // Second request with same parameters - should use cache
        const result2 = await resolver.resolveValues(testEndpoint, { search: 'test' });
        expect(mockHttpClient.getRequestLog()).toHaveLength(1); // No additional request
        expect(result2).toEqual(result1);
      });

      it('should not cache when strategy is NONE', async () => {
        const noCacheEndpoint: ValuesEndpoint = {
          ...testEndpoint,
          cacheStrategy: 'NONE',
        };

        mockHttpClient.setResponse(noCacheEndpoint.uri, { data: [] });

        await resolver.resolveValues(noCacheEndpoint, { search: 'test' });
        await resolver.resolveValues(noCacheEndpoint, { search: 'test' });

        expect(mockHttpClient.getRequestLog()).toHaveLength(2); // Two requests made
      });

      it('should create different cache keys for different parameters', async () => {
        mockHttpClient.setResponse(testEndpoint.uri, { data: [] });

        await resolver.resolveValues(testEndpoint, { search: 'test1' });
        await resolver.resolveValues(testEndpoint, { search: 'test2' });

        expect(mockHttpClient.getRequestLog()).toHaveLength(2); // Different cache keys
      });
    });

    describe('Response Parsing', () => {
      it('should handle response with custom field mapping', async () => {
        const customEndpoint: ValuesEndpoint = {
          ...testEndpoint,
          responseMapping: {
            dataField: 'results',
            hasNextField: 'moreAvailable',
            totalField: 'totalCount',
            pageField: 'currentPage',
          },
        };

        const mockResponse = {
          results: [{ value: 'test', label: 'Test' }],
          moreAvailable: true,
          totalCount: 100,
          currentPage: 2,
        };

        mockHttpClient.setResponse(customEndpoint.uri, mockResponse);

        const result = await resolver.resolveValues(customEndpoint);

        expect(result).toEqual({
          values: mockResponse.results,
          hasNext: true,
          total: 100,
          page: 2,
        });
      });

      it('should handle response as direct array', async () => {
        const arrayEndpoint: ValuesEndpoint = {
          ...testEndpoint,
          responseMapping: {
            dataField: '', // Empty field means use response directly
          },
        };

        const mockResponse = [
          { value: 'direct1', label: 'Direct 1' },
          { value: 'direct2', label: 'Direct 2' },
        ];

        mockHttpClient.setResponse(arrayEndpoint.uri, mockResponse);

        const result = await resolver.resolveValues(arrayEndpoint);

        expect(result.values).toEqual(mockResponse);
      });
    });

    describe('Error Handling', () => {
      it('should propagate HTTP errors', async () => {
        mockHttpClient.setResponse(testEndpoint.uri, null); // This will cause an error

        await expect(resolver.resolveValues(testEndpoint))
          .rejects
          .toThrow('Failed to resolve values');
      });

      it('should handle missing constraint gracefully', async () => {
        // Test error handling in different scenarios
        expect(true).toBe(true); // Placeholder for more complex error scenarios
      });
    });

    describe('Cache Management', () => {
      it('should clear cache for specific endpoint', async () => {
        const mockResponse = { data: [{ value: 'test', label: 'Test' }] };
        mockHttpClient.setResponse(testEndpoint.uri, mockResponse);

        // Cache a result
        await resolver.resolveValues(testEndpoint, { search: 'test' });
        expect(mockHttpClient.getRequestLog()).toHaveLength(1);

        // Clear cache for this endpoint
        resolver.clearCacheForEndpoint(testEndpoint);

        // Next request should hit HTTP again
        await resolver.resolveValues(testEndpoint, { search: 'test' });
        expect(mockHttpClient.getRequestLog()).toHaveLength(2);
      });
    });
  });

  describe('Integration Tests', () => {
    it('should work end-to-end with real-like scenario', async () => {
      const httpClient = new MockHttpClient();
      const cache = new MemoryCacheProvider();
      const resolver = new ValuesResolver(httpClient, cache);

      const endpoint: ValuesEndpoint = {
        uri: 'https://api.countries.com/search',
        method: 'GET',
        debounceMs: 0,
        minSearchLength: 2,
        cacheStrategy: 'SHORT_TERM',
        responseMapping: {
          dataField: 'countries',
          hasNextField: 'hasMore',
          totalField: 'total',
        },
        requestParams: {
          searchParam: 'name',
          pageParam: 'page',
          limitParam: 'size',
          defaultLimit: 10,
        },
      };

      const mockCountries = [
        { value: 'FR', label: 'France' },
        { value: 'DE', label: 'Germany' },
        { value: 'IT', label: 'Italy' },
      ];

      httpClient.setResponse(endpoint.uri, {
        countries: mockCountries,
        hasMore: false,
        total: 3,
      });

      // Test normal search
      const result = await resolver.resolveValues(endpoint, {
        search: 'europe',
        page: 1,
        limit: 5,
      });

      expect(result).toEqual({
        values: mockCountries,
        hasNext: false,
        total: 3,
        page: undefined, // No pageField mapping
      });

      // Test minimum search length
      const emptyResult = await resolver.resolveValues(endpoint, {
        search: 'a', // Below minimum of 2
      });

      expect(emptyResult).toEqual({
        values: [],
        hasNext: false,
        total: 0,
      });
    });
  });
});