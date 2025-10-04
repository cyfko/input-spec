package io.github.cyfko.inputspec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for protocol v1.0 enum types: DataType, PaginationStrategy, CacheStrategy
 * Validates serialization and protocol compliance
 */
class ProtocolEnumsTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("Should serialize and deserialize DataType enum correctly")
    void testDataTypeSerialization() throws JsonProcessingException {
        // Test all DataType values
        for (DataType dataType : DataType.values()) {
            String json = objectMapper.writeValueAsString(dataType);
            DataType deserialized = objectMapper.readValue(json, DataType.class);
            assertEquals(dataType, deserialized);
        }
        
        // Test specific critical values
        assertEquals("\"STRING\"", objectMapper.writeValueAsString(DataType.STRING));
        assertEquals("\"NUMBER\"", objectMapper.writeValueAsString(DataType.NUMBER));
        assertEquals("\"BOOLEAN\"", objectMapper.writeValueAsString(DataType.BOOLEAN));
        assertEquals("\"DATE\"", objectMapper.writeValueAsString(DataType.DATE));
        
        // Test deserialization
        assertEquals(DataType.STRING, objectMapper.readValue("\"STRING\"", DataType.class));
        assertEquals(DataType.NUMBER, objectMapper.readValue("\"NUMBER\"", DataType.class));
        assertEquals(DataType.BOOLEAN, objectMapper.readValue("\"BOOLEAN\"", DataType.class));
        assertEquals(DataType.DATE, objectMapper.readValue("\"DATE\"", DataType.class));
    }
    
    @Test
    @DisplayName("Should serialize and deserialize PaginationStrategy enum correctly")
    void testPaginationStrategySerialization() throws JsonProcessingException {
        // Test all PaginationStrategy values
        for (PaginationStrategy strategy : PaginationStrategy.values()) {
            String json = objectMapper.writeValueAsString(strategy);
            PaginationStrategy deserialized = objectMapper.readValue(json, PaginationStrategy.class);
            assertEquals(strategy, deserialized);
        }
        
        // Test specific values
        assertEquals("\"PAGE_NUMBER\"", objectMapper.writeValueAsString(PaginationStrategy.PAGE_NUMBER));
        
        // Test deserialization
        assertEquals(PaginationStrategy.PAGE_NUMBER, 
            objectMapper.readValue("\"PAGE_NUMBER\"", PaginationStrategy.class));
    }
    
    @Test
    @DisplayName("Should serialize and deserialize CacheStrategy enum correctly")
    void testCacheStrategySerialization() throws JsonProcessingException {
        // Test all CacheStrategy values
        for (CacheStrategy strategy : CacheStrategy.values()) {
            String json = objectMapper.writeValueAsString(strategy);
            CacheStrategy deserialized = objectMapper.readValue(json, CacheStrategy.class);
            assertEquals(strategy, deserialized);
        }
        
        // Test specific values
        assertEquals("\"SHORT_TERM\"", objectMapper.writeValueAsString(CacheStrategy.SHORT_TERM));
        assertEquals("\"LONG_TERM\"", objectMapper.writeValueAsString(CacheStrategy.LONG_TERM));
        
        // Test deserialization
        assertEquals(CacheStrategy.SHORT_TERM, 
            objectMapper.readValue("\"SHORT_TERM\"", CacheStrategy.class));
        assertEquals(CacheStrategy.LONG_TERM, 
            objectMapper.readValue("\"LONG_TERM\"", CacheStrategy.class));
    }
    
    @Test
    @DisplayName("Should handle enums in complex objects")
    void testEnumsInComplexObjects() throws JsonProcessingException {
        // Create a constraint with enum values
        ConstraintDescriptor constraint = new ConstraintDescriptor("test");
        constraint.setMin(1);
        constraint.setMax(10);
        
        InputFieldSpec field = new InputFieldSpec();
        field.setDisplayName("Test Field");
        field.setDataType(DataType.STRING);
        field.setExpectMultipleValues(false);
        field.setRequired(true);
        field.setConstraints(java.util.Arrays.asList(constraint));
        
        // Test serialization
        String json = objectMapper.writeValueAsString(field);
        assertTrue(json.contains("\"STRING\""));
        
        // Test deserialization
        InputFieldSpec deserialized = objectMapper.readValue(json, InputFieldSpec.class);
        assertEquals(DataType.STRING, deserialized.getDataType());
    }
    
    @Test
    @DisplayName("Should validate enum completeness according to protocol v1.0")
    void testEnumCompletenessProtocolV1() {
        // Verify DataType has minimum required values per protocol
        assertTrue(DataType.values().length >= 4); // At least basic types
        assertNotNull(DataType.STRING);
        assertNotNull(DataType.NUMBER);
        assertNotNull(DataType.BOOLEAN);
        assertNotNull(DataType.DATE);
        
        // Verify PaginationStrategy has required values  
        assertTrue(PaginationStrategy.values().length >= 1);
        assertNotNull(PaginationStrategy.PAGE_NUMBER);
        assertNotNull(PaginationStrategy.NONE);
        
        // Verify CacheStrategy has required values
        assertTrue(CacheStrategy.values().length >= 2);
        assertNotNull(CacheStrategy.SHORT_TERM);
        assertNotNull(CacheStrategy.LONG_TERM);
        assertNotNull(CacheStrategy.NONE);
        assertNotNull(CacheStrategy.SESSION);
    }
    
    @Test
    @DisplayName("Should handle invalid enum values gracefully")
    void testInvalidEnumValues() {
        // Test invalid DataType
        assertThrows(Exception.class, () -> {
            objectMapper.readValue("\"INVALID_TYPE\"", DataType.class);
        });
        
        // Test invalid PaginationStrategy
        assertThrows(Exception.class, () -> {
            objectMapper.readValue("\"INVALID_PAGINATION\"", PaginationStrategy.class);
        });
        
        // Test invalid CacheStrategy
        assertThrows(Exception.class, () -> {
            objectMapper.readValue("\"INVALID_CACHE\"", CacheStrategy.class);
        });
    }
    
    @Test
    @DisplayName("Should handle case sensitivity correctly")
    void testCaseSensitivity() {
        // Enums should be case-sensitive
        assertThrows(Exception.class, () -> {
            objectMapper.readValue("\"string\"", DataType.class); // lowercase
        });
        
        assertThrows(Exception.class, () -> {
            objectMapper.readValue("\"page_number\"", PaginationStrategy.class); // lowercase
        });
        
        assertThrows(Exception.class, () -> {
            objectMapper.readValue("\"short_term\"", CacheStrategy.class); // lowercase
        });
    }
    
    @Test
    @DisplayName("Should validate DataType values for protocol compliance")
    void testDataTypeProtocolCompliance() {
        // Test that all required protocol data types exist
        assertNotNull(DataType.STRING);
        assertNotNull(DataType.NUMBER);
        assertNotNull(DataType.BOOLEAN);
        assertNotNull(DataType.DATE);
        
        // Test that enum values are properly named (uppercase)
        assertEquals("STRING", DataType.STRING.name());
        assertEquals("NUMBER", DataType.NUMBER.name());
        assertEquals("BOOLEAN", DataType.BOOLEAN.name());
        assertEquals("DATE", DataType.DATE.name());
    }
    
    @Test
    @DisplayName("Should validate PaginationStrategy values for protocol compliance")
    void testPaginationStrategyProtocolCompliance() {
        assertNotNull(PaginationStrategy.PAGE_NUMBER);
        assertEquals("PAGE_NUMBER", PaginationStrategy.PAGE_NUMBER.name());
    }
    
    @Test
    @DisplayName("Should validate CacheStrategy values for protocol compliance")
    void testCacheStrategyProtocolCompliance() {
        assertNotNull(CacheStrategy.SHORT_TERM);
        assertNotNull(CacheStrategy.LONG_TERM);
        
        assertEquals("SHORT_TERM", CacheStrategy.SHORT_TERM.name());
        assertEquals("LONG_TERM", CacheStrategy.LONG_TERM.name());
    }
    
    /**
     * Helper method to check if an enum is contained in an array
     */
    private static <T extends Enum<T>> boolean containsEnum(T[] values, T target) {
        for (T value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }
}