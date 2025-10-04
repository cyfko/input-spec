package io.github.cyfko.inputspec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

/**
 * Tests métier robustes pour ConstraintDescriptor - Validation protocole v1.0
 * Tests des scénarios réels d'utilisation et logique de validation
 */
class ConstraintDescriptorBusinessTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("Should validate username constraint scenario (real business case)")
    void testUsernameConstraintScenario() throws JsonProcessingException {
        // Scénario réel : contrainte username pour un système d'inscription
        // Utilisation du builder pattern pour plus de lisibilité
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("username")
            .min(3)              // Min 3 caractères
            .max(20)             // Max 20 caractères  
            .pattern("^[a-zA-Z0-9_]+$") // Alphanumeric + underscore seulement
            .description("Username must be 3-20 characters, alphanumeric and underscore only")
            .errorMessage("Username invalid: use 3-20 characters, letters, numbers, and underscore only")
            .build();
        
        // Test sérialisation complète
        String json = objectMapper.writeValueAsString(constraint);
        ConstraintDescriptor deserialized = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        // Validation des propriétés critiques
        assertEquals("username", deserialized.getName());
        assertEquals(3, deserialized.getMin());
        assertEquals(20, deserialized.getMax());
        assertEquals("^[a-zA-Z0-9_]+$", deserialized.getPattern());
        assertNotNull(deserialized.getDescription());
        assertNotNull(deserialized.getErrorMessage());
        
        // Validation que le JSON contient tous les champs requis par le protocole
        assertTrue(json.contains("\"name\":\"username\""));
        assertTrue(json.contains("\"min\":3"));
        assertTrue(json.contains("\"max\":20"));
        assertTrue(json.contains("\"pattern\":"));
    }
    
    @Test
    @DisplayName("Should handle email validation constraint with format (protocol compliance)")
    void testEmailConstraintWithFormat() throws JsonProcessingException {
        ConstraintDescriptor constraint = new ConstraintDescriptor("email");
        constraint.setFormat("email");
        constraint.setPattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        constraint.setDescription("Valid email address required");
        constraint.setErrorMessage("Please enter a valid email address");
        
        String json = objectMapper.writeValueAsString(constraint);
        ConstraintDescriptor deserialized = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        assertEquals("email", deserialized.getFormat());
        assertEquals("email", deserialized.getName());
        assertNotNull(deserialized.getPattern());
        
        // Vérifier que format et pattern peuvent coexister (protocole v1.0)
        assertTrue(json.contains("\"format\":\"email\""));
        assertTrue(json.contains("\"pattern\":"));
    }
    
    @Test
    @DisplayName("Should handle numeric range constraint (min/max semantics)")
    void testNumericRangeConstraint() throws JsonProcessingException {
        // Scénario : âge entre 18 et 65 ans
        ConstraintDescriptor constraint = new ConstraintDescriptor("age");
        constraint.setMin(18);
        constraint.setMax(65);
        constraint.setDescription("Age must be between 18 and 65");
        constraint.setErrorMessage("Age must be between 18 and 65 years");
        
        String json = objectMapper.writeValueAsString(constraint);
        ConstraintDescriptor deserialized = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        assertEquals(18, deserialized.getMin());
        assertEquals(65, deserialized.getMax());
        assertEquals("age", deserialized.getName());
        
        // Validation sémantique : min <= max avec casting
        assertTrue(((Number)deserialized.getMin()).intValue() <= ((Number)deserialized.getMax()).intValue());
    }
    
    @Test
    @DisplayName("Should handle values endpoint constraint (dynamic values)")
    void testValuesEndpointConstraint() throws JsonProcessingException {
        // Scénario réel : sélection d'utilisateurs depuis une API
        ResponseMapping mapping = new ResponseMapping();
        mapping.setDataField("users");
        
        ValuesEndpoint endpoint = new ValuesEndpoint();
        endpoint.setUri("/api/users");
        endpoint.setResponseMapping(mapping);
        endpoint.setCacheStrategy(CacheStrategy.SHORT_TERM);
        endpoint.setPaginationStrategy(PaginationStrategy.PAGE_NUMBER);
        
        ConstraintDescriptor constraint = new ConstraintDescriptor("assignee");
        constraint.setValuesEndpoint(endpoint);
        constraint.setDescription("Select user to assign task");
        constraint.setErrorMessage("Please select a valid user");
        
        String json = objectMapper.writeValueAsString(constraint);
        ConstraintDescriptor deserialized = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        assertNotNull(deserialized.getValuesEndpoint());
        assertEquals("/api/users", deserialized.getValuesEndpoint().getUri());
        assertEquals(CacheStrategy.SHORT_TERM, deserialized.getValuesEndpoint().getCacheStrategy());
        assertEquals(PaginationStrategy.PAGE_NUMBER, deserialized.getValuesEndpoint().getPaginationStrategy());
        
        // Validation structure complexe dans JSON
        assertTrue(json.contains("\"valuesEndpoint\""));
        assertTrue(json.contains("\"/api/users\""));
        assertTrue(json.contains("\"SHORT_TERM\""));
    }
    
    @Test
    @DisplayName("Should handle enum values constraint (static list)")
    void testEnumValuesConstraint() throws JsonProcessingException {
        // Scénario : statut de tâche avec valeurs prédéfinies
        List<ValueAlias> enumValues = Arrays.asList(
            new ValueAlias("todo", "To Do"),
            new ValueAlias("in_progress", "In Progress"),
            new ValueAlias("done", "Done"),
            new ValueAlias("cancelled", "Cancelled")
        );
        
        ConstraintDescriptor constraint = new ConstraintDescriptor("status");
        constraint.setEnumValues(enumValues);
        constraint.setDescription("Task status selection");
        constraint.setErrorMessage("Please select a valid status");
        
        String json = objectMapper.writeValueAsString(constraint);
        ConstraintDescriptor deserialized = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        assertNotNull(deserialized.getEnumValues());
        assertEquals(4, deserialized.getEnumValues().size());
        assertEquals("todo", deserialized.getEnumValues().get(0).getValue());
        assertEquals("To Do", deserialized.getEnumValues().get(0).getLabel());
        assertEquals("done", deserialized.getEnumValues().get(2).getValue());
        
        // Validation que toutes les valeurs sont présentes
        assertTrue(json.contains("\"enumValues\""));
        assertTrue(json.contains("\"todo\""));
        assertTrue(json.contains("\"To Do\""));
        assertTrue(json.contains("\"in_progress\""));
    }
    
    @Test
    @DisplayName("Should handle multiple constraints combination (protocol ordering)")
    void testMultipleConstraintsCombination() throws JsonProcessingException {
        // Scénario complexe : mot de passe avec plusieurs contraintes
        ConstraintDescriptor lengthConstraint = new ConstraintDescriptor("length");
        lengthConstraint.setMin(8);
        lengthConstraint.setMax(128);
        lengthConstraint.setDescription("Password length");
        lengthConstraint.setErrorMessage("Password must be 8-128 characters");
        
        ConstraintDescriptor complexityConstraint = new ConstraintDescriptor("complexity");
        complexityConstraint.setPattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]");
        complexityConstraint.setDescription("Password complexity");
        complexityConstraint.setErrorMessage("Password must contain uppercase, lowercase, number and special character");
        
        // Test que chaque contrainte garde son identité
        assertNotEquals(lengthConstraint.getName(), complexityConstraint.getName());
        
        // Test sérialisation indépendante
        String json1 = objectMapper.writeValueAsString(lengthConstraint);
        String json2 = objectMapper.writeValueAsString(complexityConstraint);
        
        assertTrue(json1.contains("\"length\""));
        assertTrue(json1.contains("\"min\":8"));
        // Le pattern field sera null donc apparaîtra comme "pattern":null
        assertTrue(json1.contains("\"pattern\":null"));
        
        assertTrue(json2.contains("\"complexity\""));
        assertTrue(json2.contains("\"pattern\""));
        assertTrue(json2.contains("\"min\":null"));
    }
    
    @Test
    @DisplayName("Should validate constraint protocol compliance (strict validation)")
    void testProtocolCompliance() throws JsonProcessingException {
        ConstraintDescriptor constraint = new ConstraintDescriptor("test");
        
        // Test qu'un nom vide/null lance une exception (validation métier)
        assertThrows(IllegalArgumentException.class, () -> {
            new ConstraintDescriptor(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new ConstraintDescriptor("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new ConstraintDescriptor("   ");
        });
        
        // Test qu'un constraint valide fonctionne
        assertDoesNotThrow(() -> {
            new ConstraintDescriptor("valid_name");
        });
    }
    
    @Test
    @DisplayName("Should handle constraint inheritance and composition")
    void testConstraintComposition() throws JsonProcessingException {
        // Scénario avancé : contrainte avec valeurs par défaut
        ConstraintDescriptor constraint = ConstraintDescriptor.builder("priority")
            .defaultValue("medium")
            .enumValues(Arrays.asList(
                new ValueAlias("low", "Low Priority"),
                new ValueAlias("medium", "Medium Priority"),
                new ValueAlias("high", "High Priority")
            ))
            .description("Task priority level")
            .build();
        
        String json = objectMapper.writeValueAsString(constraint);
        ConstraintDescriptor deserialized = objectMapper.readValue(json, ConstraintDescriptor.class);
        
        assertEquals("medium", deserialized.getDefaultValue());
        assertNotNull(deserialized.getEnumValues());
        assertEquals(3, deserialized.getEnumValues().size());
        
        // Validation que la valeur par défaut existe dans les enum values
        boolean defaultExists = deserialized.getEnumValues().stream()
            .anyMatch(alias -> "medium".equals(alias.getValue()));
        assertTrue(defaultExists, "Default value should exist in enum values");
    }
    
    @Test
    @DisplayName("Should demonstrate builder pattern benefits vs traditional approach")
    void testBuilderPatternBenefits() throws JsonProcessingException {
        // Démonstration des avantages du builder pattern
        
        // AVANT (approche traditionnelle) - verbose et peu lisible
        ConstraintDescriptor traditionalApproach = new ConstraintDescriptor("complex_validation");
        traditionalApproach.setDescription("Complex multi-criteria validation");
        traditionalApproach.setMin(5);
        traditionalApproach.setMax(50);
        traditionalApproach.setPattern("^[A-Z][a-zA-Z0-9_-]*$");
        traditionalApproach.setFormat("custom");
        traditionalApproach.setErrorMessage("Field must start with uppercase, 5-50 chars, alphanumeric with dash/underscore");
        
        // APRÈS (builder pattern) - fluent et lisible
        ConstraintDescriptor builderApproach = ConstraintDescriptor.builder("complex_validation")
            .description("Complex multi-criteria validation")
            .min(5)
            .max(50)
            .pattern("^[A-Z][a-zA-Z0-9_-]*$")
            .format("custom")
            .errorMessage("Field must start with uppercase, 5-50 chars, alphanumeric with dash/underscore")
            .build();
        
        // Validation que les deux approches produisent le même résultat
        assertEquals(traditionalApproach.getName(), builderApproach.getName());
        assertEquals(traditionalApproach.getDescription(), builderApproach.getDescription());
        assertEquals(traditionalApproach.getMin(), builderApproach.getMin());
        assertEquals(traditionalApproach.getMax(), builderApproach.getMax());
        assertEquals(traditionalApproach.getPattern(), builderApproach.getPattern());
        assertEquals(traditionalApproach.getFormat(), builderApproach.getFormat());
        assertEquals(traditionalApproach.getErrorMessage(), builderApproach.getErrorMessage());
        
        // Test que l'équivalence fonctionne
        assertEquals(traditionalApproach, builderApproach);
        
        // Test sérialisation identique
        String json1 = objectMapper.writeValueAsString(traditionalApproach);
        String json2 = objectMapper.writeValueAsString(builderApproach);
        assertEquals(json1, json2);
    }
}