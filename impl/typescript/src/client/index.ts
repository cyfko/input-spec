// Re-export everything from implementations
export * from './implementations';
export * from './framework-adapters';

// Original interfaces and ValuesResolver
import {
  ValuesEndpoint,
  ValueAlias,
  CacheStrategy,
} from '../types';

// 1. HTTP Client abstraction - Délégation de responsabilité
export interface HttpClient {
  request<T>(url: string, options: RequestOptions): Promise<T>;
}

export interface RequestOptions {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  params?: Record<string, any>;
  headers?: Record<string, string>;
  body?: any;
  timeout?: number;
}

// 2. Cache abstraction - Séparation claire
export interface CacheProvider {
  get<T>(key: string): T | null;
  set<T>(key: string, value: T, ttlMs?: number): void;
  delete(key: string): void;
  clear(): void;
}

// 3. Options pour le resolver
export interface FetchValuesOptions {
  page?: number;
  search?: string;
  limit?: number;
}

export interface FetchValuesResult {
  values: ValueAlias[];
  hasNext: boolean;
  total?: number;
  page?: number;
}

// 4. VALUES RESOLVER - Orchestrateur avec injection de dépendances
export class ValuesResolver {
  constructor(
    private httpClient: HttpClient,
    private cache: CacheProvider
  ) {}

  /**
   * Résout les valeurs pour un endpoint donné
   * Gère: debouncing, cache, pagination, search
   */
  async resolveValues(
    endpoint: ValuesEndpoint,
    options: FetchValuesOptions = {}
  ): Promise<FetchValuesResult> {
    // Debouncing pour les recherches
  const debounceMs = endpoint.debounceMs ?? 0;
  if (options.search !== undefined && debounceMs > 0) {
      return this.debouncedResolve(endpoint, options);
    }

    return this.performResolve(endpoint, options);
  }

  private debouncedResolve(
    endpoint: ValuesEndpoint,
    options: FetchValuesOptions
  ): Promise<FetchValuesResult> {
    // Implementation simple sans setTimeout pour éviter les erreurs de compilation
    // TODO: Implémenter le vrai debouncing après avoir configuré les types Node.js
    return this.performResolve(endpoint, options);
  }

  private async performResolve(
    endpoint: ValuesEndpoint,
    options: FetchValuesOptions
  ): Promise<FetchValuesResult> {
    // 1. Check cache
    const cacheKey = this.buildCacheKey(endpoint, options);
    const cached = this.getFromCache(cacheKey, endpoint.cacheStrategy);
    if (cached) {
      return cached;
    }

    // 2. Validation search length
    if (
      options.search !== undefined &&
  options.search.length < (endpoint.minSearchLength ?? 0)
    ) {
      return {
        values: [],
        hasNext: false,
        total: 0,
      };
    }

    // 3. Délégation à HttpClient
    try {
      const params = this.buildRequestParams(endpoint, options);
      const response = await this.httpClient.request(endpoint.uri, {
        method: endpoint.method as 'GET' | 'POST' | 'PUT' | 'DELETE',
        params,
      });

      const result = this.parseResponse(response, endpoint);
      
      // 4. Cache result
      this.setCache(cacheKey, result, endpoint.cacheStrategy);

      return result;
    } catch (error) {
      throw new Error(`Failed to resolve values: ${error}`);
    }
  }

  private buildCacheKey(
    endpoint: ValuesEndpoint,
    options: FetchValuesOptions
  ): string {
    const parts = [
      endpoint.uri,
      options.page || 1,
      options.search || '',
      options.limit || endpoint.requestParams?.defaultLimit || 50,
    ];
    return parts.join('|');
  }

  private buildRequestParams(
    endpoint: ValuesEndpoint,
    options: FetchValuesOptions
  ): Record<string, any> {
    const params: Record<string, any> = {};

    if (!endpoint.requestParams) {
      return params;
    }

    // Pagination
    if (endpoint.paginationStrategy === 'PAGE_NUMBER' && options.page !== undefined) {
      if (endpoint.requestParams.pageParam) {
        params[endpoint.requestParams.pageParam] = options.page;
      }
    }

    // Limit
    if (endpoint.requestParams.limitParam) {
      params[endpoint.requestParams.limitParam] =
        options.limit || endpoint.requestParams.defaultLimit || 50;
    }

    // Search
    if (endpoint.requestParams.searchParam && options.search) {
      params[endpoint.requestParams.searchParam] = options.search;
    }

    return params;
  }

  private parseResponse(
    data: any,
    endpoint: ValuesEndpoint
  ): FetchValuesResult {
    const mapping = endpoint.responseMapping;

    // Extract values
    let values: ValueAlias[];
    if (mapping.dataField) {
      values = data[mapping.dataField] || [];
    } else {
      values = Array.isArray(data) ? data : [];
    }

    // Extract pagination info
    const hasNext = mapping.hasNextField ? Boolean(data[mapping.hasNextField]) : false;
    const total = mapping.totalField ? data[mapping.totalField] : undefined;
    const page = mapping.pageField ? data[mapping.pageField] : undefined;

    return {
      values,
      hasNext,
      total,
      page,
    };
  }

  private getFromCache(
    key: string,
    strategy?: CacheStrategy
  ): FetchValuesResult | null {
    if (!strategy || strategy === 'NONE') {
      return null;
    }

    return this.cache.get<FetchValuesResult>(key);
  }

  private setCache(
    key: string,
    value: FetchValuesResult,
    strategy?: CacheStrategy
  ): void {
    if (!strategy || strategy === 'NONE') {
      return;
    }

    let ttlMs: number | undefined;
    switch (strategy) {
      case 'SESSION':
        ttlMs = undefined; // No expiration
        break;
      case 'SHORT_TERM':
        ttlMs = 5 * 60 * 1000; // 5 minutes
        break;
      case 'LONG_TERM':
        ttlMs = 60 * 60 * 1000; // 1 hour
        break;
      default:
        return;
    }

    this.cache.set(key, value, ttlMs);
  }

  /**
   * Clear cache for a specific endpoint
   */
  clearCacheForEndpoint(_endpoint: ValuesEndpoint): void {
    // Cette responsabilité est déléguée au CacheProvider
    // Ici on ne fait que coordonner
    // TODO: Implémenter cache.clearByPattern() dans CacheProvider
    this.cache.clear(); // Temporary fallback
  }
}