import { ValuesResolver } from './index';
import { FetchHttpClient, MemoryCacheProvider } from './implementations';
import { ValuesEndpoint } from '../types';

/**
 * EXEMPLE D'UTILISATION - Séparation des responsabilités
 */

// 1. Le client configure SES dépendances
const httpClient = new FetchHttpClient(10000); // 10s timeout
const cache = new MemoryCacheProvider();
const resolver = new ValuesResolver(httpClient, cache);

// 2. Configuration d'un endpoint
const countriesEndpoint: ValuesEndpoint = {
  uri: 'https://api.example.com/countries',
  method: 'GET',
  minSearchLength: 2,
  debounceMs: 300,
  cacheStrategy: 'SHORT_TERM',
  paginationStrategy: 'PAGE_NUMBER',
  requestParams: {
    searchParam: 'q',
    pageParam: 'page',
    limitParam: 'limit',
    defaultLimit: 20,
  },
  responseMapping: {
    dataField: 'data',
    hasNextField: 'hasNext',
    totalField: 'total',
    pageField: 'currentPage',
  },
};

// 3. Utilisation - Le resolver orchestre TOUT
export async function searchCountries(query: string, page = 1) {
  try {
    const result = await resolver.resolveValues(countriesEndpoint, {
      search: query,
      page,
      limit: 20,
    });

    return result;
  } catch (error) {
    console.error('Failed to search countries:', error);
    throw error;
  }
}

/**
 * AVANTAGES de cette architecture:
 * 
 * 1. TESTABILITÉ: Mock facilement HttpClient ou CacheProvider
 * 2. FLEXIBILITÉ: Le client choisit son implémentation (fetch, HttpClient customisé, etc.)
 * 3. RESPONSABILITÉS CLAIRES:
 *    - HttpClient: Fait les requêtes HTTP
 *    - CacheProvider: Gère le cache
 *    - ValuesResolver: Orchestre la logique métier
 * 4. INVERSION DE CONTRÔLE: Les dépendances sont injectées
 * 5. ÉVOLUTIVITÉ: Facile d'ajouter de nouvelles implémentations
 */

// Exemple avec différentes implémentations
export function createProductionResolver() {
  // Production: Avec cache et timeout long
  return new ValuesResolver(
    new FetchHttpClient(30000),
    new MemoryCacheProvider()
  );
}

export function createTestResolver(mockHttpClient: any) {
  // Tests: Mock HTTP, pas de cache
  return new ValuesResolver(
    mockHttpClient,
    { get: () => null, set: () => {}, delete: () => {}, clear: () => {} }
  );
}