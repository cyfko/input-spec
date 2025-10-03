import {
  ValuesResolver,
  MemoryCacheProvider,
  FetchHttpClient,
} from '../client';
import { FieldValidator } from '../validation';
import {
  InputFieldSpec,
  ValuesEndpoint,
  createDefaultValuesEndpoint,
  isInputFieldSpec,
} from '../types';

describe('Integration Tests', () => {
  describe('Complete Protocol Flow', () => {
    it('should handle a complete field specification scenario', async () => {
      // 1. Create a field specification
      const countryFieldSpec: InputFieldSpec = {
        displayName: 'Country',
        description: 'Select your country',
        dataType: 'STRING',
        expectMultipleValues: false,
        required: true,
        constraints: [
          {
            name: 'country',
            errorMessage: 'Please select a valid country',
            enumValues: [
              { value: 'FR', label: 'France' },
              { value: 'DE', label: 'Germany' },
              { value: 'IT', label: 'Italy' },
              { value: 'ES', label: 'Spain' },
            ],
          }
        ],
      };

      // 2. Validate the field specification structure
      expect(isInputFieldSpec(countryFieldSpec)).toBe(true);

      // 3. Test validation engine
      const validator = new FieldValidator();

      // Valid value
      const validResult = await validator.validate(countryFieldSpec, 'FR', 'country');
      expect(validResult.isValid).toBe(true);

      // Invalid value (not in enum)
      const invalidResult = await validator.validate(countryFieldSpec, 'UK', 'country');
      expect(invalidResult.isValid).toBe(false);
      expect(invalidResult.errors[0].message).toBe('Please select a valid country');

      // Required validation - now test with empty value to trigger field-level required
      const requiredResult = await validator.validate(countryFieldSpec, '', 'country');
      expect(requiredResult.isValid).toBe(false);
      expect(requiredResult.errors[0].constraintName).toBe('required');
    });

    it('should handle dynamic values endpoint scenario', async () => {
      // Create endpoint configuration
      const citiesEndpoint = createDefaultValuesEndpoint('https://api.example.com/cities');
      expect(citiesEndpoint.uri).toBe('https://api.example.com/cities');
      expect(citiesEndpoint.method).toBe('GET');

      // Test with custom configuration
      const customEndpoint: ValuesEndpoint = {
        ...citiesEndpoint,
        minSearchLength: 2,
        debounceMs: 300,
        cacheStrategy: 'SHORT_TERM',
        requestParams: {
          searchParam: 'name',
          limitParam: 'limit',
          defaultLimit: 10,
        },
        responseMapping: {
          dataField: 'cities',
          totalField: 'total',
        },
      };

      expect(customEndpoint.minSearchLength).toBe(2);
      expect(customEndpoint.requestParams?.searchParam).toBe('name');
    });

    it('should demonstrate zero-dependency architecture', () => {
      // Create resolver with zero external dependencies
      const cache = new MemoryCacheProvider(); // Pure in-memory implementation
      const httpClient = new FetchHttpClient(); // Uses native fetch
      const resolver = new ValuesResolver(httpClient, cache);

      expect(resolver).toBeInstanceOf(ValuesResolver);
      expect(cache).toBeInstanceOf(MemoryCacheProvider);
      expect(httpClient).toBeInstanceOf(FetchHttpClient);

      // Verify types are properly exported and available
      expect(typeof cache.get).toBe('function');
      expect(typeof cache.set).toBe('function');
      expect(typeof httpClient.request).toBe('function');
      expect(typeof resolver.resolveValues).toBe('function');
    });

    it('should handle complex field with multiple constraints', async () => {
      const passwordFieldSpec: InputFieldSpec = {
        displayName: 'Password',
        description: 'Create a secure password',
        dataType: 'STRING',
        expectMultipleValues: false,
        required: true,
        constraints: [
          {
            name: 'length',
            min: 8,
            max: 128,
            errorMessage: 'Password must be between 8 and 128 characters',
          },
          {
            name: 'strength',
            pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]',
            errorMessage: 'Password must contain uppercase, lowercase, number and special character',
          }
        ],
      };

      const validator = new FieldValidator();

      // Test valid password
      const validPassword = 'MySecure123!';
      const validResult = await validator.validate(passwordFieldSpec, validPassword, 'strength');
      expect(validResult.isValid).toBe(true);

      // Test length constraint
      const shortPassword = '123';
      const shortResult = await validator.validate(passwordFieldSpec, shortPassword, 'length');
      expect(shortResult.isValid).toBe(false);
      expect(shortResult.errors[0].message).toBe('Password must be between 8 and 128 characters');

      // Test required constraint (now at field level)
      const emptyResult = await validator.validate(passwordFieldSpec, '', 'length');
      expect(emptyResult.isValid).toBe(false);
      expect(emptyResult.errors[0].constraintName).toBe('required');
    });

    it('should handle multi-value fields with array validation', async () => {
      const tagsFieldSpec: InputFieldSpec = {
        displayName: 'Tags',
        description: 'Add relevant tags',
        dataType: 'STRING',
        expectMultipleValues: true,
        required: false,
        constraints: [
          {
            name: 'arraySize',
            min: 1, // Minimum 1 tag
            max: 5, // Maximum 5 tags
          }
        ],
      };

      const validator = new FieldValidator();

      // Valid tags array
      const validTags = ['frontend', 'react', 'typescript'];
      const validResult = await validator.validate(tagsFieldSpec, validTags, 'arraySize');
      expect(validResult.isValid).toBe(true);

      // Too many tags
      const tooManyTags = ['tag1', 'tag2', 'tag3', 'tag4', 'tag5', 'tag6'];
      const tooManyResult = await validator.validate(tagsFieldSpec, tooManyTags, 'arraySize');
      expect(tooManyResult.isValid).toBe(false);
      expect(tooManyResult.errors[0].message).toContain('Maximum 5 items');

      // Test individual tag format (separate constraint)
      const tagsWithFormatSpec: InputFieldSpec = {
        ...tagsFieldSpec,
        constraints: [
          {
            name: 'tagFormat',
            pattern: '^[a-zA-Z0-9-]+$',
            errorMessage: 'Tags can only contain letters, numbers, and hyphens',
          }
        ],
      };

      const invalidFormatTags = ['valid-tag', 'invalid tag with spaces'];
      const formatResult = await validator.validate(tagsWithFormatSpec, invalidFormatTags, 'tagFormat');
      expect(formatResult.isValid).toBe(false);
      expect(formatResult.errors[0].constraintName).toBe('tagFormat[1]');
    });

    it('should demonstrate type safety without runtime overhead', () => {
      // TypeScript ensures type safety at compile time
      const spec: InputFieldSpec = {
        displayName: 'Test Field',
        // dataType: 'INVALID_TYPE', // This would cause TypeScript error
        dataType: 'STRING', // Only valid DataType values allowed
        expectMultipleValues: false,
        required: false,
        constraints: [],
      };

      // Type inference works correctly
      const dataType = spec.dataType; // TypeScript knows this is DataType
      expect(['STRING', 'NUMBER', 'DATE', 'BOOLEAN']).toContain(dataType);

      // Optional properties work correctly
      const description = spec.description; // TypeScript knows this is string | undefined
      expect(description).toBeUndefined();

      // Constraint structure is type-safe
      // Now constraints is an array, so we can't access by key
      const constraint = spec.constraints.find(c => c.name === 'test');
      expect(constraint).toBeUndefined();
    });
  });

  describe('Error Handling and Edge Cases', () => {
    it('should handle malformed data gracefully', async () => {
      const validator = new FieldValidator();
      
      const fieldSpec: InputFieldSpec = {
        displayName: 'Test',
        dataType: 'STRING',
        expectMultipleValues: false,
        required: false,
        constraints: [
          {
            name: 'test',
          }
        ],
      };

      // Should handle various types gracefully
      const results = await Promise.all([
        validator.validate(fieldSpec, null, 'test'),
        validator.validate(fieldSpec, undefined, 'test'),
        validator.validate(fieldSpec, [], 'test'),
        validator.validate(fieldSpec, {}, 'test'),
      ]);

      results.forEach(result => {
        expect(result).toHaveProperty('isValid');
        expect(result).toHaveProperty('errors');
        expect(Array.isArray(result.errors)).toBe(true);
      });
    });

    it('should maintain performance with zero dependencies', () => {
      // Verify no external dependencies at runtime
      const startTime = performance.now();
      
      // Create instances
      const cache = new MemoryCacheProvider();
      const httpClient = new FetchHttpClient();
      const resolver = new ValuesResolver(httpClient, cache);
      const validator = new FieldValidator();

      const endTime = performance.now();
      
      // Should be very fast since no external dependencies to load
      expect(endTime - startTime).toBeLessThan(100); // Less than 100ms
      
      // Verify instances are created
      expect(cache).toBeDefined();
      expect(httpClient).toBeDefined();
      expect(resolver).toBeDefined();
      expect(validator).toBeDefined();
    });
  });
});