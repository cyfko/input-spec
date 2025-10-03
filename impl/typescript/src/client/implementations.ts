/**
 * HTTP Client and Cache Provider implementations for the Dynamic Input Field Specification Protocol
 * Zero-dependency implementations using native browser/Node.js APIs
 */
import { HttpClient, RequestOptions, CacheProvider } from './index';

// ==== IMPLÉMENTATIONS HTTP CLIENT ====

/**
 * Implementation avec fetch natif - Aucune dépendance externe
 */
export class FetchHttpClient implements HttpClient {
  private baseTimeout: number;

  constructor(baseTimeout: number = 5000) {
    this.baseTimeout = baseTimeout;
  }

  async request<T>(url: string, options: RequestOptions): Promise<T> {
    const controller = new AbortController();
    const timeout = options.timeout || this.baseTimeout;

    // Setup timeout
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    try {
      // Build URL with params
      const requestUrl = this.buildUrl(url, options.params);

      // Build fetch options
      const fetchOptions: RequestInit = {
        method: options.method,
        signal: controller.signal,
      };

      if (options.headers) {
        fetchOptions.headers = options.headers;
      }

      if (options.body) {
        fetchOptions.body = JSON.stringify(options.body);
        fetchOptions.headers = {
          'Content-Type': 'application/json',
          ...options.headers,
        };
      }

      const response = await fetch(requestUrl, fetchOptions);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } finally {
      clearTimeout(timeoutId);
    }
  }

  private buildUrl(baseUrl: string, params?: Record<string, any>): string {
    if (!params || Object.keys(params).length === 0) {
      return baseUrl;
    }

    const url = new URL(baseUrl);
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        url.searchParams.append(key, String(value));
      }
    });

    return url.toString();
  }
}

/**
 * Implementation pour Node.js avec node:http - Pas de dépendances externes
 */
export class NodeHttpClient implements HttpClient {
  async request<T>(url: string, options: RequestOptions): Promise<T> {
    // TODO: Implémenter avec node:http/https
    // Pour l'instant, fallback sur fetch
    const fetchClient = new FetchHttpClient();
    return fetchClient.request<T>(url, options);
  }
}

// ==== IMPLÉMENTATIONS CACHE ====

/**
 * Cache en mémoire simple - Aucune dépendance
 */
export class MemoryCacheProvider implements CacheProvider {
  private cache = new Map<string, { value: any; expiry?: number }>();

  get<T>(key: string): T | null {
    const item = this.cache.get(key);
    
    if (!item) {
      return null;
    }

    // Check expiry
    if (item.expiry && Date.now() > item.expiry) {
      this.cache.delete(key);
      return null;
    }

    return item.value;
  }

  set<T>(key: string, value: T, ttlMs?: number): void {
    const item: { value: T; expiry?: number } = { value };
    
    if (ttlMs !== undefined) {
      item.expiry = Date.now() + ttlMs;
    }
    
    this.cache.set(key, item);
  }

  delete(key: string): void {
    this.cache.delete(key);
  }

  clear(): void {
    this.cache.clear();
  }

  // Helper method for pattern-based clearing
  clearByPattern(pattern: string): void {
    const keys = Array.from(this.cache.keys());
    keys
      .filter((key: string) => key.includes(pattern))
      .forEach((key: string) => this.cache.delete(key));
  }
}

/**
 * Cache qui ne cache rien - Pour les tests ou si on veut désactiver le cache
 */
export class NullCacheProvider implements CacheProvider {
  get<T>(): T | null {
    return null;
  }

  set<T>(_key: string, _value: T, _ttlMs?: number): void {
    // Do nothing
  }

  delete(): void {
    // Do nothing
  }

  clear(): void {
    // Do nothing
  }
}

// ==== FACTORY POUR FACILITER L'UTILISATION ====

export interface ClientConfig {
  httpTimeout?: number;
  cacheStrategy?: 'memory' | 'null';
  httpImplementation?: 'fetch' | 'node';
}

export function createValuesResolver(config: ClientConfig = {}) {
  // Choix de l'implémentation HTTP
  let httpClient: HttpClient;
  switch (config.httpImplementation) {
    case 'node':
      httpClient = new NodeHttpClient();
      break;
    case 'fetch':
    default:
      httpClient = new FetchHttpClient(config.httpTimeout);
      break;
  }

  // Choix de l'implémentation Cache
  let cacheProvider: CacheProvider;
  switch (config.cacheStrategy) {
    case 'null':
      cacheProvider = new NullCacheProvider();
      break;
    case 'memory':
    default:
      cacheProvider = new MemoryCacheProvider();
      break;
  }

  return { httpClient, cacheProvider };
}