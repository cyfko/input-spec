/**
 * HTTP Client factory and framework adapters for the Dynamic Input Field Specification Protocol
 * Provides seamless integration with frontend frameworks and their HTTP interceptor systems
 */

import { HttpClient, RequestOptions, CacheProvider } from './index';

// ==== FRAMEWORK ADAPTERS ====

/**
 * Angular HttpClient adapter
 * Allows integration with Angular's interceptor system and dependency injection
 */
export class AngularHttpClientAdapter implements HttpClient {
  constructor(private angularHttpClient: any) {}

  async request<T>(url: string, options: RequestOptions): Promise<T> {
    const angularOptions: any = {
      headers: options.headers || {},
      params: options.params || {},
    };

    // Let Angular handle interceptors, authentication, etc.
    const observable = this.angularHttpClient.request(
      options.method,
      url,
      {
        ...angularOptions,
        body: options.body,
        responseType: 'json',
      }
    );

    return observable.toPromise();
  }
}

/**
 * Axios adapter
 * Allows integration with existing Axios configurations and interceptors
 */
export class AxiosHttpClientAdapter implements HttpClient {
  constructor(private axiosInstance: any) {}

  async request<T>(url: string, options: RequestOptions): Promise<T> {
    const axiosConfig = {
      method: options.method.toLowerCase(),
      url,
      headers: options.headers,
      params: options.params,
      data: options.body,
      timeout: options.timeout,
    };

    const response = await this.axiosInstance.request(axiosConfig);
    return response.data;
  }
}

/**
 * Generic HTTP Client factory
 * Detects available HTTP clients and provides appropriate adapters
 */
export class HttpClientFactory {
  /**
   * Create an HTTP client using Angular's HttpClient
   * Perfect for Angular applications with interceptors
   */
  static createAngularAdapter(angularHttpClient: any): HttpClient {
    return new AngularHttpClientAdapter(angularHttpClient);
  }

  /**
   * Create an HTTP client using an Axios instance
   * Perfect for applications already using Axios with custom configuration
   */
  static createAxiosAdapter(axiosInstance: any): HttpClient {
    return new AxiosHttpClientAdapter(axiosInstance);
  }

  /**
   * Create a native fetch-based HTTP client
   * Good for applications without specific HTTP client requirements
   */
  static createFetchAdapter(baseConfig?: Partial<RequestInit>): HttpClient {
    return new ConfigurableFetchHttpClient(baseConfig);
  }

  /**
   * Auto-detect and create appropriate HTTP client
   * Tries to detect existing HTTP client instances in the environment
   */
  static createAuto(): HttpClient {
    // Try to detect Angular HttpClient
    if (typeof window !== 'undefined' && (window as any).ng) {
      console.warn('Angular detected but HttpClient not provided. Use createAngularAdapter() for better integration.');
    }

    // Fallback to fetch
    return new ConfigurableFetchHttpClient();
  }
}

/**
 * Enhanced Fetch HTTP Client with better configuration options
 * Supports custom headers, interceptors, and error handling
 */
export class ConfigurableFetchHttpClient implements HttpClient {
  private defaultHeaders: Record<string, string>;
  private baseTimeout: number;
  private interceptors: RequestInterceptor[];
  private errorHandlers: ErrorHandler[];

  constructor(
    private baseConfig: Partial<RequestInit> = {},
    options: ClientOptions = {}
  ) {
    this.defaultHeaders = options.defaultHeaders || {};
    this.baseTimeout = options.timeout || 30000;
    this.interceptors = options.interceptors || [];
    this.errorHandlers = options.errorHandlers || [];
  }

  async request<T>(url: string, options: RequestOptions): Promise<T> {
    // Apply interceptors
    let finalOptions = { ...options };
    for (const interceptor of this.interceptors) {
      finalOptions = await interceptor(url, finalOptions);
    }

    const requestInit: RequestInit = {
      ...this.baseConfig,
      method: finalOptions.method,
      headers: {
        'Content-Type': 'application/json',
        ...this.defaultHeaders,
        ...finalOptions.headers,
      },
    };

    // Add body for POST/PUT
    if (finalOptions.body && ['POST', 'PUT'].includes(finalOptions.method)) {
      requestInit.body = JSON.stringify(finalOptions.body);
    }

    // Add query parameters
    const finalUrl = this.buildUrlWithParams(url, finalOptions.params);

    // Create timeout controller
    const controller = new AbortController();
    const timeout = finalOptions.timeout || this.baseTimeout;
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    try {
      const response = await fetch(finalUrl, {
        ...requestInit,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        const error = new HttpError(
          `HTTP ${response.status}: ${response.statusText}`,
          response.status,
          response.statusText
        );

        // Apply error handlers
        for (const handler of this.errorHandlers) {
          await handler(error, response);
        }

        throw error;
      }

      const data = await response.json();
      return data as T;
    } catch (error) {
      clearTimeout(timeoutId);

      if (error instanceof HttpError) {
        throw error;
      }

      // Handle network errors, timeouts, etc.
      const httpError = new HttpError(
        error instanceof Error ? error.message : 'Network error',
        0,
        'Network Error'
      );

      for (const handler of this.errorHandlers) {
        await handler(httpError);
      }

      throw httpError;
    }
  }

  /**
   * Add a request interceptor
   * Useful for adding authentication tokens, logging, etc.
   */
  addInterceptor(interceptor: RequestInterceptor): void {
    this.interceptors.push(interceptor);
  }

  /**
   * Add an error handler
   * Useful for global error handling, logging, retries, etc.
   */
  addErrorHandler(handler: ErrorHandler): void {
    this.errorHandlers.push(handler);
  }

  private buildUrlWithParams(url: string, params?: Record<string, any>): string {
    if (!params || Object.keys(params).length === 0) {
      return url;
    }

    const urlObj = new URL(url);
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        urlObj.searchParams.append(key, String(value));
      }
    });

    return urlObj.toString();
  }
}

// ==== TYPES FOR ENHANCED HTTP CLIENT ====

export interface ClientOptions {
  defaultHeaders?: Record<string, string>;
  timeout?: number;
  interceptors?: RequestInterceptor[];
  errorHandlers?: ErrorHandler[];
}

export type RequestInterceptor = (
  url: string,
  options: RequestOptions
) => Promise<RequestOptions> | RequestOptions;

export type ErrorHandler = (
  error: HttpError,
  response?: Response
) => Promise<void> | void;

export class HttpError extends Error {
  constructor(
    message: string,
    public status: number,
    public statusText: string
  ) {
    super(message);
    this.name = 'HttpError';
  }
}

// ==== CACHE PROVIDER WITH FRAMEWORK INTEGRATION ====

/**
 * Framework-aware cache provider that can integrate with existing cache systems
 */
export class FrameworkCacheAdapter implements CacheProvider {
  constructor(private cacheImplementation: any) {}

  get<T>(key: string): T | null {
    // Delegate to framework cache (Redis, Angular HTTP cache, etc.)
    return this.cacheImplementation.get ? this.cacheImplementation.get(key) : null;
  }

  set<T>(key: string, value: T, ttlMs?: number): void {
    if (this.cacheImplementation.set) {
      this.cacheImplementation.set(key, value, ttlMs);
    }
  }

  delete(key: string): void {
    if (this.cacheImplementation.delete) {
      this.cacheImplementation.delete(key);
    }
  }

  clear(): void {
    if (this.cacheImplementation.clear) {
      this.cacheImplementation.clear();
    }
  }
}