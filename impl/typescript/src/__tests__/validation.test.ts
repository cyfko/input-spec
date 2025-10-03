import { FieldValidator, validateField } from '../validation';
import { InputFieldSpec, ConstraintDescriptor } from '../types';

describe('Validation Module', () => {
  let validator: FieldValidator;

  beforeEach(() => {
    validator = new FieldValidator();
  });

  describe('FieldValidator', () => {
    describe('Basic Validation', () => {
      it('should validate required fields', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Required Field',
          dataType: 'STRING',
          expectMultipleValues: false,
          required: true,
          constraints: [
            {
              name: 'format',
              errorMessage: 'Invalid format',
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, 'valid value', 'format');
        expect(validResult.isValid).toBe(true);
        expect(validResult.errors).toHaveLength(0);

        const emptyResult = await validator.validate(fieldSpec, '', 'format');
        expect(emptyResult.isValid).toBe(false);
        expect(emptyResult.errors).toHaveLength(1);
        expect(emptyResult.errors[0].constraintName).toBe('required');
        expect(emptyResult.errors[0].message).toBe('This field is required');

        const nullResult = await validator.validate(fieldSpec, null, 'format');
        expect(nullResult.isValid).toBe(false);
        expect(nullResult.errors).toHaveLength(1);
      });

      it('should allow empty values for non-required fields', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Optional Field',
          dataType: 'STRING',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'optional',
            }
          ],
        };

        const emptyResult = await validator.validate(fieldSpec, '', 'optional');
        expect(emptyResult.isValid).toBe(true);
        expect(emptyResult.errors).toHaveLength(0);

        const nullResult = await validator.validate(fieldSpec, null, 'optional');
        expect(nullResult.isValid).toBe(true);
        expect(nullResult.errors).toHaveLength(0);
      });
    });

    describe('Data Type Validation', () => {
      it('should validate STRING type', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'String Field',
          dataType: 'STRING',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'value',
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, 'valid string', 'value');
        expect(validResult.isValid).toBe(true);

        const invalidResult = await validator.validate(fieldSpec, 123, 'value');
        expect(invalidResult.isValid).toBe(false);
        expect(invalidResult.errors[0].message).toContain('string type');
      });

      it('should validate NUMBER type', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Number Field',
          dataType: 'NUMBER',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'value',
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, 42, 'value');
        expect(validResult.isValid).toBe(true);

        const invalidResult = await validator.validate(fieldSpec, 'not a number', 'value');
        expect(invalidResult.isValid).toBe(false);
        expect(invalidResult.errors[0].message).toContain('number type');
      });

      it('should validate BOOLEAN type', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Boolean Field',
          dataType: 'BOOLEAN',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'value',
            }
          ],
        };

        const trueResult = await validator.validate(fieldSpec, true, 'value');
        expect(trueResult.isValid).toBe(true);

        const falseResult = await validator.validate(fieldSpec, false, 'value');
        expect(falseResult.isValid).toBe(true);

        const invalidResult = await validator.validate(fieldSpec, 'not boolean', 'value');
        expect(invalidResult.isValid).toBe(false);
        expect(invalidResult.errors[0].message).toContain('boolean type');
      });

      it('should validate DATE type', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Date Field',
          dataType: 'DATE',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'value',
            }
          ],
        };

        const validDateResult = await validator.validate(fieldSpec, '2023-12-25', 'value');
        expect(validDateResult.isValid).toBe(true);

        const validDateObjResult = await validator.validate(fieldSpec, new Date(), 'value');
        expect(validDateObjResult.isValid).toBe(true);

        const invalidResult = await validator.validate(fieldSpec, 'invalid date', 'value');
        expect(invalidResult.isValid).toBe(false);
        expect(invalidResult.errors[0].message).toContain('date type');
      });
    });

    describe('String Constraints', () => {
      it('should validate string length constraints', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'String Field',
          dataType: 'STRING',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'length',
              min: 3,
              max: 10,
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, 'valid', 'length');
        expect(validResult.isValid).toBe(true);

        const tooShortResult = await validator.validate(fieldSpec, 'hi', 'length');
        expect(tooShortResult.isValid).toBe(false);
        expect(tooShortResult.errors[0].message).toContain('Minimum 3 characters');

        const tooLongResult = await validator.validate(fieldSpec, 'this is too long', 'length');
        expect(tooLongResult.isValid).toBe(false);
        expect(tooLongResult.errors[0].message).toContain('Maximum 10 characters');
      });

      it('should validate string pattern constraints', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Email Field',
          dataType: 'STRING',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'email',
              pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
              errorMessage: 'Please enter a valid email address',
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, 'test@example.com', 'email');
        expect(validResult.isValid).toBe(true);

        const invalidResult = await validator.validate(fieldSpec, 'invalid-email', 'email');
        expect(invalidResult.isValid).toBe(false);
        expect(invalidResult.errors[0].message).toBe('Please enter a valid email address');
      });
    });

    describe('Number Constraints', () => {
      it('should validate numeric range constraints', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Age Field',
          dataType: 'NUMBER',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'age',
              min: 18,
              max: 65,
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, 25, 'age');
        expect(validResult.isValid).toBe(true);

        const tooLowResult = await validator.validate(fieldSpec, 16, 'age');
        expect(tooLowResult.isValid).toBe(false);
        expect(tooLowResult.errors[0].message).toContain('Minimum value is 18');

        const tooHighResult = await validator.validate(fieldSpec, 70, 'age');
        expect(tooHighResult.isValid).toBe(false);
        expect(tooHighResult.errors[0].message).toContain('Maximum value is 65');
      });
    });

    describe('Date Constraints', () => {
      it('should validate date range constraints', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Date Field',
          dataType: 'DATE',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'dateRange',
              min: '2023-01-01',
              max: '2023-12-31',
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, '2023-06-15', 'dateRange');
        expect(validResult.isValid).toBe(true);

        const tooEarlyResult = await validator.validate(fieldSpec, '2022-12-31', 'dateRange');
        expect(tooEarlyResult.isValid).toBe(false);
        expect(tooEarlyResult.errors[0].message).toContain('after 2023-01-01');

        const tooLateResult = await validator.validate(fieldSpec, '2024-01-01', 'dateRange');
        expect(tooLateResult.isValid).toBe(false);
        expect(tooLateResult.errors[0].message).toContain('before 2023-12-31');
      });
    });

    describe('Enum Validation', () => {
      it('should validate enum constraints', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Status Field',
          dataType: 'STRING',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'status',
              enumValues: [
                { value: 'active', label: 'Active' },
                { value: 'inactive', label: 'Inactive' },
                { value: 'pending', label: 'Pending' },
              ],
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, 'active', 'status');
        expect(validResult.isValid).toBe(true);

        const invalidResult = await validator.validate(fieldSpec, 'unknown', 'status');
        expect(invalidResult.isValid).toBe(false);
        expect(invalidResult.errors[0].message).toContain('Invalid value selected');
      });
    });

    describe('Array Validation', () => {
      it('should validate arrays when expectMultipleValues is true', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Tags Field',
          dataType: 'STRING',
          expectMultipleValues: true,
          required: false,
          constraints: [
            {
              name: 'tags',
              min: 2,
              max: 5,
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, ['tag1', 'tag2', 'tag3'], 'tags');
        expect(validResult.isValid).toBe(true);

        const tooFewResult = await validator.validate(fieldSpec, ['tag1'], 'tags');
        expect(tooFewResult.isValid).toBe(false);
        expect(tooFewResult.errors[0].message).toContain('Minimum 2 items');

        const tooManyResult = await validator.validate(fieldSpec, ['tag1', 'tag2', 'tag3', 'tag4', 'tag5', 'tag6'], 'tags');
        expect(tooManyResult.isValid).toBe(false);
        expect(tooManyResult.errors[0].message).toContain('Maximum 5 items');

        const notArrayResult = await validator.validate(fieldSpec, 'not an array', 'tags');
        expect(notArrayResult.isValid).toBe(false);
        expect(notArrayResult.errors[0].message).toContain('Expected an array');
      });

      it('should validate each element in array', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Numbers Field',
          dataType: 'NUMBER',
          expectMultipleValues: true,
          required: false,
          constraints: [
            {
              name: 'numbers',
              min: 1,
              max: 100,
            }
          ],
        };

        const validResult = await validator.validate(fieldSpec, [10, 20, 30], 'numbers');
        expect(validResult.isValid).toBe(true);

        const invalidElementResult = await validator.validate(fieldSpec, [10, 'not a number', 30], 'numbers');
        expect(invalidElementResult.isValid).toBe(false);
        expect(invalidElementResult.errors[0].constraintName).toBe('numbers[1]');
      });
    });

    describe('Error Handling', () => {
      it('should return error for non-existent constraint', async () => {
        const fieldSpec: InputFieldSpec = {
          displayName: 'Test Field',
          dataType: 'STRING',
          expectMultipleValues: false,
          required: false,
          constraints: [],
        };

        const result = await validator.validate(fieldSpec, 'value', 'nonExistent');
        expect(result.isValid).toBe(false);
        expect(result.errors[0].message).toContain('Constraint \'nonExistent\' not found');
      });

      it('should use custom error messages', async () => {
        const customMessage = 'Custom validation error';
        const fieldSpec: InputFieldSpec = {
          displayName: 'Test Field',
          dataType: 'STRING',
          expectMultipleValues: false,
          required: false,
          constraints: [
            {
              name: 'custom',
              pattern: '^valid$', // Only 'valid' is accepted
              errorMessage: customMessage,
            }
          ],
        };

        const result = await validator.validate(fieldSpec, 'invalid', 'custom');
        expect(result.isValid).toBe(false);
        expect(result.errors[0].message).toBe(customMessage);
      });
    });
  });

  describe('Convenience Functions', () => {
    it('should provide validateField convenience function', async () => {
      const fieldSpec: InputFieldSpec = {
        displayName: 'Test Field',
        dataType: 'STRING',
        expectMultipleValues: false,
        required: true,
        constraints: [
          {
            name: 'value',
          }
        ],
      };

      const result = await validateField(fieldSpec, 'test value');
      expect(result.isValid).toBe(true);
    });
  });
});