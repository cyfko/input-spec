/**
 * Dynamic Values Example
 * 
 * This example demonstrates how to use ValuesResolver for dynamic value
 * resolution with HTTP APIs, caching, and search functionality.
 */

import { 
  ValuesResolver, 
  FetchHttpClient, 
  MemoryCacheProvider,
  ValuesEndpoint 
} from '../src';

// Mock HTTP client for demo purposes
class MockHttpClient {
  async request<T>(url: string, options: any): Promise<T> {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 100));

    // Mock country data
    const mockCountries = [
      { name: { common: 'France' }, cca2: 'FR' },
      { name: { common: 'Germany' }, cca2: 'DE' },
      { name: { common: 'Italy' }, cca2: 'IT' },
      { name: { common: 'Spain' }, cca2: 'ES' },
      { name: { common: 'United Kingdom' }, cca2: 'GB' },
    ];

    // Filter by search if provided
    let filtered = mockCountries;
    if (options.params?.name) {
      const search = options.params.name.toLowerCase();
      filtered = mockCountries.filter(country => 
        country.name.common.toLowerCase().includes(search)
      );
    }

    // Apply pagination
    const page = options.params?.page || 1;
    const limit = options.params?.limit || 10;
    const start = (page - 1) * limit;
    const end = start + limit;
    
    const paginated = filtered.slice(start, end);

    return {
      data: paginated.map(country => ({
        value: country.cca2,
        label: country.name.common
      })),
      hasNext: end < filtered.length,
      total: filtered.length,
      page: page
    } as T;
  }
}

// Configure endpoint
const countriesEndpoint: ValuesEndpoint = {
  uri: 'https://restcountries.com/v3.1/all',
  method: 'GET',
  debounceMs: 0, // Disabled for demo
  minSearchLength: 0,
  cacheStrategy: 'SHORT_TERM',
  paginationStrategy: 'PAGE_NUMBER',
  responseMapping: {
    dataField: 'data',
    hasNextField: 'hasNext',
    totalField: 'total',
    pageField: 'page'
  },
  requestParams: {
    searchParam: 'name',
    pageParam: 'page',
    limitParam: 'limit',
    defaultLimit: 5
  }
};

async function runDynamicValuesExamples() {
  // Setup resolver with mock client for demo
  const httpClient = new MockHttpClient() as any;
  const cache = new MemoryCacheProvider();
  const resolver = new ValuesResolver(httpClient, cache);

  console.log('🌐 Dynamic Values Examples\n');

  // Test 1: Basic resolution
  console.log('1. Fetching all countries (first page)...');
  const allCountries = await resolver.resolveValues(countriesEndpoint, {
    page: 1,
    limit: 3
  });
  console.log(`   Found ${allCountries.values.length} countries:`);
  allCountries.values.forEach(country => {
    console.log(`   - ${country.label} (${country.value})`);
  });
  console.log(`   Has more: ${allCountries.hasNext ? 'Yes' : 'No'}`);

  // Test 2: Search functionality
  console.log('\n2. Searching for countries containing "fr"...');
  const searchResults = await resolver.resolveValues(countriesEndpoint, {
    search: 'fr',
    limit: 5
  });
  console.log(`   Found ${searchResults.values.length} countries:`);
  searchResults.values.forEach(country => {
    console.log(`   - ${country.label} (${country.value})`);
  });

  // Test 3: Caching demonstration
  console.log('\n3. Testing cache functionality...');
  
  console.log('   First request (will hit API)...');
  const start1 = Date.now();
  await resolver.resolveValues(countriesEndpoint, { search: 'germany' });
  const time1 = Date.now() - start1;
  console.log(`   Request took: ${time1}ms`);

  console.log('   Second request (should use cache)...');
  const start2 = Date.now();
  await resolver.resolveValues(countriesEndpoint, { search: 'germany' });
  const time2 = Date.now() - start2;
  console.log(`   Request took: ${time2}ms`);
  
  console.log(`   Cache speedup: ${time1 > time2 ? '✅ Cache working!' : '❌ Cache not working'}`);

  // Test 4: Pagination
  console.log('\n4. Testing pagination...');
  const page1 = await resolver.resolveValues(countriesEndpoint, {
    page: 1,
    limit: 2
  });
  console.log(`   Page 1: ${page1.values.length} items, hasNext: ${page1.hasNext}`);

  if (page1.hasNext) {
    const page2 = await resolver.resolveValues(countriesEndpoint, {
      page: 2,
      limit: 2
    });
    console.log(`   Page 2: ${page2.values.length} items, hasNext: ${page2.hasNext}`);
  }

  // Test 5: Error handling
  console.log('\n5. Testing error handling...');
  const errorEndpoint: ValuesEndpoint = {
    ...countriesEndpoint,
    uri: 'https://invalid-url-that-will-fail.com/api'
  };

  try {
    await resolver.resolveValues(errorEndpoint);
    console.log('   ❌ Expected error but got success');
  } catch (error) {
    console.log('   ✅ Error handled gracefully');
    console.log(`   Error message: ${(error as Error).message}`);
  }
}

async function runRealWorldExample() {
  console.log('\n🏢 Real-World Example: User Registration Form\n');

  const httpClient = new FetchHttpClient();
  const cache = new MemoryCacheProvider();
  const resolver = new ValuesResolver(httpClient, cache);

  // Simulate form field that needs dynamic country list
  console.log('Simulating user typing in country field...');
  
  const searches = ['uni', 'unite', 'united'];
  
  for (const search of searches) {
    console.log(`\n   User typed: "${search}"`);
    
    // This would normally be called by your UI framework
    // when user types in an autocomplete field
    try {
      const results = await resolver.resolveValues(countriesEndpoint, {
        search: search,
        limit: 3
      });
      
      console.log(`   Suggestions (${results.values.length}):`);
      results.values.forEach(country => {
        console.log(`     • ${country.label}`);
      });
    } catch (error) {
      console.log('     ❌ Error loading suggestions');
    }
  }
}

// Run examples if this file is executed directly
async function main() {
  await runDynamicValuesExamples();
  await runRealWorldExample();
}

// Auto-run if this is the main module
if (typeof window === 'undefined' && typeof process !== 'undefined') {
  main().catch(console.error);
}

// Export for use in other examples
export { 
  runDynamicValuesExamples, 
  runRealWorldExample,
  MockHttpClient,
  countriesEndpoint 
};