// =========================================
// DYNAMIC INPUT FIELD SPECIFICATION PROTOCOL v2.0
// TypeScript Implementation - Zero Dependencies 
// =========================================

// Export all types
export * from './types';

// Export validation engine
export * from './validation';

// Export HTTP client abstractions and implementations
export * from './client';

// Export convenient factory functions
export { createDefaultValuesEndpoint } from './types';

// Version info
export const PROTOCOL_VERSION = '2.0';
/**
 * Library version
 */
export const LIBRARY_VERSION = "1.0.0";

/**
 * Quick start example:
 * 
 * ```typescript
 * import { 
 *   ValuesResolver, 
 *   FetchHttpClient, 
 *   MemoryCacheProvider,
 *   createDefaultValuesEndpoint 
 * } from 'input-field-spec-ts';
 * 
 * // Setup
 * const resolver = new ValuesResolver(
 *   new FetchHttpClient(),
 *   new MemoryCacheProvider()
 * );
 * 
 * // Use
 * const endpoint = createDefaultValuesEndpoint('https://api.example.com/data');
 * const result = await resolver.resolveValues(endpoint, { search: 'query' });
 * ```
 */