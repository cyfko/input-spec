import {
  InputFieldSpec,
  ConstraintDescriptor,
  DataType,
  ValuesEndpoint,
  ValidationResult,
  isInputFieldSpec,
  isValueAlias,
  createDefaultValuesEndpoint,
} from '../types';

describe('Types Module', () => {
  describe('Type Guards', () => {
    describe('isInputFieldSpec', () => {
      it('should return true for valid InputFieldSpec', () => {
        const validSpec: InputFieldSpec = {
          displayName: 'Test Field',
          description: 'A test field',
          dataType: 'STRING',
          expectMultipleValues: false,
          required: true,
          constraints: [
            {
              name: 'format',
              description: 'Format validation',
            }
          ],
        };

        expect(isInputFieldSpec(validSpec)).toBe(true);
      });

      it('should return false for invalid objects', () => {
        expect(isInputFieldSpec(null)).toBe(false);
        expect(isInputFieldSpec(undefined)).toBe(false);
        expect(isInputFieldSpec({})).toBe(false);
        expect(isInputFieldSpec({ displayName: 'test' })).toBe(false);
        expect(isInputFieldSpec({ 
          displayName: 'test', 
          dataType: 'INVALID_TYPE',
          expectMultipleValues: false,
          required: false,
          constraints: []
        })).toBe(false);
      });

      it('should validate all required properties', () => {
        const invalidSpecs = [
          { dataType: 'STRING', expectMultipleValues: false, constraints: {} }, // missing displayName
          { displayName: 'test', expectMultipleValues: false, constraints: {} }, // missing dataType
          { displayName: 'test', dataType: 'STRING', constraints: {} }, // missing expectMultipleValues
          { displayName: 'test', dataType: 'STRING', expectMultipleValues: false }, // missing constraints
        ];

        invalidSpecs.forEach(spec => {
          expect(isInputFieldSpec(spec)).toBe(false);
        });
      });
    });

    describe('isValueAlias', () => {
      it('should return true for valid ValueAlias', () => {
        expect(isValueAlias({ value: 'test', label: 'Test Label' })).toBe(true);
        expect(isValueAlias({ value: 123, label: 'Number' })).toBe(true);
        expect(isValueAlias({ value: null, label: 'Null Value' })).toBe(true);
      });

      it('should return false for invalid objects', () => {
        expect(isValueAlias(null)).toBe(false);
        expect(isValueAlias(undefined)).toBe(false);
        expect(isValueAlias({})).toBe(false);
        expect(isValueAlias({ value: 'test' })).toBe(false); // missing label
        expect(isValueAlias({ label: 'test' })).toBe(false); // missing value
        expect(isValueAlias({ value: 'test', label: 123 })).toBe(false); // wrong label type
      });
    });
  });

  describe('Helper Functions', () => {
    describe('createDefaultValuesEndpoint', () => {
      it('should create endpoint with default values', () => {
        const endpoint = createDefaultValuesEndpoint('https://api.example.com/data');

        expect(endpoint).toEqual({
          protocol: 'HTTP',
          uri: 'https://api.example.com/data',
          method: 'GET',
          debounceMs: 300,
          minSearchLength: 0,
          responseMapping: {
            dataField: 'data',
          },
        });
      });

      it('should preserve provided URI', () => {
        const customUri = 'https://custom.api.com/endpoint';
        const endpoint = createDefaultValuesEndpoint(customUri);

        expect(endpoint.uri).toBe(customUri);
      });
    });
  });

  describe('Type Compatibility', () => {
    it('should allow valid DataType values', () => {
      const validTypes: DataType[] = ['STRING', 'NUMBER', 'DATE', 'BOOLEAN'];
      
      validTypes.forEach(type => {
        const spec: InputFieldSpec = {
          displayName: 'Test',
          dataType: type,
          expectMultipleValues: false,
          constraints: {},
        };
        expect(spec.dataType).toBe(type);
      });
    });

    it('should create valid constraint descriptors', () => {
      const constraint: ConstraintDescriptor = {
        required: true,
        description: 'Test constraint',
        errorMessage: 'Custom error',
        min: 1,
        max: 10,
        pattern: '^[a-zA-Z]+$',
        enumValues: [
          { value: 'option1', label: 'Option 1' },
          { value: 'option2', label: 'Option 2' },
        ],
      };

      expect(constraint.required).toBe(true);
      expect(constraint.min).toBe(1);
      expect(constraint.max).toBe(10);
      expect(constraint.enumValues).toHaveLength(2);
    });

    it('should create valid values endpoint configurations', () => {
      const endpoint: ValuesEndpoint = {
        uri: 'https://api.example.com/values',
        method: 'POST',
        debounceMs: 500,
        minSearchLength: 2,
        paginationStrategy: 'PAGE_NUMBER',
        cacheStrategy: 'SHORT_TERM',
        responseMapping: {
          dataField: 'results',
          hasNextField: 'hasMore',
          totalField: 'totalCount',
          pageField: 'currentPage',
        },
        requestParams: {
          searchParam: 'q',
          pageParam: 'page',
          limitParam: 'size',
          defaultLimit: 25,
        },
      };

      expect(endpoint.method).toBe('POST');
      expect(endpoint.debounceMs).toBe(500);
      expect(endpoint.paginationStrategy).toBe('PAGE_NUMBER');
      expect(endpoint.responseMapping.dataField).toBe('results');
    });
  });

  describe('Interface Completeness', () => {
    it('should support all validation result structures', () => {
      const successResult: ValidationResult = {
        isValid: true,
        errors: [],
      };

      const errorResult: ValidationResult = {
        isValid: false,
        errors: [
          {
            constraintName: 'required',
            message: 'This field is required',
            value: null,
          },
          {
            constraintName: 'pattern',
            message: 'Invalid format',
            value: 'invalid-input',
          },
        ],
      };

      expect(successResult.isValid).toBe(true);
      expect(successResult.errors).toHaveLength(0);
      expect(errorResult.isValid).toBe(false);
      expect(errorResult.errors).toHaveLength(2);
    });
  });
});