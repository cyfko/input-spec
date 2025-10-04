package io.github.cyfko.inputspec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

/**
 * Tests pour le Builder Pattern de ConstraintDescriptor
 * Validation de la fluent API et comparaison avec l'approche traditionnelle
 */
class ConstraintDescriptorBuilderTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("Should create constraint using builder pattern")
    void testBuilderBasicUsage() {
        // Utilisation du builder pattern
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("username")
            .description("Username validation")
            .min(3)
            .max(20)
            .pattern("^[a-zA-Z0-9_]+$")
            .errorMessage("Username must be 3-20 characters, alphanumeric only")
            .build();
        
        // Validation
        assertEquals("username", constraint.getName());
        assertEquals("Username validation", constraint.getDescription());
        assertEquals(3, constraint.getMin());
        assertEquals(20, constraint.getMax());
        assertEquals("^[a-zA-Z0-9_]+$", constraint.getPattern());
        assertEquals("Username must be 3-20 characters, alphanumeric only", constraint.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should create complex constraint with builder")
    void testBuilderComplexConstraint() {
        // Configuration endpoint dynamique
        ResponseMapping mapping = ResponseMapping.builder()
            .dataField("users")
            .build();
        
        ValuesEndpoint endpoint = ValuesEndpoint.builder("/api/users", mapping)
            .cacheStrategy(CacheStrategy.SHORT_TERM)
            .paginationStrategy(PaginationStrategy.PAGE_NUMBER)
            .build();
        
        // Contrainte complexe avec builder
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("assignee")
            .description("Select user to assign task")
            .errorMessage("Please select a valid user")
            .defaultValue("unassigned")
            .valuesEndpoint(endpoint)
            .build();
        
        // Validation
        assertEquals("assignee", constraint.getName());
        assertEquals("Select user to assign task", constraint.getDescription());
        assertEquals("Please select a valid user", constraint.getErrorMessage());
        assertEquals("unassigned", constraint.getDefaultValue());
        assertNotNull(constraint.getValuesEndpoint());
        assertEquals("/api/users", constraint.getValuesEndpoint().getUri());
        assertEquals(CacheStrategy.SHORT_TERM, constraint.getValuesEndpoint().getCacheStrategy());
    }
    
    @Test
    @DisplayName("Should create enum constraint with builder")
    void testBuilderEnumConstraint() {
        List<ValueAlias> priorities = Arrays.asList(
            new ValueAlias("low", "Low Priority"),
            new ValueAlias("medium", "Medium Priority"),
            new ValueAlias("high", "High Priority"),
            new ValueAlias("urgent", "Urgent Priority")
        );
        
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("priority")
            .description("Task priority level")
            .errorMessage("Please select a valid priority")
            .defaultValue("medium")
            .enumValues(priorities)
            .build();
        
        assertEquals("priority", constraint.getName());
        assertEquals("Task priority level", constraint.getDescription());
        assertEquals("medium", constraint.getDefaultValue());
        assertNotNull(constraint.getEnumValues());
        assertEquals(4, constraint.getEnumValues().size());
        assertEquals("low", constraint.getEnumValues().get(0).getValue());
        assertEquals("urgent", constraint.getEnumValues().get(3).getValue());
    }
    
    @Test
    @DisplayName("Should create email format constraint with builder")
    void testBuilderEmailConstraint() {
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("email")
            .description("Valid email address required")
            .format("email")
            .pattern("^[\\w\\.-]+@[\\w\\.-]+\\.[a-zA-Z]{2,}$")
            .errorMessage("Please enter a valid email address")
            .build();
        
        assertEquals("email", constraint.getName());
        assertEquals("email", constraint.getFormat());
        assertEquals("^[\\w\\.-]+@[\\w\\.-]+\\.[a-zA-Z]{2,}$", constraint.getPattern());
        assertEquals("Valid email address required", constraint.getDescription());
    }
    
    @Test
    @DisplayName("Should create date range constraint with builder")
    void testBuilderDateRangeConstraint() {
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("start_date")
            .description("Project start date")
            .format("date")
            .min("2024-01-01")
            .max("2025-12-31")
            .errorMessage("Start date must be between 2024 and 2025")
            .build();
        
        assertEquals("start_date", constraint.getName());
        assertEquals("date", constraint.getFormat());
        assertEquals("2024-01-01", constraint.getMin());
        assertEquals("2025-12-31", constraint.getMax());
        assertEquals("Project start date", constraint.getDescription());
    }
    
    @Test
    @DisplayName("Should compare builder vs traditional approach")
    void testBuilderVsTraditionalApproach() {
        // Approche traditionnelle (verbose)
        ConstraintDescriptor traditional = new ConstraintDescriptor("password");
        traditional.setDescription("Password strength validation");
        traditional.setMin(8);
        traditional.setMax(128);
        traditional.setPattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])");
        traditional.setErrorMessage("Password must contain uppercase, lowercase, number and special character");
        
        // Approche builder (fluent)
        ConstraintDescriptor builder = ConstraintDescriptor.builder("password")
            .description("Password strength validation")
            .min(8)
            .max(128)
            .pattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])")
            .errorMessage("Password must contain uppercase, lowercase, number and special character")
            .build();
        
        // Validation que les deux approches donnent le même résultat
        assertEquals(traditional.getName(), builder.getName());
        assertEquals(traditional.getDescription(), builder.getDescription());
        assertEquals(traditional.getMin(), builder.getMin());
        assertEquals(traditional.getMax(), builder.getMax());
        assertEquals(traditional.getPattern(), builder.getPattern());
        assertEquals(traditional.getErrorMessage(), builder.getErrorMessage());
        
        // Test equals
        assertTrue(traditional.equals(builder));
        assertTrue(builder.equals(traditional));
        assertEquals(traditional.hashCode(), builder.hashCode());
    }
    
    @Test
    @DisplayName("Should serialize builder-created constraint correctly")
    void testBuilderSerializationCompatibility() throws JsonProcessingException {
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("age")
            .description("Age validation")
            .min(18)
            .max(65)
            .errorMessage("Age must be between 18 and 65")
            .build();
        
        // Test sérialisation
        String json = objectMapper.writeValueAsString(constraint);
        ConstraintDescriptor deserialized = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        // Validation que la sérialisation préserve toutes les données
        assertEquals(constraint.getName(), deserialized.getName());
        assertEquals(constraint.getDescription(), deserialized.getDescription());
        assertEquals(constraint.getMin(), deserialized.getMin());
        assertEquals(constraint.getMax(), deserialized.getMax());
        assertEquals(constraint.getErrorMessage(), deserialized.getErrorMessage());
        
        // Validation JSON structure
        assertTrue(json.contains("\"name\":\"age\""));
        assertTrue(json.contains("\"min\":18"));
        assertTrue(json.contains("\"max\":65"));
        assertTrue(json.contains("\"description\":\"Age validation\""));
    }
    
    @Test
    @DisplayName("Should chain all builder methods fluently")
    void testBuilderFluentChaining() {
        // Test que tous les builders retournent this pour le chaining
        ConstraintDescriptor.Builder builder = ConstraintDescriptor.builder("test");
        
        // Test fluent chaining complet
        ConstraintDescriptor constraint = builder
            .description("Test description")
            .errorMessage("Test error")
            .defaultValue("test")
            .min(1)
            .max(10)
            .pattern("^test$")
            .format("text")
            .build();
        
        // Validation que toutes les propriétés sont définies
        assertEquals("test", constraint.getName());
        assertEquals("Test description", constraint.getDescription());
        assertEquals("Test error", constraint.getErrorMessage());
        assertEquals("test", constraint.getDefaultValue());
        assertEquals(1, constraint.getMin());
        assertEquals(10, constraint.getMax());
        assertEquals("^test$", constraint.getPattern());
        assertEquals("text", constraint.getFormat());
    }
    
    @Test
    @DisplayName("Should handle minimal constraint creation")
    void testBuilderMinimalConstraint() {
        // Contrainte minimale (juste le nom)
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("simple")
            .build();
        
        assertEquals("simple", constraint.getName());
        assertNull(constraint.getDescription());
        assertNull(constraint.getErrorMessage());
        assertNull(constraint.getMin());
        assertNull(constraint.getMax());
        assertNull(constraint.getPattern());
        assertNull(constraint.getFormat());
        assertNull(constraint.getEnumValues());
        assertNull(constraint.getValuesEndpoint());
    }
}