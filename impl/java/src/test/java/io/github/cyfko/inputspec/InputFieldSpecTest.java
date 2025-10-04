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
        
        InputFieldSpec usernameField = new InputFieldSpec(
            "Username",
            DataType.STRING,
            false, // expectMultipleValues
            true,  // required
            Arrays.asList(valueConstraint)
        );
        usernameField.setDescription("User's unique identifier");
        
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
        
        InputFieldSpec priceField = new InputFieldSpec(
            "Price",
            DataType.NUMBER,
            false, // expectMultipleValues
            true,  // required
            Arrays.asList(valueConstraint)
        );
        priceField.setDescription("Price filter range");
        
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
        
        InputFieldSpec emailField = new InputFieldSpec(
            "Email Address",
            DataType.STRING,
            false, // expectMultipleValues
            true,  // required
            Arrays.asList(valueConstraint)
        );
        emailField.setDescription("Contact email address");
        
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
        
        InputFieldSpec statusField = new InputFieldSpec(
            "Status",
            DataType.STRING,
            false, // expectMultipleValues
            true,  // required
            Arrays.asList(valueConstraint)
        );
        statusField.setDescription("Filter by status");
        
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
        ResponseMapping responseMapping = new ResponseMapping("tags");
        
        RequestParams requestParams = new RequestParams();
        requestParams.setSearchParam("q");
        
        ValuesEndpoint valuesEndpoint = new ValuesEndpoint("/api/tags", responseMapping);
        valuesEndpoint.setSearchField("name");
        valuesEndpoint.setPaginationStrategy(PaginationStrategy.NONE);
        valuesEndpoint.setCacheStrategy(CacheStrategy.LONG_TERM);
        valuesEndpoint.setDebounceMs(200);
        valuesEndpoint.setMinSearchLength(1);
        valuesEndpoint.setRequestParams(requestParams);
        
        ConstraintDescriptor valueConstraint = ConstraintDescriptor.builder("value")
            .min(1)
            .max(5)
            .description("Select 1 to 5 relevant tags")
            .errorMessage("You must select between 1 and 5 tags")
            .valuesEndpoint(valuesEndpoint)
            .build();
        
        InputFieldSpec tagsField = new InputFieldSpec(
            "Tags",
            DataType.STRING,
            true, // expectMultipleValues = true for multi-select
            true, // required
            Arrays.asList(valueConstraint)
        );
        tagsField.setDescription("Select relevant tags for content");
        
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
        
        InputFieldSpec dateField = new InputFieldSpec(
            "Created Date",
            DataType.DATE,
            false, // expectMultipleValues
            false, // not required
            Arrays.asList(valueConstraint)
        );
        dateField.setDescription("Filter by creation date");
        
        // Verify field properties
        assertEquals("Created Date", dateField.getDisplayName());
        assertEquals(DataType.DATE, dateField.getDataType());
        assertFalse(dateField.isRequired()); // Optional field
        assertEquals("iso8601", dateField.getConstraints().get(0).getFormat());
    }
}