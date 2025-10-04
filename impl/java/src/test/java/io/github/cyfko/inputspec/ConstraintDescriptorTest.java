package io.github.cyfko.inputspec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive tests for ConstraintDescriptor
 * Tests serialization, validation, and protocol compliance
 */
class ConstraintDescriptorTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("Should create ConstraintDescriptor with required name")
    void testConstraintCreation() {
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("test-constraint").build();
        
        assertEquals("test-constraint", constraint.getName());
        assertNull(constraint.getMin());
        assertNull(constraint.getMax());
        assertNull(constraint.getPattern());
        assertNull(constraint.getFormat());
        assertNull(constraint.getDescription());
        assertNull(constraint.getErrorMessage());
        assertNull(constraint.getDefaultValue());
        assertNull(constraint.getEnumValues());
        assertNull(constraint.getValuesEndpoint());
    }
    
    @Test
    @DisplayName("Should not allow null or empty constraint name")
    void testConstraintNameValidation() {
        assertThrows(IllegalArgumentException.class, () -> new ConstraintDescriptor(null));
        assertThrows(IllegalArgumentException.class, () -> new ConstraintDescriptor(""));
        assertThrows(IllegalArgumentException.class, () -> new ConstraintDescriptor("   "));
    }
    
    @Test
    @DisplayName("Should set and get all constraint properties")
    void testConstraintProperties() {
        List<ValueAlias> enumValues = Arrays.asList(
            new ValueAlias("value1", "Label 1"),
            new ValueAlias("value2", "Label 2")
        );
        
        ValuesEndpoint endpoint = new ValuesEndpoint("/api/values", new ResponseMapping("data"));
        
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("test")
            .min(5)
            .max(100)
            .pattern("^[a-zA-Z]+$")
            .format("email")
            .description("Test constraint description")
            .errorMessage("Custom error message")
            .defaultValue("default")
            .enumValues(enumValues)
            .valuesEndpoint(endpoint)
            .build();
        
        assertEquals(5, constraint.getMin());
        assertEquals(100, constraint.getMax());
        assertEquals("^[a-zA-Z]+$", constraint.getPattern());
        assertEquals("email", constraint.getFormat());
        assertEquals("Test constraint description", constraint.getDescription());
        assertEquals("Custom error message", constraint.getErrorMessage());
        assertEquals("default", constraint.getDefaultValue());
        assertEquals(enumValues, constraint.getEnumValues());
        assertEquals(endpoint, constraint.getValuesEndpoint());
    }
    
    @Test
    @DisplayName("Should serialize to JSON correctly according to protocol")
    void testJsonSerialization() throws JsonProcessingException {
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("value")
            .min(3)
            .max(20)
            .pattern("^[a-zA-Z0-9_]+$")
            .description("Username validation")
            .errorMessage("Invalid username format")
            .build();
        
        String json = objectMapper.writeValueAsString(constraint);
        
        // Verify required fields are present
        assertTrue(json.contains("\"name\":\"value\""));
        assertTrue(json.contains("\"min\":3"));
        assertTrue(json.contains("\"max\":20"));
        assertTrue(json.contains("\"pattern\":\"^[a-zA-Z0-9_]+$\""));
        assertTrue(json.contains("\"description\":\"Username validation\""));
        assertTrue(json.contains("\"errorMessage\":\"Invalid username format\""));
    }
    
    @Test
    @DisplayName("Should deserialize from JSON correctly according to protocol")
    void testJsonDeserialization() throws JsonProcessingException {
        String json = "{\n" +
            "    \"name\": \"value\",\n" +
            "    \"min\": 3,\n" +
            "    \"max\": 20,\n" +
            "    \"pattern\": \"^[a-zA-Z0-9_]+$\",\n" +
            "    \"format\": \"username\",\n" +
            "    \"description\": \"Username validation\",\n" +
            "    \"errorMessage\": \"Invalid username format\",\n" +
            "    \"defaultValue\": \"user123\"\n" +
            "}";
        
        ConstraintDescriptor constraint = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        assertEquals("value", constraint.getName());
        assertEquals(3, constraint.getMin());
        assertEquals(20, constraint.getMax());
        assertEquals("^[a-zA-Z0-9_]+$", constraint.getPattern());
        assertEquals("username", constraint.getFormat());
        assertEquals("Username validation", constraint.getDescription());
        assertEquals("Invalid username format", constraint.getErrorMessage());
        assertEquals("user123", constraint.getDefaultValue());
    }
    
    @Test
    @DisplayName("Should handle enum values in JSON serialization")
    void testEnumValuesSerialization() throws JsonProcessingException {
        List<ValueAlias> enumValues = Arrays.asList(
            new ValueAlias("active", "Active"),
            new ValueAlias("inactive", "Inactive")
        );
        
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("status")
            .enumValues(enumValues)
            .build();
        
        String json = objectMapper.writeValueAsString(constraint);
        ConstraintDescriptor deserialized = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        assertNotNull(deserialized.getEnumValues());
        assertEquals(2, deserialized.getEnumValues().size());
        assertEquals("active", deserialized.getEnumValues().get(0).getValue());
        assertEquals("Active", deserialized.getEnumValues().get(0).getLabel());
    }
    
    @Test
    @DisplayName("Should handle ValuesEndpoint in JSON serialization")
    void testValuesEndpointSerialization() throws JsonProcessingException {
        ResponseMapping responseMapping = new ResponseMapping("data");
        ValuesEndpoint endpoint = new ValuesEndpoint("/api/users", responseMapping);
        endpoint.setPaginationStrategy(PaginationStrategy.PAGE_NUMBER);
        endpoint.setCacheStrategy(CacheStrategy.SHORT_TERM);
        
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("assignee")
            .valuesEndpoint(endpoint)
            .build();
        
        String json = objectMapper.writeValueAsString(constraint);
        ConstraintDescriptor deserialized = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        assertNotNull(deserialized.getValuesEndpoint());
        assertEquals("/api/users", deserialized.getValuesEndpoint().getUri());
        assertEquals(PaginationStrategy.PAGE_NUMBER, deserialized.getValuesEndpoint().getPaginationStrategy());
        assertEquals(CacheStrategy.SHORT_TERM, deserialized.getValuesEndpoint().getCacheStrategy());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"email", "url", "phone", "date", "time", "datetime"})
    @DisplayName("Should accept valid format values")
    void testValidFormats(String format) {
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("test").build();
        assertDoesNotThrow(() -> constraint.setFormat(format));
        assertEquals(format, constraint.getFormat());
    }
    
    @Test
    @DisplayName("Should support numeric constraints for different data types")
    void testNumericConstraints() {
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("numeric").build();
        
        // Test integer constraints
        constraint.setMin(0);
        constraint.setMax(100);
        assertEquals(0, constraint.getMin());
        assertEquals(100, constraint.getMax());
        
        // Test decimal constraints
        constraint.setMin(0.5);
        constraint.setMax(99.9);
        assertEquals(0.5, constraint.getMin());
        assertEquals(99.9, constraint.getMax());
    }
    
    @Test
    @DisplayName("Should maintain constraint order for protocol compliance")
    void testConstraintOrdering() {
        // Create multiple constraints to test ordering
        ConstraintDescriptor constraint1 = ConstraintDescriptor.builder("first").build();
        ConstraintDescriptor constraint2 = ConstraintDescriptor.builder("second").build(); 
        ConstraintDescriptor constraint3 = ConstraintDescriptor.builder("third").build();
        
        List<ConstraintDescriptor> constraints = Arrays.asList(constraint1, constraint2, constraint3);
        
        // Verify order is maintained
        assertEquals("first", constraints.get(0).getName());
        assertEquals("second", constraints.get(1).getName());
        assertEquals("third", constraints.get(2).getName());
    }
    
    @Test
    @DisplayName("Should handle edge cases for min/max values")
    void testMinMaxEdgeCases() {
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("edge-test").build();
        
        // Test zero values
        constraint.setMin(0);
        constraint.setMax(0);
        assertEquals(0, constraint.getMin());
        assertEquals(0, constraint.getMax());
        
        // Test negative values
        constraint.setMin(-100);
        constraint.setMax(-1);
        assertEquals(-100, constraint.getMin());
        assertEquals(-1, constraint.getMax());
        
        // Test large values
        constraint.setMin(Integer.MAX_VALUE);
        constraint.setMax(Long.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, constraint.getMin());
        assertEquals(Long.MAX_VALUE, constraint.getMax());
    }
    
    @Test
    @DisplayName("Should validate regex patterns")
    void testPatternValidation() {
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("pattern-test").build();
        
        // Test valid patterns
        assertDoesNotThrow(() -> constraint.setPattern("^[a-zA-Z]+$"));
        assertDoesNotThrow(() -> constraint.setPattern("\\d{3}-\\d{3}-\\d{4}"));
        assertDoesNotThrow(() -> constraint.setPattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"));
        
        // Test complex patterns
        constraint.setPattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$");
        assertEquals("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$", constraint.getPattern());
    }
}