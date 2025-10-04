package io.github.cyfko.inputspec.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for ValidationError - ensures proper error reporting
 */
class ValidationErrorTest {
    
    @Test
    @DisplayName("Should create ValidationError with all properties")
    void testValidationErrorCreation() {
        Object testValue = "invalid-value";
        ValidationError error = new ValidationError("pattern", "Invalid format", testValue);
        
        assertEquals("pattern", error.getConstraintName());
        assertEquals("Invalid format", error.getMessage());
        assertEquals(testValue, error.getValue());
    }
    
    @Test
    @DisplayName("Should handle null values appropriately")
    void testNullValueHandling() {
        ValidationError error = new ValidationError("required", "Field is required", null);
        
        assertEquals("required", error.getConstraintName());
        assertEquals("Field is required", error.getMessage());
        assertNull(error.getValue());
    }
    
    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void testEqualsAndHashCode() {
        ValidationError error1 = new ValidationError("min", "Too short", "ab");
        ValidationError error2 = new ValidationError("min", "Too short", "ab");
        ValidationError error3 = new ValidationError("max", "Too long", "verylongtext");
        
        // Test equality
        assertEquals(error1, error2);
        assertNotEquals(error1, error3);
        
        // Test hash code
        assertEquals(error1.hashCode(), error2.hashCode());
        assertNotEquals(error1.hashCode(), error3.hashCode());
    }
    
    @Test
    @DisplayName("Should provide meaningful toString representation")
    void testToString() {
        ValidationError error = new ValidationError("email", "Invalid email format", "invalid@");
        
        String toString = error.toString();
        assertTrue(toString.contains("email"));
        assertTrue(toString.contains("Invalid email format"));
        assertTrue(toString.contains("invalid@"));
    }
}

/**
 * Tests for ValidationResult - ensures proper validation result handling
 */
class ValidationResultTest {
    
    @Test
    @DisplayName("Should create valid ValidationResult with no errors")
    void testValidResult() {
        ValidationResult result = new ValidationResult(true, Arrays.asList());
        
        assertTrue(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    @DisplayName("Should create invalid ValidationResult with errors")
    void testInvalidResult() {
        List<ValidationError> errors = Arrays.asList(
            new ValidationError("min", "Too short", "ab"),
            new ValidationError("pattern", "Invalid format", "ab")
        );
        
        ValidationResult result = new ValidationResult(false, errors);
        
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertEquals("min", result.getErrors().get(0).getConstraintName());
        assertEquals("pattern", result.getErrors().get(1).getConstraintName());
    }
    
    @Test
    @DisplayName("Should maintain consistency between valid flag and errors list")
    void testConsistency() {
        // Valid result should have empty errors
        ValidationResult validResult = new ValidationResult(true, Arrays.asList());
        assertTrue(validResult.isValid());
        assertTrue(validResult.getErrors().isEmpty());
        
        // Invalid result should have errors
        List<ValidationError> errors = Arrays.asList(
            new ValidationError("required", "Field required", null)
        );
        ValidationResult invalidResult = new ValidationResult(false, errors);
        assertFalse(invalidResult.isValid());
        assertFalse(invalidResult.getErrors().isEmpty());
    }
    
    @Test
    @DisplayName("Should handle large number of errors efficiently")
    void testManyErrors() {
        List<ValidationError> manyErrors = Arrays.asList(
            new ValidationError("min", "Too short", "a"),
            new ValidationError("max", "Too long", "verylongtext"),
            new ValidationError("pattern", "Invalid pattern", "123"),
            new ValidationError("format", "Invalid format", "invalid"),
            new ValidationError("enum", "Invalid enum", "wrong")
        );
        
        ValidationResult result = new ValidationResult(false, manyErrors);
        
        assertFalse(result.isValid());
        assertEquals(5, result.getErrors().size());
        
        // Verify all errors are present
        List<String> constraintNames = result.getErrors().stream()
            .map(ValidationError::getConstraintName)
            .toList();
        
        assertTrue(constraintNames.contains("min"));
        assertTrue(constraintNames.contains("max"));
        assertTrue(constraintNames.contains("pattern"));
        assertTrue(constraintNames.contains("format"));
        assertTrue(constraintNames.contains("enum"));
    }
    
    @Test
    @DisplayName("Should provide immutable errors list")
    void testImmutableErrors() {
        List<ValidationError> originalErrors = Arrays.asList(
            new ValidationError("test", "Test error", "value")
        );
        
        ValidationResult result = new ValidationResult(false, originalErrors);
        
        // Verify we can't modify the returned list
        List<ValidationError> returnedErrors = result.getErrors();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedErrors.add(new ValidationError("new", "New error", "new"));
        });
    }
    
    @Test
    @DisplayName("Should handle null errors list gracefully")
    void testNullErrors() {
        // Should not throw exception, should use empty list
        ValidationResult result = new ValidationResult(true, null);
        
        assertTrue(result.isValid());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }
}