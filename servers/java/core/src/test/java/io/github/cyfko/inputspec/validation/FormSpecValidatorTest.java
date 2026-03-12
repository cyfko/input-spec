package io.github.cyfko.inputspec.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cyfko.inputspec.model.*;
import io.github.cyfko.inputspec.protocol.*;
import io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationError;
import io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationResult;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DIFSP FormSpecValidator.
 *
 * Tests are grouped by fix:
 * - Fix 1: FormSpecModel.version field
 * - Fix 2: ValidationError protocol alignment (§2.8 / §2.10)
 * - Fix 3: Custom constraint handler registry
 */
class FormSpecValidatorTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private FormSpecValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FormSpecValidator();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FIX 1 — FormSpecModel.version
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Fix1: FormSpecModel deserializes version field")
    void formSpecModel_parsesVersionField() throws Exception {
        String json = """
            {
              "id": "test-form",
              "version": "2.1.0",
              "displayName": "Test Form",
              "fields": [],
              "crossConstraints": []
            }
            """;
        FormSpecModel model = mapper.readValue(json, FormSpecModel.class);
        assertEquals("2.1.0", model.version());
        assertEquals("test-form", model.id());
    }

    @Test
    @DisplayName("Fix1: FormSpecModel works without version (optional)")
    void formSpecModel_worksWithoutVersion() throws Exception {
        String json = """
            {
              "id": "test-form",
              "fields": []
            }
            """;
        FormSpecModel model = mapper.readValue(json, FormSpecModel.class);
        assertNull(model.version());
        assertEquals("test-form", model.id());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FIX 2 — ValidationError protocol alignment
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Fix2: Field error contains value (§2.8)")
    void fieldError_containsRejectedValue() {
        InputFieldSpec spec = buildStringField("username", true, 3, null);
        List<ValidationError> errors = validator.validateField(spec, "ab", "username");

        assertFalse(errors.isEmpty());
        ValidationError err = errors.get(0);
        assertEquals("size-min", err.constraintName());
        assertEquals("ab", err.value(), "Rejected value should be captured per §2.8");
        assertNotNull(err.message());
        assertNull(err.crossConstraintName(), "Field errors must not have crossConstraintName");
        assertNull(err.fields(), "Field errors must not have fields");
    }

    @Test
    @DisplayName("Fix2: Required error contains null value (§2.8)")
    void requiredError_containsNullValue() {
        InputFieldSpec spec = buildStringField("email", true, null, null);
        List<ValidationError> errors = validator.validateField(spec, null, "email");

        assertEquals(1, errors.size());
        ValidationError err = errors.get(0);
        assertEquals("required", err.constraintName());
        assertNull(err.value(), "Value for required violation should be null");
    }

    @Test
    @DisplayName("Fix2: Multi-value error contains index (§2.8)")
    void multiValueError_containsIndex() {
        InputFieldSpec spec = buildMultiStringField("tags", true, 3, null);
        List<ValidationError> errors = validator.validateField(spec,
            List.of("hello", "ab"), "tags");

        // Should have error for "ab" at index 1
        Optional<ValidationError> elemErr = errors.stream()
            .filter(e -> "size-min".equals(e.constraintName()) && e.index() != null)
            .findFirst();
        assertTrue(elemErr.isPresent(), "Should have a per-element error with index");
        assertEquals(1, elemErr.get().index());
    }

    @Test
    @DisplayName("Fix2: Cross-constraint error uses 'fields' (§2.10)")
    void crossConstraintError_usesFields() throws Exception {
        String formJson = """
            {
              "id": "test-form",
              "fields": [
                { "name": "startDate", "displayName": "Start", "dataType": "STRING",
                  "expectMultipleValues": false, "required": true, "constraints": [] },
                { "name": "endDate", "displayName": "End", "dataType": "STRING",
                  "expectMultipleValues": false, "required": true, "constraints": [] }
              ],
              "crossConstraints": [
                {
                  "name": "dateRange",
                  "type": "fieldComparison",
                  "fields": ["endDate", "startDate"],
                  "params": { "operator": "gt" },
                  "errorMessage": "End must be after start"
                }
              ]
            }
            """;
        FormSpecModel form = mapper.readValue(formJson, FormSpecModel.class);

        // endDate < startDate → should fail
        Map<String, Object> values = Map.of("startDate", "100", "endDate", "50");
        ValidationResult result = validator.validateForm(form, values);

        assertFalse(result.isValid());
        ValidationError err = result.errors().get(0);
        assertEquals("dateRange", err.crossConstraintName());
        assertNotNull(err.fields(), "Cross errors must have 'fields' per §2.10");
        assertEquals(List.of("endDate", "startDate"), err.fields());
        assertNull(err.constraintName(), "Cross errors must not have constraintName");
    }

    @Test
    @DisplayName("Fix2: ValidationError JSON omits null fields (@JsonInclude NON_NULL)")
    void validationError_omitsNullFields() throws Exception {
        // Field error: no crossConstraintName, no fields, no index
        ValidationError fieldErr = ValidationError.field("email", "required",
            "This field is required", null);
        JsonNode json = mapper.valueToTree(fieldErr);

        assertTrue(json.has("constraintName"));
        assertTrue(json.has("message"));
        assertFalse(json.has("crossConstraintName"),
            "Null crossConstraintName should be omitted");
        assertFalse(json.has("fields"),
            "Null fields should be omitted");
        assertFalse(json.has("index"),
            "Null index should be omitted");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FIX 3 — Custom constraint handler registry
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Fix3: Registered custom handler is invoked and error propagated")
    void customHandler_invokedAndErrorPropagated() throws Exception {
        // Register a handler that rejects codes < 5 chars
        validator.registerCustomHandler("promoCode", (value, params) -> {
            String code = value.toString();
            int minLen = params.path("minLength").asInt(5);
            return code.length() >= minLen
                ? Optional.empty()
                : Optional.of("Promo code must be at least " + minLen + " characters");
        });

        String fieldJson = """
            {
              "name": "code",
              "displayName": "Code",
              "dataType": "STRING",
              "expectMultipleValues": false,
              "required": true,
              "constraints": [
                {
                  "name": "promoCheck",
                  "type": "custom",
                  "params": { "key": "promoCode", "minLength": 5 }
                }
              ]
            }
            """;
        InputFieldSpec spec = mapper.readValue(fieldJson, InputFieldSpec.class);

        // Too short → should fail
        List<ValidationError> errors = validator.validateField(spec, "AB", "code");
        assertEquals(1, errors.size());
        assertEquals("promoCheck", errors.get(0).constraintName());
        assertTrue(errors.get(0).message().contains("at least 5"));
        assertEquals("AB", errors.get(0).value());

        // Long enough → should pass
        errors = validator.validateField(spec, "ABCDEF", "code");
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Execution Phase 1: Standard constraints prevent CUSTOM constraints from running")
    void phase1_preventsPhase2() throws Exception {
        // Register a custom handler that always fails (to prove it never ran)
        validator.registerCustomHandler("alwaysFail", (value, params) -> 
            Optional.of("This should not be reached!"));

        // Field requires minLength=5 (Standard) AND custom "alwaysFail"
        String fieldJson = """
            {
              "name": "code",
              "displayName": "Code",
              "dataType": "STRING",
              "expectMultipleValues": false,
              "required": true,
              "constraints": [
                {
                  "name": "lenCheck",
                  "type": "minLength",
                  "params": { "value": 5 }
                },
                {
                  "name": "customCheck",
                  "type": "custom",
                  "params": { "key": "alwaysFail" }
                }
              ]
            }
            """;
        InputFieldSpec spec = mapper.readValue(fieldJson, InputFieldSpec.class);

        // Value is 2 chars (fails standard minLength)
        List<ValidationError> errors = validator.validateField(spec, "AB", "code");
        
        // We should ONLY see the standard error, not the custom one, because Phase 1 failed fast
        assertEquals(1, errors.size());
        assertEquals("lenCheck", errors.get(0).constraintName());
    }

    @Test
    @DisplayName("Execution Phase 3: Global validators only run if Fields are flawless")
    void phase3_onlyRunsIfFieldsPass() throws Exception {
        // Form with one standard field (required)
        String formJson = """
            {
              "id": "global-test-form",
              "fields": [
                { "name": "email", "dataType": "STRING", "required": true, "constraints": [] }
              ]
            }
            """;
        FormSpecModel form = mapper.readValue(formJson, FormSpecModel.class);

        // Register a global handler that always fails
        validator.registerGlobalFormHandler("global-test-form", values -> 
            Map.of("global", "This global error should not be reached!"));

        // 1. Missing required field -> fails Phase 1
        ValidationResult result1 = validator.validateForm(form, Map.of());
        assertFalse(result1.isValid());
        assertEquals(1, result1.errors().size());
        assertEquals("required", result1.errors().get(0).constraintName());

        // 2. Flawless field -> proceeds to Phase 3 (Global Validation fails)
        ValidationResult result2 = validator.validateForm(form, Map.of("email", "test@test.com"));
        assertFalse(result2.isValid());
        assertEquals(1, result2.errors().size());
        assertEquals("global", result2.errors().get(0).constraintName());
        assertEquals("This global error should not be reached!", result2.errors().get(0).message());
    }

    @Test
    @DisplayName("Fix3: Unregistered custom handler is silently tolerated")
    void customHandler_unregisteredIsTolerated() throws Exception {
        String fieldJson = """
            {
              "name": "code",
              "displayName": "Code",
              "dataType": "STRING",
              "expectMultipleValues": false,
              "required": false,
              "constraints": [
                {
                  "name": "unknownCheck",
                  "type": "custom",
                  "params": { "key": "nonExistentHandler" }
                }
              ]
            }
            """;
        InputFieldSpec spec = mapper.readValue(fieldJson, InputFieldSpec.class);
        List<ValidationError> errors = validator.validateField(spec, "anything", "code");
        assertTrue(errors.isEmpty(), "Unregistered custom key must be silently tolerated");
    }

    @Test
    @DisplayName("Fix3: Custom cross-constraint handler is invoked")
    void customCrossHandler_invokedAndErrorPropagated() throws Exception {
        // Register a cross-constraint handler that checks sum <= max
        validator.registerCustomCrossHandler("sumCheck", (fieldValues, params) -> {
            int maxSum = params.path("maxSum").asInt(100);
            int sum = fieldValues.values().stream()
                .filter(v -> v instanceof Number)
                .mapToInt(v -> ((Number) v).intValue())
                .sum();
            return sum <= maxSum
                ? Optional.empty()
                : Optional.of("Sum of fields must not exceed " + maxSum);
        });

        String formJson = """
            {
              "id": "test-form",
              "fields": [
                { "name": "a", "displayName": "A", "dataType": "NUMBER",
                  "expectMultipleValues": false, "required": true, "constraints": [] },
                { "name": "b", "displayName": "B", "dataType": "NUMBER",
                  "expectMultipleValues": false, "required": true, "constraints": [] }
              ],
              "crossConstraints": [
                {
                  "name": "totalLimit",
                  "type": "custom",
                  "fields": ["a", "b"],
                  "params": { "key": "sumCheck", "maxSum": 10 }
                }
              ]
            }
            """;
        FormSpecModel form = mapper.readValue(formJson, FormSpecModel.class);

        // Sum = 12 > 10 → should fail
        Map<String, Object> values = Map.of("a", 7, "b", 5);
        ValidationResult result = validator.validateForm(form, values);
        assertFalse(result.isValid());
        assertEquals("totalLimit", result.errors().get(0).crossConstraintName());
        assertTrue(result.errors().get(0).message().contains("must not exceed 10"));

        // Sum = 8 <= 10 → should pass
        values = Map.of("a", 3, "b", 5);
        result = validator.validateForm(form, values);
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Fix3: Unregistered custom cross-handler is tolerated")
    void customCrossHandler_unregisteredIsTolerated() throws Exception {
        String formJson = """
            {
              "id": "test-form",
              "fields": [
                { "name": "x", "displayName": "X", "dataType": "STRING",
                  "expectMultipleValues": false, "required": true, "constraints": [] }
              ],
              "crossConstraints": [
                {
                  "name": "mystery",
                  "type": "custom",
                  "fields": ["x"],
                  "params": { "key": "nonExistent" }
                }
              ]
            }
            """;
        FormSpecModel form = mapper.readValue(formJson, FormSpecModel.class);
        Map<String, Object> values = Map.of("x", "hello");
        ValidationResult result = validator.validateForm(form, values);
        assertTrue(result.isValid(), "Unregistered custom cross-handler must be tolerated");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EXISTING PIPELINE — regression checks
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Regression: minLength/maxLength validation still works")
    void regression_minMaxLengthValidation() {
        InputFieldSpec spec = buildStringField("name", true, 3, 20);
        // Too short
        assertFalse(validator.validateField(spec, "ab", "name").isEmpty());
        // Valid
        assertTrue(validator.validateField(spec, "Alice", "name").isEmpty());
    }

    @Test
    @DisplayName("Regression: INLINE CLOSED membership validation")
    void regression_inlineClosedMembership() throws Exception {
        String fieldJson = """
            {
              "name": "status",
              "displayName": "Status",
              "dataType": "STRING",
              "expectMultipleValues": false,
              "required": true,
              "valuesEndpoint": {
                "protocol": "INLINE",
                "mode": "CLOSED",
                "items": [
                  { "value": "ACTIVE", "label": "Active" },
                  { "value": "INACTIVE", "label": "Inactive" }
                ]
              },
              "constraints": []
            }
            """;
        InputFieldSpec spec = mapper.readValue(fieldJson, InputFieldSpec.class);

        assertTrue(validator.validateField(spec, "ACTIVE", "status").isEmpty());
        assertFalse(validator.validateField(spec, "UNKNOWN", "status").isEmpty());
    }

    @Test
    @DisplayName("Regression: OBJECT recursion with subFields")
    void regression_objectRecursion() throws Exception {
        String fieldJson = """
            {
              "name": "address",
              "displayName": "Address",
              "dataType": "OBJECT",
              "expectMultipleValues": false,
              "required": true,
              "subFields": [
                {
                  "name": "city",
                  "displayName": "City",
                  "dataType": "STRING",
                  "expectMultipleValues": false,
                  "required": true,
                  "constraints": []
                }
              ],
              "constraints": []
            }
            """;
        InputFieldSpec spec = mapper.readValue(fieldJson, InputFieldSpec.class);

        // Missing required sub-field city
        Map<String, Object> addr = Map.of();
        List<ValidationError> errors = validator.validateField(spec, addr, "address");
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> "required".equals(e.constraintName())));
    }

    @Test
    @DisplayName("Regression: unknown constraint type tolerated (forward-compat)")
    void regression_unknownConstraintTolerated() throws Exception {
        String fieldJson = """
            {
              "name": "x",
              "displayName": "X",
              "dataType": "STRING",
              "expectMultipleValues": false,
              "required": false,
              "constraints": [
                {
                  "name": "futureType",
                  "type": "someNewType",
                  "params": {}
                }
              ]
            }
            """;
        InputFieldSpec spec = mapper.readValue(fieldJson, InputFieldSpec.class);
        List<ValidationError> errors = validator.validateField(spec, "value", "x");
        assertTrue(errors.isEmpty(), "Unknown constraint types must be silently tolerated");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helpers — build specs programmatically
    // ═══════════════════════════════════════════════════════════════════════════

    private InputFieldSpec buildStringField(String name, boolean required,
                                            Integer minLen, Integer maxLen) {
        List<ConstraintDescriptor> constraints = new ArrayList<>();
        if (minLen != null) {
            constraints.add(new ConstraintDescriptor("size-min", ConstraintType.MIN_LENGTH,
                paramsNode("value", minLen), null, null));
        }
        if (maxLen != null) {
            constraints.add(new ConstraintDescriptor("size-max", ConstraintType.MAX_LENGTH,
                paramsNode("value", maxLen), null, null));
        }
        return new InputFieldSpec(name, textNode(name), null,
            DataType.STRING, false, required, null, null, null, constraints);
    }

    private InputFieldSpec buildMultiStringField(String name, boolean required,
                                                  Integer minLen, Integer maxLen) {
        List<ConstraintDescriptor> constraints = new ArrayList<>();
        if (minLen != null) {
            constraints.add(new ConstraintDescriptor("size-min", ConstraintType.MIN_LENGTH,
                paramsNode("value", minLen), null, null));
        }
        if (maxLen != null) {
            constraints.add(new ConstraintDescriptor("size-max", ConstraintType.MAX_LENGTH,
                paramsNode("value", maxLen), null, null));
        }
        return new InputFieldSpec(name, textNode(name), null,
            DataType.STRING, true, required, null, null, null, constraints);
    }

    private JsonNode textNode(String text) {
        return mapper.getNodeFactory().textNode(text);
    }

    private JsonNode paramsNode(String key, int value) {
        ObjectNode node = mapper.createObjectNode();
        node.put(key, value);
        return node;
    }
}
