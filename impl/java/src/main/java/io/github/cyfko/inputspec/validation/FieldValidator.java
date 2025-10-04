package io.github.cyfko.inputspec.validation;

import io.github.cyfko.inputspec.ConstraintDescriptor;
import io.github.cyfko.inputspec.DataType;
import io.github.cyfko.inputspec.InputFieldSpec;
import io.github.cyfko.inputspec.ValueAlias;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Field validator implementing the validation logic as specified in the protocol.
 * 
 * Validation order from the specification:
 * 1. Check field-level required (if field is empty and required=true → error)
 * 2. Type validation (implicit from dataType)
 * 3. Execute constraints in array order:
 *    - Apply pattern (if present)
 *    - Apply min and max (interpret based on context)
 *    - Apply format (semantic hint, optional strict validation)
 *    - Apply enumValues or valuesEndpoint (if present)
 */
public class FieldValidator {
    
    /**
     * Validates only the required constraint
     * 
     * @param fieldSpec the field specification
     * @param value the value to validate
     * @return validation result
     */
    public ValidationResult validateRequired(InputFieldSpec fieldSpec, Object value) {
        if (!fieldSpec.isRequired()) {
            return new ValidationResult(true, new ArrayList<>());
        }
        
        if (isEmpty(value)) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(new ValidationError("required", "This field is required", value));
            return new ValidationResult(false, errors);
        }
        
        return new ValidationResult(true, new ArrayList<>());
    }
    
    /**
     * Validates a value against all constraints or a specific constraint by name
     * 
     * @param fieldSpec the field specification
     * @param value the value to validate
     * @param constraintName optional specific constraint name to validate
     * @return validation result
     */
    public ValidationResult validate(InputFieldSpec fieldSpec, Object value, String constraintName) {
        List<ValidationError> errors = new ArrayList<>();
        
        // 1. Check field-level required constraint first
        if (fieldSpec.isRequired() && isEmpty(value)) {
            errors.add(new ValidationError("required", "This field is required", value));
            return new ValidationResult(false, errors);
        }
        
        // If empty and not required, it's valid ONLY for non-multiple values
        // For multiple values (arrays), even empty arrays should be validated against constraints
        if (isEmpty(value) && !fieldSpec.isExpectMultipleValues()) {
            return new ValidationResult(true, new ArrayList<>());
        }
        
        // 2. Type validation (implicit from dataType)
        if (!validateType(value, fieldSpec.getDataType(), fieldSpec.isExpectMultipleValues())) {
            String expectedType = fieldSpec.isExpectMultipleValues() 
                ? "array of " + fieldSpec.getDataType().name().toLowerCase()
                : fieldSpec.getDataType().name().toLowerCase();
            errors.add(new ValidationError("type", "Expected " + expectedType + " type", value));
            return new ValidationResult(false, errors);
        }
        
        // 3. Execute constraints in array order
        if (constraintName != null) {
            // Validate only specific constraint
            ConstraintDescriptor constraint = findConstraintByName(fieldSpec.getConstraints(), constraintName);
            if (constraint == null) {
                errors.add(new ValidationError(constraintName, "Constraint '" + constraintName + "' not found"));
                return new ValidationResult(false, errors);
            }
            
            errors.addAll(validateSingleConstraint(value, constraint, fieldSpec.getDataType(), fieldSpec.isExpectMultipleValues()));
        } else {
            // Validate all constraints in order
            for (ConstraintDescriptor constraint : fieldSpec.getConstraints()) {
                errors.addAll(validateSingleConstraint(value, constraint, fieldSpec.getDataType(), fieldSpec.isExpectMultipleValues()));
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates a value against all constraints
     * 
     * @param fieldSpec the field specification
     * @param value the value to validate
     * @return validation result
     */
    public ValidationResult validate(InputFieldSpec fieldSpec, Object value) {
        return validate(fieldSpec, value, null);
    }
    
    /**
     * Validates all constraints for a field (alias for validate)
     * 
     * @param fieldSpec the field specification
     * @param value the value to validate
     * @return validation result
     */
    public ValidationResult validateAll(InputFieldSpec fieldSpec, Object value) {
        return validate(fieldSpec, value);
    }
    
    /**
     * Validates a value against a single constraint
     */
    private List<ValidationError> validateSingleConstraint(Object value, ConstraintDescriptor constraint, 
                                                          DataType dataType, boolean expectMultipleValues) {
        if (expectMultipleValues) {
            return validateArrayConstraint(value, constraint, dataType);
        } else {
            return validateSingleValueConstraint(value, constraint, dataType);
        }
    }
    
    /**
     * Validates array constraints according to the protocol
     */
    private List<ValidationError> validateArrayConstraint(Object value, ConstraintDescriptor constraint, DataType dataType) {
        List<ValidationError> errors = new ArrayList<>();
        
        if (!(value instanceof List)) {
            errors.add(new ValidationError(constraint.getName(), 
                constraint.getErrorMessage() != null ? constraint.getErrorMessage() : "Expected an array", value));
            return errors;
        }
        
        @SuppressWarnings("unchecked")
        List<Object> array = (List<Object>) value;
        
        // Validate array length (min/max for multiple values means array length)
        if (constraint.getMin() != null && constraint.getMin() instanceof Number) {
            int minLength = ((Number) constraint.getMin()).intValue();
            if (array.size() < minLength) {
                errors.add(new ValidationError(constraint.getName(),
                    constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                    "Minimum " + minLength + " items required", value));
            }
        }
        
        if (constraint.getMax() != null && constraint.getMax() instanceof Number) {
            int maxLength = ((Number) constraint.getMax()).intValue();
            if (array.size() > maxLength) {
                errors.add(new ValidationError(constraint.getName(),
                    constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                    "Maximum " + maxLength + " items allowed", value));
            }
        }
        
        // Validate each element only if constraint has element-specific rules
        // (pattern, format, enumValues apply per element according to protocol)
        if (hasElementSpecificConstraints(constraint)) {
            for (int i = 0; i < array.size(); i++) {
                Object element = array.get(i);
                List<ValidationError> elementErrors = validateSingleValueConstraint(element, constraint, dataType, 
                    constraint.getName() + "[" + i + "]");
                errors.addAll(elementErrors);
            }
        }
        
        return errors;
    }
    
    /**
     * Validates single value constraints according to the protocol
     */
    private List<ValidationError> validateSingleValueConstraint(Object value, ConstraintDescriptor constraint, 
                                                               DataType dataType) {
        return validateSingleValueConstraint(value, constraint, dataType, constraint.getName());
    }
    
    /**
     * Validates single value constraints with custom constraint name
     */
    private List<ValidationError> validateSingleValueConstraint(Object value, ConstraintDescriptor constraint, 
                                                               DataType dataType, String constraintNameOverride) {
        List<ValidationError> errors = new ArrayList<>();
        String constraintName = constraintNameOverride != null ? constraintNameOverride : constraint.getName();
        
        // String-specific validations
        if (dataType == DataType.STRING && value instanceof String) {
            String stringValue = (String) value;
            
            // Pattern validation (applies per element)
            if (constraint.getPattern() != null) {
                try {
                    Pattern pattern = Pattern.compile(constraint.getPattern());
                    if (!pattern.matcher(stringValue).matches()) {
                        errors.add(new ValidationError(constraintName,
                            constraint.getErrorMessage() != null ? constraint.getErrorMessage() : "Invalid format", value));
                    }
                } catch (PatternSyntaxException e) {
                    errors.add(new ValidationError(constraintName, "Invalid regex pattern: " + e.getMessage(), value));
                }
            }
            
            // Length validation (min/max for single STRING means character count)
            if (constraint.getMin() != null && constraint.getMin() instanceof Number) {
                int minLength = ((Number) constraint.getMin()).intValue();
                if (stringValue.length() < minLength) {
                    errors.add(new ValidationError(constraintName,
                        constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                        "Minimum " + minLength + " characters required", value));
                }
            }
            
            if (constraint.getMax() != null && constraint.getMax() instanceof Number) {
                int maxLength = ((Number) constraint.getMax()).intValue();
                if (stringValue.length() > maxLength) {
                    errors.add(new ValidationError(constraintName,
                        constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                        "Maximum " + maxLength + " characters allowed", value));
                }
            }
        }
        
        // Number-specific validations
        if (dataType == DataType.NUMBER && value instanceof Number) {
            double numericValue = ((Number) value).doubleValue();
            
            // Value validation (min/max for single NUMBER means numeric value)
            if (constraint.getMin() != null && constraint.getMin() instanceof Number) {
                double minValue = ((Number) constraint.getMin()).doubleValue();
                if (numericValue < minValue) {
                    errors.add(new ValidationError(constraintName,
                        constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                        "Minimum value is " + minValue, value));
                }
            }
            
            if (constraint.getMax() != null && constraint.getMax() instanceof Number) {
                double maxValue = ((Number) constraint.getMax()).doubleValue();
                if (numericValue > maxValue) {
                    errors.add(new ValidationError(constraintName,
                        constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                        "Maximum value is " + maxValue, value));
                }
            }
        }
        
        // Date-specific validations
        if (dataType == DataType.DATE) {
            try {
                LocalDate date = parseDate(value);
                
                // Date range validation (min/max for single DATE means date value)
                if (constraint.getMin() != null) {
                    LocalDate minDate = parseDate(constraint.getMin());
                    if (date.isBefore(minDate)) {
                        errors.add(new ValidationError(constraintName,
                            constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                            "Date must be after " + constraint.getMin(), value));
                    }
                }
                
                if (constraint.getMax() != null) {
                    LocalDate maxDate = parseDate(constraint.getMax());
                    if (date.isAfter(maxDate)) {
                        errors.add(new ValidationError(constraintName,
                            constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                            "Date must be before " + constraint.getMax(), value));
                    }
                }
            } catch (DateTimeParseException e) {
                errors.add(new ValidationError(constraintName,
                    constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                    "Invalid date format", value));
            }
        }
        
        // Enum validation
        if (constraint.getEnumValues() != null && !constraint.getEnumValues().isEmpty()) {
            boolean found = false;
            for (ValueAlias enumValue : constraint.getEnumValues()) {
                if (enumValue.getValue() != null && enumValue.getValue().equals(value)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                errors.add(new ValidationError(constraintName,
                    constraint.getErrorMessage() != null ? constraint.getErrorMessage() : 
                    "Invalid value selected", value));
            }
        }
        
        return errors;
    }
    
    /**
     * Validates type according to the protocol specification
     */
    private boolean validateType(Object value, DataType dataType, boolean expectMultipleValues) {
        if (expectMultipleValues) {
            if (!(value instanceof List)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            for (Object item : list) {
                if (!validateSingleType(item, dataType)) {
                    return false;
                }
            }
            return true;
        } else {
            return validateSingleType(value, dataType);
        }
    }
    
    /**
     * Validates single value type
     */
    private boolean validateSingleType(Object value, DataType dataType) {
        switch (dataType) {
            case STRING:
                return value instanceof String;
            case NUMBER:
                return value instanceof Number;
            case BOOLEAN:
                return value instanceof Boolean;
            case DATE:
                try {
                    parseDate(value);
                    return true;
                } catch (DateTimeParseException e) {
                    return false;
                }
            default:
                return false;
        }
    }
    
    /**
     * Checks if a value is empty according to the protocol
     */
    private boolean isEmpty(Object value) {
        return value == null ||
               (value instanceof String && ((String) value).isEmpty()) ||
               (value instanceof List && ((List<?>) value).isEmpty());
    }
    
    /**
     * Parses date value supporting multiple formats
     */
    private LocalDate parseDate(Object value) throws DateTimeParseException {
        if (value instanceof String) {
            return LocalDate.parse((String) value);
        } else if (value instanceof LocalDate) {
            return (LocalDate) value;
        } else {
            throw new DateTimeParseException("Unsupported date type", value.toString(), 0);
        }
    }
    
    /**
     * Checks if constraint has element-specific rules (pattern, format, enumValues)
     */
    private boolean hasElementSpecificConstraints(ConstraintDescriptor constraint) {
        return constraint.getPattern() != null ||
               constraint.getFormat() != null ||
               (constraint.getEnumValues() != null && !constraint.getEnumValues().isEmpty()) ||
               constraint.getValuesEndpoint() != null;
    }
    
    /**
     * Finds constraint by name in the constraints list
     */
    private ConstraintDescriptor findConstraintByName(List<ConstraintDescriptor> constraints, String name) {
        if (constraints == null || name == null) {
            return null;
        }
        return constraints.stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElse(null);
    }
}