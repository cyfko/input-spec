package io.github.cyfko.inputspec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for InputFieldSpec based on examples from the protocol specification
 */
class InputFieldSpecTest {
    
    @Test
    @DisplayName("Should create simple text input field from protocol example 1")
    void testSimpleTextInput() {
        // Example 1: Simple Text Input from protocol
        ConstraintDescriptor valueConstraint = ConstraintDescriptor.builder("value")
            .min(3)
            .max(20)
            .pattern("^[a-zA-Z0-9_]+$")
            .description("Username (3-20 alphanumeric characters)")
            .errorMessage("Username must be 3-20 characters, alphanumeric with underscores")
            .build();
        
        InputFieldSpec usernameField = InputFieldSpec.builder("Username", DataType.STRING)
            .description("User's unique identifier")
            .expectMultipleValues(false)
            .required(true)
            .constraints(Arrays.asList(valueConstraint))
            .build();
        
        // Verify field properties
        assertEquals("Username", usernameField.getDisplayName());
        assertEquals("User's unique identifier", usernameField.getDescription());
        assertEquals(DataType.STRING, usernameField.getDataType());
        assertFalse(usernameField.isExpectMultipleValues());
        assertTrue(usernameField.isRequired());
        assertEquals(1, usernameField.getConstraints().size());
        
        // Verify constraint
        ConstraintDescriptor constraint = usernameField.getConstraints().get(0);
        assertEquals("value", constraint.getName());
        assertEquals(3, constraint.getMin());
        assertEquals(20, constraint.getMax());
        assertEquals("^[a-zA-Z0-9_]+$", constraint.getPattern());
    }
    
    @Test
    @DisplayName("Should create numeric range input from protocol example 2")
    void testNumericRangeInput() {
        // Example 2: Numeric Range Input from protocol
        ConstraintDescriptor valueConstraint = ConstraintDescriptor.builder("value")
            .min(0)
            .description("Price value")
            .errorMessage("Price must be greater than 0")
            .defaultValue(0)
            .build();
        
        InputFieldSpec priceField = InputFieldSpec.builder("Price", DataType.NUMBER)
            .description("Price filter range")
            .expectMultipleValues(false)
            .required(true)
            .constraints(Arrays.asList(valueConstraint))
            .build();
        
        // Verify field properties
        assertEquals("Price", priceField.getDisplayName());
        assertEquals(DataType.NUMBER, priceField.getDataType());
        assertEquals(0, priceField.getConstraints().get(0).getMin());
        assertEquals(0, priceField.getConstraints().get(0).getDefaultValue());
    }
    
    @Test
    @DisplayName("Should create email input with pattern from protocol example 3")
    void testEmailInput() {
        // Example 3: Email Input with Pattern from protocol
        ConstraintDescriptor valueConstraint = ConstraintDescriptor.builder("value")
            .pattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
            .format("email")
            .description("Valid email address")
            .errorMessage("Please provide a valid email address")
            .build();
        
        InputFieldSpec emailField = InputFieldSpec.builder("Email Address", DataType.STRING)
            .description("Contact email address")
            .expectMultipleValues(false)
            .required(true)
            .constraints(Arrays.asList(valueConstraint))
            .build();
        
        // Verify field properties
        assertEquals("Email Address", emailField.getDisplayName());
        assertEquals("email", emailField.getConstraints().get(0).getFormat());
        assertNotNull(emailField.getConstraints().get(0).getPattern());
    }
    
    @Test
    @DisplayName("Should create static select field from protocol example 4")
    void testStaticSelectField() {
        // Example 4: Static Select Field from protocol
        List<ValueAlias> enumValues = Arrays.asList(
            new ValueAlias("active", "Active"),
            new ValueAlias("inactive", "Inactive"),
            new ValueAlias("pending", "Pending")
        );
        
        ConstraintDescriptor valueConstraint = ConstraintDescriptor.builder("value")
            .description("Item status")
            .errorMessage("Please select a status")
            .enumValues(enumValues)
            .build();
        
        InputFieldSpec statusField = InputFieldSpec.builder("Status", DataType.STRING)
            .description("Filter by status")
            .expectMultipleValues(false)
            .required(true)
            .constraints(Arrays.asList(valueConstraint))
            .build();
        
        // Verify field properties
        assertEquals("Status", statusField.getDisplayName());
        assertEquals(3, statusField.getConstraints().get(0).getEnumValues().size());
        assertEquals("active", statusField.getConstraints().get(0).getEnumValues().get(0).getValue());
        assertEquals("Active", statusField.getConstraints().get(0).getEnumValues().get(0).getLabel());
    }
    
    @Test
    @DisplayName("Should create multi-select field from protocol example 6")
    void testMultiSelectField() {
        // Example 6: Multi-Select Tags with Search from protocol
        ResponseMapping responseMapping = ResponseMapping.builder("tags").build();
        
        RequestParams requestParams = new RequestParams();
        requestParams.setSearchParam("q");
        
        ValuesEndpoint valuesEndpoint = ValuesEndpoint.builder("/api/tags", responseMapping)
            .searchField("name")
            .paginationStrategy(PaginationStrategy.NONE)
            .cacheStrategy(CacheStrategy.LONG_TERM)
            .debounceMs(200)
            .minSearchLength(1)
            .requestParams(requestParams)
            .build();
        
        ConstraintDescriptor valueConstraint = ConstraintDescriptor.builder("value")
            .min(1)
            .max(5)
            .description("Select 1 to 5 relevant tags")
            .errorMessage("You must select between 1 and 5 tags")
            .valuesEndpoint(valuesEndpoint)
            .build();
        
        InputFieldSpec tagsField = InputFieldSpec.builder("Tags", DataType.STRING)
            .description("Select relevant tags for content")
            .expectMultipleValues(true)
            .required(true)
            .constraints(Arrays.asList(valueConstraint))
            .build();
        
        // Verify field properties
        assertEquals("Tags", tagsField.getDisplayName());
        assertTrue(tagsField.isExpectMultipleValues()); // Multi-select
        assertEquals(1, tagsField.getConstraints().get(0).getMin());
        assertEquals(5, tagsField.getConstraints().get(0).getMax());
        assertNotNull(tagsField.getConstraints().get(0).getValuesEndpoint());
        assertEquals("/api/tags", tagsField.getConstraints().get(0).getValuesEndpoint().getUri());
    }
    
    @Test
    @DisplayName("Should create date input from protocol example 7")
    void testDateInput() {
        // Example 7: Date Range Input from protocol
        ConstraintDescriptor valueConstraint = ConstraintDescriptor.builder("value")
            .format("iso8601")
            .description("Creation date")
            .errorMessage("Please provide a valid date")
            .build();
        
        InputFieldSpec dateField = InputFieldSpec.builder("Created Date", DataType.DATE)
            .description("Filter by creation date")
            .expectMultipleValues(false)
            .required(false)
            .constraints(Arrays.asList(valueConstraint))
            .build();
        
        // Verify field properties
        assertEquals("Created Date", dateField.getDisplayName());
        assertEquals(DataType.DATE, dateField.getDataType());
        assertFalse(dateField.isRequired()); // Optional field
        assertEquals("iso8601", dateField.getConstraints().get(0).getFormat());
    }
}