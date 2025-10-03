import {
  InputFieldSpec,
  ConstraintDescriptor,
  DataType,
  ValidationResult,
  ValidationError,
} from '../types';

export class FieldValidator {
  /**
   * Validates only the required constraint
   */
  async validateRequired(
    fieldSpec: InputFieldSpec,
    value: any
  ): Promise<ValidationResult> {
    if (!fieldSpec.required) {
      return { isValid: true, errors: [] };
    }

    if (this.isEmpty(value)) {
      return {
        isValid: false,
        errors: [
          {
            constraintName: 'required',
            message: 'This field is required',
            value,
          },
        ],
      };
    }

    return { isValid: true, errors: [] };
  }

  /**
   * Validates a value against a specific constraint by name
   */
  async validate(
    fieldSpec: InputFieldSpec,
    value: any,
    constraintName?: string
  ): Promise<ValidationResult> {
    const errors: ValidationError[] = [];

    // Check required at field level first
    if (fieldSpec.required && this.isEmpty(value)) {
      errors.push({
        constraintName: 'required',
        message: 'This field is required',
        value,
      });
      return { isValid: false, errors };
    }

    // If empty and not required, it's valid
    if (this.isEmpty(value)) {
      return { isValid: true, errors: [] };
    }

    // If specific constraint name provided, validate only that one
    if (constraintName) {
      const constraint = fieldSpec.constraints.find(c => c.name === constraintName);
      if (!constraint) {
        return {
          isValid: false,
          errors: [
            {
              constraintName,
              message: `Constraint '${constraintName}' not found`,
            },
          ],
        };
      }

      const constraintErrors = this.validateSingleConstraint(
        value,
        constraint,
        fieldSpec.dataType,
        fieldSpec.expectMultipleValues
      );
      errors.push(...constraintErrors);
    } else {
      // Validate all constraints in order
      for (const constraint of fieldSpec.constraints) {
        const constraintErrors = this.validateSingleConstraint(
          value,
          constraint,
          fieldSpec.dataType,
          fieldSpec.expectMultipleValues
        );
        errors.push(...constraintErrors);
        
        // Stop on first error for better UX (optional behavior)
        // if (constraintErrors.length > 0) break;
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  }

  /**
   * Validates all constraints for a field
   */
  async validateAll(
    fieldSpec: InputFieldSpec,
    value: any
  ): Promise<ValidationResult> {
    return this.validate(fieldSpec, value);
  }

  /**
   * Validates a value against a single constraint
   */
  private validateSingleConstraint(
    value: any,
    constraint: ConstraintDescriptor,
    dataType: DataType,
    expectMultipleValues: boolean
  ): ValidationError[] {
    if (expectMultipleValues) {
      return this.validateArrayConstraint(value, constraint, dataType);
    } else {
      return this.validateSingleValueConstraint(value, constraint, dataType);
    }
  }

  private validateArrayConstraint(
    value: any,
    constraint: ConstraintDescriptor,
    dataType: DataType
  ): ValidationError[] {
    const errors: ValidationError[] = [];

    if (!Array.isArray(value)) {
      errors.push({
        constraintName: constraint.name,
        message: constraint.errorMessage || 'Expected an array',
        value,
      });
      return errors;
    }

    // Validate array length
    if (constraint.min !== undefined && typeof constraint.min === 'number' && value.length < constraint.min) {
      errors.push({
        constraintName: constraint.name,
        message:
          constraint.errorMessage || `Minimum ${constraint.min} items required`,
        value,
      });
    }

    if (constraint.max !== undefined && typeof constraint.max === 'number' && value.length > constraint.max) {
      errors.push({
        constraintName: constraint.name,
        message:
          constraint.errorMessage || `Maximum ${constraint.max} items allowed`,
        value,
      });
    }

    // Only validate individual elements if the constraint is NOT an array size constraint
    if (!constraint.name.toLowerCase().includes('arraysize') && !constraint.name.toLowerCase().includes('size')) {
      // Validate each element
      for (let i = 0; i < value.length; i++) {
        const elementErrors = this.validateSingleValueConstraint(
          value[i],
          constraint,
          dataType,
          `${constraint.name}[${i}]`
        );
        errors.push(...elementErrors);
      }
    }

    return errors;
  }

  private validateSingleValueConstraint(
    value: any,
    constraint: ConstraintDescriptor,
    dataType: DataType,
    constraintNameOverride?: string
  ): ValidationError[] {
    const errors: ValidationError[] = [];
    const constraintName = constraintNameOverride || constraint.name;

    // Type validation
    if (!this.validateType(value, dataType)) {
      errors.push({
        constraintName,
        message:
          constraint.errorMessage || `Expected ${dataType.toLowerCase()} type`,
        value,
      });
      return errors; // Don't continue if type is wrong
    }

    // String-specific validations
    if (dataType === 'STRING' && typeof value === 'string') {
      if (constraint.pattern) {
        const regex = new RegExp(constraint.pattern);
        if (!regex.test(value)) {
          errors.push({
            constraintName,
            message: constraint.errorMessage || 'Invalid format',
            value,
          });
        }
      }

      if (constraint.min !== undefined && typeof constraint.min === 'number' && value.length < constraint.min) {
        errors.push({
          constraintName,
          message:
            constraint.errorMessage ||
            `Minimum ${constraint.min} characters required`,
          value,
        });
      }

      if (constraint.max !== undefined && typeof constraint.max === 'number' && value.length > constraint.max) {
        errors.push({
          constraintName,
          message:
            constraint.errorMessage ||
            `Maximum ${constraint.max} characters allowed`,
          value,
        });
      }
    }

    // Number-specific validations
    if (dataType === 'NUMBER' && typeof value === 'number') {
      if (constraint.min !== undefined && typeof constraint.min === 'number' && value < constraint.min) {
        errors.push({
          constraintName,
          message:
            constraint.errorMessage ||
            `Minimum value is ${constraint.min}`,
          value,
        });
      }

      if (constraint.max !== undefined && typeof constraint.max === 'number' && value > constraint.max) {
        errors.push({
          constraintName,
          message:
            constraint.errorMessage ||
            `Maximum value is ${constraint.max}`,
          value,
        });
      }
    }

    // Date-specific validations
    if (dataType === 'DATE') {
      const date = new Date(value);
      if (isNaN(date.getTime())) {
        errors.push({
          constraintName,
          message: constraint.errorMessage || 'Invalid date format',
          value,
        });
        return errors;
      }

      if (constraint.min !== undefined) {
        const minDate = new Date(constraint.min);
        if (date < minDate) {
          errors.push({
            constraintName,
            message:
              constraint.errorMessage ||
              `Date must be after ${constraint.min}`,
            value,
          });
        }
      }

      if (constraint.max !== undefined) {
        const maxDate = new Date(constraint.max);
        if (date > maxDate) {
          errors.push({
            constraintName,
            message:
              constraint.errorMessage ||
              `Date must be before ${constraint.max}`,
            value,
          });
        }
      }
    }

    // Enum validation
    if (constraint.enumValues) {
      const validValues = constraint.enumValues.map((item: any) => item.value);
      if (!validValues.includes(value)) {
        errors.push({
          constraintName,
          message: constraint.errorMessage || 'Invalid value selected',
          value,
        });
      }
    }

    return errors;
  }

  private validateType(value: any, dataType: DataType): boolean {
    switch (dataType) {
      case 'STRING':
        return typeof value === 'string';
      case 'NUMBER':
        return typeof value === 'number' && !isNaN(value);
      case 'BOOLEAN':
        return typeof value === 'boolean';
      case 'DATE':
        return !isNaN(new Date(value).getTime());
      default:
        return false;
    }
  }

  private isEmpty(value: any): boolean {
    return (
      value === null ||
      value === undefined ||
      value === '' ||
      (Array.isArray(value) && value.length === 0)
    );
  }
}

// Convenience functions
export async function validateField(
  fieldSpec: InputFieldSpec,
  value: any,
  constraintName?: string
): Promise<ValidationResult> {
  const validator = new FieldValidator();
  return validator.validate(fieldSpec, value, constraintName);
}

export async function validateAllConstraints(
  fieldSpec: InputFieldSpec,
  value: any
): Promise<ValidationResult> {
  const validator = new FieldValidator();
  return validator.validateAll(fieldSpec, value);
}