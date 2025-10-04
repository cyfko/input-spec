package io.github.cyfko.inputspec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValueAlias class - Core data structure for protocol v1.0
 * Tests creation, serialization, and edge cases
 */
class ValueAliasTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("Should create ValueAlias with value and label")
    void testCreateValueAlias() {
        ValueAlias alias = new ValueAlias("active", "Active Status");
        
        assertEquals("active", alias.getValue());
        assertEquals("Active Status", alias.getLabel());
    }
    
    @Test
    @DisplayName("Should create ValueAlias with default constructor")
    void testDefaultConstructor() {
        ValueAlias alias = new ValueAlias();
        
        assertNull(alias.getValue());
        assertNull(alias.getLabel());
    }
    
    @Test
    @DisplayName("Should serialize and deserialize ValueAlias correctly")
    void testSerialization() throws JsonProcessingException {
        ValueAlias alias = new ValueAlias("premium", "Premium User");
        
        // Test serialization
        String json = objectMapper.writeValueAsString(alias);
        assertTrue(json.contains("\"value\":\"premium\""));
        assertTrue(json.contains("\"label\":\"Premium User\""));
        
        // Test deserialization
        ValueAlias deserialized = objectMapper.readValue(json, ValueAlias.class);
        assertEquals("premium", deserialized.getValue());
        assertEquals("Premium User", deserialized.getLabel());
    }
    
    @Test
    @DisplayName("Should handle null values correctly")
    void testNullValues() throws JsonProcessingException {
        // Test null value
        ValueAlias nullValue = new ValueAlias(null, "Null Value");
        assertEquals("Null Value", nullValue.getLabel());
        assertNull(nullValue.getValue());
        
        // Test null label
        ValueAlias nullLabel = new ValueAlias("value", null);
        assertEquals("value", nullLabel.getValue());
        assertNull(nullLabel.getLabel());
        
        // Test serialization with nulls
        String json = objectMapper.writeValueAsString(nullValue);
        ValueAlias deserialized = objectMapper.readValue(json, ValueAlias.class);
        assertNull(deserialized.getValue());
        assertEquals("Null Value", deserialized.getLabel());
    }
    
    @Test
    @DisplayName("Should handle empty values correctly")
    void testEmptyValues() {
        ValueAlias emptyValue = new ValueAlias("", "Empty Value");
        assertEquals("", emptyValue.getValue());
        assertEquals("Empty Value", emptyValue.getLabel());
        
        ValueAlias emptyLabel = new ValueAlias("value", "");
        assertEquals("value", emptyLabel.getValue());
        assertEquals("", emptyLabel.getLabel());
    }
    
    @Test
    @DisplayName("Should support setters and getters")
    void testSettersAndGetters() {
        ValueAlias alias = new ValueAlias();
        
        alias.setValue("test");
        alias.setLabel("Test Label");
        
        assertEquals("test", alias.getValue());
        assertEquals("Test Label", alias.getLabel());
        
        // Test changing values
        alias.setValue("modified");
        alias.setLabel("Modified Label");
        
        assertEquals("modified", alias.getValue());
        assertEquals("Modified Label", alias.getLabel());
    }
    
    @Test
    @DisplayName("Should handle special characters and Unicode")
    void testSpecialCharacters() throws JsonProcessingException {
        ValueAlias alias = new ValueAlias("special_@#$%", "Spéciál Çhäracters ñ 中文");
        
        String json = objectMapper.writeValueAsString(alias);
        ValueAlias deserialized = objectMapper.readValue(json, ValueAlias.class);
        
        assertEquals("special_@#$%", deserialized.getValue());
        assertEquals("Spéciál Çhäracters ñ 中文", deserialized.getLabel());
    }
    
    @Test
    @DisplayName("Should work correctly in arrays")
    void testInArrays() throws JsonProcessingException {
        ValueAlias[] aliases = {
            new ValueAlias("low", "Low Priority"),
            new ValueAlias("medium", "Medium Priority"),
            new ValueAlias("high", "High Priority")
        };
        
        String json = objectMapper.writeValueAsString(aliases);
        ValueAlias[] deserialized = objectMapper.readValue(json, ValueAlias[].class);
        
        assertEquals(3, deserialized.length);
        assertEquals("low", deserialized[0].getValue());
        assertEquals("Low Priority", deserialized[0].getLabel());
        assertEquals("high", deserialized[2].getValue());
        assertEquals("High Priority", deserialized[2].getLabel());
    }
    
    @Test
    @DisplayName("Should handle long values")
    void testLongValues() {
        StringBuilder longValueBuilder = new StringBuilder();
        StringBuilder longLabelBuilder = new StringBuilder();
        
        for (int i = 0; i < 50; i++) {
            longValueBuilder.append("very_long_value_");
        }
        for (int i = 0; i < 100; i++) {
            longLabelBuilder.append("Very Long Label ");
        }
        
        String longValue = longValueBuilder.toString(); // ~800 chars
        String longLabel = longLabelBuilder.toString(); // ~1600 chars
        
        ValueAlias alias = new ValueAlias(longValue, longLabel);
        
        assertEquals(longValue, alias.getValue());
        assertEquals(longLabel, alias.getLabel());
    }
}