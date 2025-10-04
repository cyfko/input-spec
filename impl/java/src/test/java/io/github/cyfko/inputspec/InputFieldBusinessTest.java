package io.github.cyfko.inputspec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

/**
 * Tests de validation pour InputFieldSpec - Scénarios métier réels
 * Validation des workflows complets selon protocole v1.0
 */
class InputFieldBusinessTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("Should create complete user registration form (real scenario)")
    void testUserRegistrationFormScenario() throws JsonProcessingException {
        // Scénario métier : formulaire d'inscription complet
        
        // 1. Champ username avec validation
        ConstraintDescriptor usernameConstraint = new ConstraintDescriptor("username_validation");
        usernameConstraint.setMin(3);
        usernameConstraint.setMax(20);
        usernameConstraint.setPattern("^[a-zA-Z0-9_]+$");
        usernameConstraint.setErrorMessage("Username must be 3-20 characters, alphanumeric only");
        
        InputFieldSpec username = new InputFieldSpec();
        username.setDisplayName("Username");
        username.setDescription("Choose a unique username");
        username.setDataType(DataType.STRING);
        username.setRequired(true);
        username.setExpectMultipleValues(false);
        username.setConstraints(Arrays.asList(usernameConstraint));
        
        // 2. Champ email
        ConstraintDescriptor emailConstraint = new ConstraintDescriptor("email_format");
        emailConstraint.setFormat("email");
        emailConstraint.setPattern("^[\\w\\.-]+@[\\w\\.-]+\\.[a-zA-Z]{2,}$");
        emailConstraint.setErrorMessage("Please enter a valid email address");
        
        InputFieldSpec email = new InputFieldSpec();
        email.setDisplayName("Email Address");
        email.setDescription("Your email address for login");
        email.setDataType(DataType.STRING);
        email.setRequired(true);
        email.setExpectMultipleValues(false);
        email.setConstraints(Arrays.asList(emailConstraint));
        
        // 3. Champ âge avec validation numérique
        ConstraintDescriptor ageConstraint = new ConstraintDescriptor("age_range");
        ageConstraint.setMin(18);
        ageConstraint.setMax(120);
        ageConstraint.setErrorMessage("Age must be between 18 and 120");
        
        InputFieldSpec age = new InputFieldSpec();
        age.setDisplayName("Age");
        age.setDescription("Your age in years");
        age.setDataType(DataType.NUMBER);
        age.setRequired(true);
        age.setExpectMultipleValues(false);
        age.setConstraints(Arrays.asList(ageConstraint));
        
        // Test sérialisation complète du formulaire
        List<InputFieldSpec> form = Arrays.asList(username, email, age);
        
        for (InputFieldSpec field : form) {
            String json = objectMapper.writeValueAsString(field);
            InputFieldSpec deserialized = objectMapper.readValue(json, InputFieldSpec.class);
            
            // Validation structure complète
            assertNotNull(deserialized.getDisplayName());
            assertNotNull(deserialized.getDataType());
            assertTrue(deserialized.isRequired());
            assertNotNull(deserialized.getConstraints());
            assertFalse(deserialized.getConstraints().isEmpty());
            
            // Validation que tous les champs critiques sont préservés
            assertEquals(field.getDisplayName(), deserialized.getDisplayName());
            assertEquals(field.getDataType(), deserialized.getDataType());
            assertEquals(field.isRequired(), deserialized.isRequired());
        }
    }
    
    @Test
    @DisplayName("Should handle dynamic select field with values endpoint")
    void testDynamicSelectFieldScenario() throws JsonProcessingException {
        // Scénario : sélection de pays depuis une API
        ResponseMapping mapping = new ResponseMapping();
        mapping.setDataField("countries");
        
        ValuesEndpoint endpoint = new ValuesEndpoint();
        endpoint.setUri("/api/countries");
        endpoint.setResponseMapping(mapping);
        endpoint.setCacheStrategy(CacheStrategy.LONG_TERM);
        endpoint.setPaginationStrategy(PaginationStrategy.PAGE_NUMBER);
        
        ConstraintDescriptor dynamicConstraint = new ConstraintDescriptor("country_selection");
        dynamicConstraint.setValuesEndpoint(endpoint);
        dynamicConstraint.setErrorMessage("Please select a valid country");
        
        InputFieldSpec countryField = new InputFieldSpec();
        countryField.setDisplayName("Country");
        countryField.setDescription("Select your country");
        countryField.setDataType(DataType.STRING);
        countryField.setRequired(true);
        countryField.setExpectMultipleValues(false);
        countryField.setConstraints(Arrays.asList(dynamicConstraint));
        
        String json = objectMapper.writeValueAsString(countryField);
        InputFieldSpec deserialized = objectMapper.readValue(json, InputFieldSpec.class);
        
        // Validation structure complète
        assertEquals(DataType.STRING, deserialized.getDataType());
        assertTrue(deserialized.isRequired());
        
        ConstraintDescriptor constraint = deserialized.getConstraints().get(0);
        assertNotNull(constraint.getValuesEndpoint());
        assertEquals("/api/countries", constraint.getValuesEndpoint().getUri());
        assertEquals(CacheStrategy.LONG_TERM, constraint.getValuesEndpoint().getCacheStrategy());
        assertEquals(PaginationStrategy.PAGE_NUMBER, constraint.getValuesEndpoint().getPaginationStrategy());
        
        // Validation JSON structure protocole
        assertTrue(json.contains("\"valuesEndpoint\""));
        assertTrue(json.contains("\"/api/countries\""));
        assertTrue(json.contains("\"PAGE_NUMBER\""));
    }
    
    @Test
    @DisplayName("Should handle conditional field visibility (protocol workflow)")
    void testConditionalFieldWorkflow() throws JsonProcessingException {
        // Scénario : champ conditionnel "Autre" apparaît si "Autre" sélectionné
        
        // 1. Champ de sélection principal
        List<ValueAlias> jobTypes = Arrays.asList(
            new ValueAlias("developer", "Software Developer"),
            new ValueAlias("designer", "UX/UI Designer"),
            new ValueAlias("manager", "Project Manager"),
            new ValueAlias("other", "Other")
        );
        
        ConstraintDescriptor jobConstraint = new ConstraintDescriptor("job_selection");
        jobConstraint.setEnumValues(jobTypes);
        jobConstraint.setErrorMessage("Please select your job type");
        
        InputFieldSpec jobField = new InputFieldSpec();
        jobField.setDisplayName("Job Type");
        jobField.setDescription("Select your primary job role");
        jobField.setDataType(DataType.STRING);
        jobField.setRequired(true);
        jobField.setExpectMultipleValues(false);
        jobField.setConstraints(Arrays.asList(jobConstraint));
        
        // 2. Champ conditionnel "Autre"
        ConstraintDescriptor otherConstraint = new ConstraintDescriptor("other_job_text");
        otherConstraint.setMin(5);
        otherConstraint.setMax(100);
        otherConstraint.setErrorMessage("Please describe your job (5-100 characters)");
        
        InputFieldSpec otherJobField = new InputFieldSpec();
        otherJobField.setDisplayName("Other Job Type");
        otherJobField.setDescription("Please specify your job type");
        otherJobField.setDataType(DataType.STRING);
        otherJobField.setRequired(false); // Sera requis conditionnellement
        otherJobField.setExpectMultipleValues(false);
        otherJobField.setConstraints(Arrays.asList(otherConstraint));
        
        // Test que les champs peuvent être liés logiquement
        String jobJson = objectMapper.writeValueAsString(jobField);
        String otherJson = objectMapper.writeValueAsString(otherJobField);
        
        InputFieldSpec jobDeserialized = objectMapper.readValue(jobJson, InputFieldSpec.class);
        InputFieldSpec otherDeserialized = objectMapper.readValue(otherJson, InputFieldSpec.class);
        
        // Validation que le workflow peut être reconstruit
        assertEquals("Job Type", jobDeserialized.getDisplayName());
        assertEquals("Other Job Type", otherDeserialized.getDisplayName());
        assertTrue(jobDeserialized.isRequired());
        assertFalse(otherDeserialized.isRequired()); // Conditionnel
        
        // Validation que les valeurs "other" existent
        boolean hasOtherOption = jobDeserialized.getConstraints().get(0).getEnumValues().stream()
            .anyMatch(alias -> "other".equals(alias.getValue()));
        assertTrue(hasOtherOption);
    }
    
    @Test
    @DisplayName("Should validate complex data type scenarios")
    void testComplexDataTypeScenarios() throws JsonProcessingException {
        // Test tous les types de données avec contraintes appropriées
        
        // DATE avec format
        InputFieldSpec dateField = new InputFieldSpec();
        dateField.setDisplayName("Birth Date");
        dateField.setDataType(DataType.DATE);
        dateField.setExpectMultipleValues(false);
        ConstraintDescriptor dateConstraint = new ConstraintDescriptor("date_format");
        dateConstraint.setFormat("date");
        dateConstraint.setErrorMessage("Please enter a valid date");
        dateField.setConstraints(Arrays.asList(dateConstraint));
        
        // NUMBER avec precision
        InputFieldSpec priceField = new InputFieldSpec();
        priceField.setDisplayName("Price");
        priceField.setDataType(DataType.NUMBER);
        priceField.setExpectMultipleValues(false);
        ConstraintDescriptor priceConstraint = new ConstraintDescriptor("price_range");
        priceConstraint.setMin(0);
        priceConstraint.setMax(999999);
        priceConstraint.setErrorMessage("Price must be between 0 and 999,999");
        priceField.setConstraints(Arrays.asList(priceConstraint));
        
        // BOOLEAN avec valeur par défaut
        InputFieldSpec newsletterField = new InputFieldSpec();
        newsletterField.setDisplayName("Newsletter");
        newsletterField.setDataType(DataType.BOOLEAN);
        newsletterField.setExpectMultipleValues(false);
        ConstraintDescriptor newsletterConstraint = new ConstraintDescriptor("newsletter_default");
        newsletterConstraint.setDefaultValue("false");
        newsletterField.setConstraints(Arrays.asList(newsletterConstraint));
        
        List<InputFieldSpec> complexFields = Arrays.asList(dateField, priceField, newsletterField);
        
        for (InputFieldSpec field : complexFields) {
            String json = objectMapper.writeValueAsString(field);
            InputFieldSpec deserialized = objectMapper.readValue(json, InputFieldSpec.class);
            
            assertNotNull(deserialized.getDisplayName());
            assertNotNull(deserialized.getDataType());
            assertNotNull(deserialized.getConstraints());
            assertFalse(deserialized.getConstraints().isEmpty());
            
            // Validation que tous les champs critiques sont préservés
            assertEquals(field.getDisplayName(), deserialized.getDisplayName());
            assertEquals(field.getDataType(), deserialized.getDataType());
            
            // Validation spécifique par type
            switch (deserialized.getDataType()) {
                case DATE:
                    assertEquals("date", deserialized.getConstraints().get(0).getFormat());
                    break;
                case NUMBER:
                    assertEquals(0, deserialized.getConstraints().get(0).getMin());
                    assertEquals(999999, deserialized.getConstraints().get(0).getMax());
                    break;
                case BOOLEAN:
                    assertEquals("false", deserialized.getConstraints().get(0).getDefaultValue());
                    break;
            }
        }
    }
    
    @Test
    @DisplayName("Should handle field validation chain (protocol logic)")
    void testFieldValidationChain() throws JsonProcessingException {
        // Test validation en chaîne selon protocole
        
        InputFieldSpec field = new InputFieldSpec();
        field.setDisplayName("Password");
        field.setDataType(DataType.STRING);
        field.setRequired(true);
        field.setExpectMultipleValues(false);
        
        // Contraintes multiples dans l'ordre logique
        ConstraintDescriptor lengthConstraint = new ConstraintDescriptor("length");
        lengthConstraint.setMin(8);
        lengthConstraint.setMax(128);
        lengthConstraint.setErrorMessage("Password must be 8-128 characters");
        
        ConstraintDescriptor strengthConstraint = new ConstraintDescriptor("strength");
        strengthConstraint.setPattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])");
        strengthConstraint.setErrorMessage("Password must contain uppercase, lowercase, number and special character");
        
        ConstraintDescriptor commonConstraint = new ConstraintDescriptor("common_passwords");
        commonConstraint.setPattern("^(?!.*(?:password|123456|qwerty)).*$");
        commonConstraint.setErrorMessage("Password too common, please choose a different one");
        
        // Ordre important : longueur -> force -> unicité
        field.setConstraints(Arrays.asList(lengthConstraint, strengthConstraint, commonConstraint));
        
        String json = objectMapper.writeValueAsString(field);
        InputFieldSpec deserialized = objectMapper.readValue(json, InputFieldSpec.class);
        
        // Validation que l'ordre est préservé
        assertEquals(3, deserialized.getConstraints().size());
        assertEquals("length", deserialized.getConstraints().get(0).getName());
        assertEquals("strength", deserialized.getConstraints().get(1).getName());
        assertEquals("common_passwords", deserialized.getConstraints().get(2).getName());
        
        // Validation que chaque contrainte a sa logique
        assertTrue(((Number)deserialized.getConstraints().get(0).getMin()).intValue() > 0);
        assertNotNull(deserialized.getConstraints().get(1).getPattern());
        assertNotNull(deserialized.getConstraints().get(2).getPattern());
    }
    
    @Test
    @DisplayName("Should validate protocol error handling")
    void testProtocolErrorHandling() {
        // Test validation stricte selon protocole
        
        assertThrows(IllegalArgumentException.class, () -> {
            new ConstraintDescriptor(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new ConstraintDescriptor("");
        });
        
        // Test qu'un champ valide fonctionne
        assertDoesNotThrow(() -> {
            InputFieldSpec field = new InputFieldSpec();
            field.setDisplayName("Test Field");
            field.setDataType(DataType.STRING);
            field.setRequired(true);
        });
    }
}