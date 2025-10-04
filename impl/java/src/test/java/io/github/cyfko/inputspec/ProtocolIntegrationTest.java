package io.github.cyfko.inputspec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

/**
 * Tests d'intégration protocole - Validation complète de scénarios complexes
 * Tests qui valident vraiment la conformité au protocole v1.0
 */
class ProtocolIntegrationTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("Complete user profile form integration test")
    void testCompleteUserProfileFormIntegration() throws JsonProcessingException {
        // Scénario intégration complète : formulaire de profil utilisateur
        
        // 1. Champ nom complet avec validation complexe
        ConstraintDescriptor nameLength = ConstraintDescriptor.builder("name_length")
            .min(2)
            .max(50)
            .errorMessage("Name must be 2-50 characters")
            .build();
        
        ConstraintDescriptor namePattern = ConstraintDescriptor.builder("name_pattern")
            .pattern("^[a-zA-ZÀ-ÿ\\s'-]+$")
            .errorMessage("Name can only contain letters, spaces, apostrophes and hyphens")
            .build();
        
        InputFieldSpec fullNameField = new InputFieldSpec();
        fullNameField.setDisplayName("Full Name");
        fullNameField.setDescription("Enter your full name");
        fullNameField.setDataType(DataType.STRING);
        fullNameField.setRequired(true);
        fullNameField.setExpectMultipleValues(false);
        fullNameField.setConstraints(Arrays.asList(nameLength, namePattern));
        
        // 2. Champ compétences avec valeurs multiples
        ConstraintDescriptor skillsConstraint = new ConstraintDescriptor("skills_selection");
        skillsConstraint.setMin(1); // Au moins 1 compétence
        skillsConstraint.setMax(10); // Maximum 10 compétences
        skillsConstraint.setEnumValues(Arrays.asList(
            new ValueAlias("java", "Java Programming"),
            new ValueAlias("python", "Python Programming"),
            new ValueAlias("javascript", "JavaScript"),
            new ValueAlias("react", "React"),
            new ValueAlias("angular", "Angular"),
            new ValueAlias("vue", "Vue.js"),
            new ValueAlias("nodejs", "Node.js"),
            new ValueAlias("docker", "Docker"),
            new ValueAlias("kubernetes", "Kubernetes"),
            new ValueAlias("aws", "Amazon Web Services")
        ));
        skillsConstraint.setErrorMessage("Please select 1-10 skills");
        
        InputFieldSpec skillsField = new InputFieldSpec();
        skillsField.setDisplayName("Technical Skills");
        skillsField.setDescription("Select your technical skills");
        skillsField.setDataType(DataType.STRING);
        skillsField.setRequired(true);
        skillsField.setExpectMultipleValues(true); // Valeurs multiples !
        skillsField.setConstraints(Arrays.asList(skillsConstraint));
        
        // 3. Champ localisation avec endpoint dynamique
        ResponseMapping locationMapping = new ResponseMapping();
        locationMapping.setDataField("cities");
        
        ValuesEndpoint locationEndpoint = new ValuesEndpoint();
        locationEndpoint.setUri("/api/locations");
        locationEndpoint.setResponseMapping(locationMapping);
        locationEndpoint.setCacheStrategy(CacheStrategy.SHORT_TERM);
        locationEndpoint.setPaginationStrategy(PaginationStrategy.PAGE_NUMBER);
        
        ConstraintDescriptor locationConstraint = new ConstraintDescriptor("location_selection");
        locationConstraint.setValuesEndpoint(locationEndpoint);
        locationConstraint.setErrorMessage("Please select a valid location");
        
        InputFieldSpec locationField = new InputFieldSpec();
        locationField.setDisplayName("Location");
        locationField.setDescription("Select your current location");
        locationField.setDataType(DataType.STRING);
        locationField.setRequired(true);
        locationField.setExpectMultipleValues(false);
        locationField.setConstraints(Arrays.asList(locationConstraint));
        
        // Test sérialisation/désérialisation complète
        List<InputFieldSpec> profileForm = Arrays.asList(fullNameField, skillsField, locationField);
        
        // Validation chaque champ individuellement
        for (InputFieldSpec field : profileForm) {
            String json = objectMapper.writeValueAsString(field);
            InputFieldSpec deserialized = objectMapper.readValue(json, InputFieldSpec.class);
            
            // Validation structure protocole
            assertNotNull(deserialized.getDisplayName());
            assertNotNull(deserialized.getDataType());
            assertNotNull(deserialized.getConstraints());
            assertFalse(deserialized.getConstraints().isEmpty());
            
            // Validation préservation des données
            assertEquals(field.getDisplayName(), deserialized.getDisplayName());
            assertEquals(field.getDataType(), deserialized.getDataType());
            assertEquals(field.isRequired(), deserialized.isRequired());
            assertEquals(field.isExpectMultipleValues(), deserialized.isExpectMultipleValues());
            assertEquals(field.getConstraints().size(), deserialized.getConstraints().size());
        }
        
        // Test spécifique : validation valeurs multiples
        assertTrue(skillsField.isExpectMultipleValues());
        assertFalse(fullNameField.isExpectMultipleValues());
        assertFalse(locationField.isExpectMultipleValues());
        
        // Test spécifique : validation contraintes multiples sur nom
        assertEquals(2, fullNameField.getConstraints().size());
        assertEquals("name_length", fullNameField.getConstraints().get(0).getName());
        assertEquals("name_pattern", fullNameField.getConstraints().get(1).getName());
        
        // Test spécifique : validation endpoint dynamique
        assertNotNull(locationField.getConstraints().get(0).getValuesEndpoint());
        assertEquals("/api/locations", locationField.getConstraints().get(0).getValuesEndpoint().getUri());
    }
    
    @Test
    @DisplayName("Complex e-commerce product form integration")
    void testECommerceProductFormIntegration() throws JsonProcessingException {
        // Test intégration e-commerce avec logique métier complexe
        
        // 1. Prix avec validation métier
        ConstraintDescriptor priceRange = new ConstraintDescriptor("price_validation");
        priceRange.setMin(0.01);
        priceRange.setMax(999999.99);
        priceRange.setFormat("currency");
        priceRange.setErrorMessage("Price must be between $0.01 and $999,999.99");
        
        InputFieldSpec priceField = new InputFieldSpec();
        priceField.setDisplayName("Product Price");
        priceField.setDescription("Set the product price in USD");
        priceField.setDataType(DataType.NUMBER);
        priceField.setRequired(true);
        priceField.setExpectMultipleValues(false);
        priceField.setConstraints(Arrays.asList(priceRange));
        
        // 2. Catégories avec hiérarchie
        ConstraintDescriptor categoryConstraint = new ConstraintDescriptor("category_hierarchy");
        categoryConstraint.setEnumValues(Arrays.asList(
            new ValueAlias("electronics", "Electronics"),
            new ValueAlias("electronics.computers", "Electronics > Computers"),
            new ValueAlias("electronics.phones", "Electronics > Phones"),
            new ValueAlias("clothing", "Clothing"),
            new ValueAlias("clothing.men", "Clothing > Men"),
            new ValueAlias("clothing.women", "Clothing > Women"),
            new ValueAlias("books", "Books"),
            new ValueAlias("books.fiction", "Books > Fiction"),
            new ValueAlias("books.nonfiction", "Books > Non-Fiction")
        ));
        categoryConstraint.setErrorMessage("Please select a valid category");
        
        InputFieldSpec categoryField = new InputFieldSpec();
        categoryField.setDisplayName("Product Category");
        categoryField.setDescription("Choose the product category");
        categoryField.setDataType(DataType.STRING);
        categoryField.setRequired(true);
        categoryField.setExpectMultipleValues(false);
        categoryField.setConstraints(Arrays.asList(categoryConstraint));
        
        // 3. Tags avec validation de nombre et format
        ConstraintDescriptor tagsCount = new ConstraintDescriptor("tags_count");
        tagsCount.setMin(1);
        tagsCount.setMax(5);
        tagsCount.setErrorMessage("Please provide 1-5 tags");
        
        ConstraintDescriptor tagFormat = new ConstraintDescriptor("tag_format");
        tagFormat.setPattern("^[a-z0-9-]+$");
        tagFormat.setErrorMessage("Tags must be lowercase, alphanumeric with hyphens only");
        
        InputFieldSpec tagsField = new InputFieldSpec();
        tagsField.setDisplayName("Product Tags");
        tagsField.setDescription("Add relevant tags (1-5 tags, lowercase-with-hyphens)");
        tagsField.setDataType(DataType.STRING);
        tagsField.setRequired(true);
        tagsField.setExpectMultipleValues(true);
        tagsField.setConstraints(Arrays.asList(tagsCount, tagFormat));
        
        // 4. Date de disponibilité avec contrainte temporelle
        ConstraintDescriptor availabilityDate = new ConstraintDescriptor("availability_date");
        availabilityDate.setFormat("date");
        availabilityDate.setMin("2024-01-01"); // Date minimum
        availabilityDate.setErrorMessage("Availability date must be January 1, 2024 or later");
        
        InputFieldSpec availabilityField = new InputFieldSpec();
        availabilityField.setDisplayName("Availability Date");
        availabilityField.setDescription("When will this product be available?");
        availabilityField.setDataType(DataType.DATE);
        availabilityField.setRequired(true);
        availabilityField.setExpectMultipleValues(false);
        availabilityField.setConstraints(Arrays.asList(availabilityDate));
        
        // Test intégration complète
        List<InputFieldSpec> productForm = Arrays.asList(priceField, categoryField, tagsField, availabilityField);
        
        for (InputFieldSpec field : productForm) {
            String json = objectMapper.writeValueAsString(field);
            InputFieldSpec deserialized = objectMapper.readValue(json, InputFieldSpec.class);
            
            // Tests spécifiques par champ
            switch (field.getDisplayName()) {
                case "Product Price":
                    assertEquals(DataType.NUMBER, deserialized.getDataType());
                    assertFalse(deserialized.isExpectMultipleValues());
                    assertEquals("currency", deserialized.getConstraints().get(0).getFormat());
                    assertEquals(0.01, deserialized.getConstraints().get(0).getMin());
                    break;
                    
                case "Product Category":
                    assertEquals(DataType.STRING, deserialized.getDataType());
                    assertFalse(deserialized.isExpectMultipleValues());
                    assertEquals(9, deserialized.getConstraints().get(0).getEnumValues().size());
                    // Validation hiérarchie
                    assertTrue(deserialized.getConstraints().get(0).getEnumValues().stream()
                        .anyMatch(v -> "electronics.computers".equals(v.getValue())));
                    break;
                    
                case "Product Tags":
                    assertEquals(DataType.STRING, deserialized.getDataType());
                    assertTrue(deserialized.isExpectMultipleValues()); // Multiple !
                    assertEquals(2, deserialized.getConstraints().size()); // Count + format
                    assertEquals(1, deserialized.getConstraints().get(0).getMin());
                    assertEquals(5, deserialized.getConstraints().get(0).getMax());
                    assertNotNull(deserialized.getConstraints().get(1).getPattern());
                    break;
                    
                case "Availability Date":
                    assertEquals(DataType.DATE, deserialized.getDataType());
                    assertFalse(deserialized.isExpectMultipleValues());
                    assertEquals("date", deserialized.getConstraints().get(0).getFormat());
                    assertEquals("2024-01-01", deserialized.getConstraints().get(0).getMin());
                    break;
            }
        }
    }
    
    @Test
    @DisplayName("Protocol compliance validation test")
    void testProtocolComplianceValidation() throws JsonProcessingException {
        // Test validation stricte de conformité protocole v1.0
        
        // Test champ avec toutes les propriétés protocole
        ConstraintDescriptor fullConstraint = new ConstraintDescriptor("full_validation");
        fullConstraint.setDescription("Complete constraint validation");
        fullConstraint.setErrorMessage("This constraint validates all protocol features");
        fullConstraint.setDefaultValue("default");
        fullConstraint.setMin(1);
        fullConstraint.setMax(100);
        fullConstraint.setPattern("^[a-zA-Z0-9]+$");
        fullConstraint.setFormat("text");
        fullConstraint.setEnumValues(Arrays.asList(
            new ValueAlias("option1", "Option 1"),
            new ValueAlias("option2", "Option 2")
        ));
        
        // Endpoint avec toutes les propriétés
        ResponseMapping completeMapping = new ResponseMapping();
        completeMapping.setDataField("data");
        
        ValuesEndpoint completeEndpoint = new ValuesEndpoint();
        completeEndpoint.setUri("/api/complete");
        completeEndpoint.setResponseMapping(completeMapping);
        completeEndpoint.setCacheStrategy(CacheStrategy.SHORT_TERM);
        completeEndpoint.setPaginationStrategy(PaginationStrategy.PAGE_NUMBER);
        
        ConstraintDescriptor endpointConstraint = new ConstraintDescriptor("endpoint_validation");
        endpointConstraint.setValuesEndpoint(completeEndpoint);
        
        InputFieldSpec completeField = new InputFieldSpec();
        completeField.setDisplayName("Complete Protocol Field");
        completeField.setDescription("Field testing all protocol features");
        completeField.setDataType(DataType.STRING);
        completeField.setRequired(true);
        completeField.setExpectMultipleValues(true);
        completeField.setConstraints(Arrays.asList(fullConstraint, endpointConstraint));
        
        // Sérialisation/désérialisation complète
        String json = objectMapper.writeValueAsString(completeField);
        InputFieldSpec deserialized = objectMapper.readValue(json, InputFieldSpec.class);
        
        // Validation protocole complète
        assertEquals("Complete Protocol Field", deserialized.getDisplayName());
        assertEquals("Field testing all protocol features", deserialized.getDescription());
        assertEquals(DataType.STRING, deserialized.getDataType());
        assertTrue(deserialized.isRequired());
        assertTrue(deserialized.isExpectMultipleValues());
        assertEquals(2, deserialized.getConstraints().size());
        
        // Validation première contrainte (complète)
        ConstraintDescriptor firstConstraint = deserialized.getConstraints().get(0);
        assertEquals("full_validation", firstConstraint.getName());
        assertEquals("Complete constraint validation", firstConstraint.getDescription());
        assertEquals("This constraint validates all protocol features", firstConstraint.getErrorMessage());
        assertEquals("default", firstConstraint.getDefaultValue());
        assertEquals(1, firstConstraint.getMin());
        assertEquals(100, firstConstraint.getMax());
        assertEquals("^[a-zA-Z0-9]+$", firstConstraint.getPattern());
        assertEquals("text", firstConstraint.getFormat());
        assertEquals(2, firstConstraint.getEnumValues().size());
        
        // Validation deuxième contrainte (endpoint)
        ConstraintDescriptor secondConstraint = deserialized.getConstraints().get(1);
        assertEquals("endpoint_validation", secondConstraint.getName());
        assertNotNull(secondConstraint.getValuesEndpoint());
        assertEquals("/api/complete", secondConstraint.getValuesEndpoint().getUri());
        assertEquals(CacheStrategy.SHORT_TERM, secondConstraint.getValuesEndpoint().getCacheStrategy());
        assertEquals(PaginationStrategy.PAGE_NUMBER, secondConstraint.getValuesEndpoint().getPaginationStrategy());
        
        // Validation JSON contient tous les champs protocole
        assertTrue(json.contains("\"displayName\""));
        assertTrue(json.contains("\"description\""));
        assertTrue(json.contains("\"dataType\""));
        assertTrue(json.contains("\"expectMultipleValues\""));
        assertTrue(json.contains("\"required\""));
        assertTrue(json.contains("\"constraints\""));
        assertTrue(json.contains("\"valuesEndpoint\""));
        assertTrue(json.contains("\"enumValues\""));
        assertTrue(json.contains("\"responseMapping\""));
        assertTrue(json.contains("\"cacheStrategy\""));
        assertTrue(json.contains("\"paginationStrategy\""));
    }
}