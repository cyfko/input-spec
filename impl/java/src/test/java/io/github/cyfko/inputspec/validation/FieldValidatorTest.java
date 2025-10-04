package io.github.cyfko.inputspec.validation;

import io.github.cyfko.inputspec.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for FieldValidator based on validation logic from protocol specification
 */
class FieldValidatorTest {
    
    private FieldValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new FieldValidator();
    }
    
    @Test
    @DisplayName("Should validate required field correctly")
    void testRequiredFieldValidation() {
        // Create a required field
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("value").build();
        InputFieldSpec field = new InputFieldSpec(
            "Test Field",
            DataType.STRING,
            false,
            true, // required
            Arrays.asList(constraint)
        );
        
        // Test with null value
        ValidationResult result = validator.validate(field, null);
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("required", result.getErrors().get(0).getConstraintName());
        
        // Test with empty string
        result = validator.validate(field, "");
        assertFalse(result.isValid());
        
        // Test with valid value
        result = validator.validate(field, "test");
        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }
    
    @Test
    @DisplayName("Should validate string length constraints according to protocol")
    void testStringLengthValidation() {
        // Create field with min/max constraints for STRING type
        ConstraintDescriptor constraint = new ConstraintDescriptor("value");
        constraint.setMin(3); // minimum 3 characters
        constraint.setMax(10); // maximum 10 characters
        constraint.setErrorMessage("Length must be between 3 and 10 characters");
        
        InputFieldSpec field = new InputFieldSpec(
            "Username",
            DataType.STRING,
            false, // single value
            true,
            Arrays.asList(constraint)
        );
        
        // Test too short
        ValidationResult result = validator.validate(field, "ab");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("3"));
        
        // Test too long
        result = validator.validate(field, "verylongusername");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("10"));
        
        // Test valid length
        result = validator.validate(field, "username");
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should validate numeric range constraints according to protocol")
    void testNumericRangeValidation() {
        // Create field with min/max constraints for NUMBER type
        ConstraintDescriptor constraint = new ConstraintDescriptor("value");
        constraint.setMin(0); // minimum value 0
        constraint.setMax(150); // maximum value 150
        constraint.setErrorMessage("Value must be between 0 and 150");
        
        InputFieldSpec field = new InputFieldSpec(
            "Age",
            DataType.NUMBER,
            false, // single value
            true,
            Arrays.asList(constraint)
        );
        
        // Test below minimum
        ValidationResult result = validator.validate(field, -5);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("0"));
        
        // Test above maximum
        result = validator.validate(field, 200);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("150"));
        
        // Test valid value
        result = validator.validate(field, 25);
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should validate array length constraints for multiple values according to protocol")
    void testArrayLengthValidation() {
        // Create field with min/max constraints for array length
        ConstraintDescriptor constraint = new ConstraintDescriptor("value");
        constraint.setMin(1); // minimum 1 element
        constraint.setMax(5); // maximum 5 elements
        constraint.setErrorMessage("Must select between 1 and 5 items");
        
        InputFieldSpec field = new InputFieldSpec(
            "Tags",
            DataType.STRING,
            true, // multiple values
            false, // NOT required for this test
            Arrays.asList(constraint)
        );
        
        // Test empty array
        ValidationResult result = validator.validate(field, Arrays.asList());
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("1"));
        
        // Test too many elements
        result = validator.validate(field, Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5", "tag6"));
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("5"));
        
        // Test valid array
        result = validator.validate(field, Arrays.asList("tag1", "tag2", "tag3"));
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should validate pattern constraints according to protocol")
    void testPatternValidation() {
        // Create field with pattern constraint
        ConstraintDescriptor constraint = new ConstraintDescriptor("value");
        constraint.setPattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        constraint.setErrorMessage("Invalid email format");
        
        InputFieldSpec field = new InputFieldSpec(
            "Email",
            DataType.STRING,
            false,
            true,
            Arrays.asList(constraint)
        );
        
        // Test invalid email
        ValidationResult result = validator.validate(field, "invalid-email");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("Invalid email"));
        
        // Test valid email
        result = validator.validate(field, "user@example.com");
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should validate enum values according to protocol")
    void testEnumValidation() {
        // Create field with enum values
        List<ValueAlias> enumValues = Arrays.asList(
            new ValueAlias("active", "Active"),
            new ValueAlias("inactive", "Inactive"),
            new ValueAlias("pending", "Pending")
        );
        
        ConstraintDescriptor constraint = new ConstraintDescriptor("value");
        constraint.setEnumValues(enumValues);
        constraint.setErrorMessage("Invalid status selected");
        
        InputFieldSpec field = new InputFieldSpec(
            "Status",
            DataType.STRING,
            false,
            true,
            Arrays.asList(constraint)
        );
        
        // Test invalid value
        ValidationResult result = validator.validate(field, "invalid-status");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).getMessage().contains("Invalid status"));
        
        // Test valid value
        result = validator.validate(field, "active");
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should validate constraints in order according to protocol")
    void testConstraintOrderExecution() {
        // Create field with multiple constraints to test order
        ConstraintDescriptor lengthConstraint = new ConstraintDescriptor("length");
        lengthConstraint.setMin(5);
        lengthConstraint.setMax(20);
        lengthConstraint.setErrorMessage("Length must be 5-20 characters");
        
        ConstraintDescriptor patternConstraint = new ConstraintDescriptor("pattern");
        patternConstraint.setPattern("^[a-zA-Z]+$");
        patternConstraint.setErrorMessage("Only letters allowed");
        
        InputFieldSpec field = new InputFieldSpec(
            "Name",
            DataType.STRING,
            false,
            true,
            Arrays.asList(lengthConstraint, patternConstraint) // Order matters
        );
        
        // Test value that fails first constraint
        ValidationResult result = validator.validate(field, "ab");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.getConstraintName().equals("length")));
        
        // Test value that passes first but fails second constraint
        result = validator.validate(field, "test123");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.getConstraintName().equals("pattern")));
        
        // Test valid value
        result = validator.validate(field, "testname");
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Should handle optional fields correctly")
    void testOptionalFieldValidation() {
        // Create optional field
        ConstraintDescriptor constraint = new ConstraintDescriptor("value");
        constraint.setPattern("^[0-9]+$");
        
        InputFieldSpec field = new InputFieldSpec(
            "Optional Number",
            DataType.STRING,
            false,
            false, // not required
            Arrays.asList(constraint)
        );
        
        // Test with null value (should be valid since not required)
        ValidationResult result = validator.validate(field, null);
        assertTrue(result.isValid());
        
        // Test with empty string (should be valid since not required)
        result = validator.validate(field, "");
        assertTrue(result.isValid());
        
        // Test with invalid value (should fail pattern validation)
        result = validator.validate(field, "abc");
        assertFalse(result.isValid());
        
        // Test with valid value
        result = validator.validate(field, "123");
        assertTrue(result.isValid());
    }
}