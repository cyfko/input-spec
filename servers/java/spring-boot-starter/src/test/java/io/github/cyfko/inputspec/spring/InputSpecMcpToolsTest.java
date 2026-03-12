package io.github.cyfko.inputspec.spring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.cache.BundleResolver;
import io.github.cyfko.inputspec.cache.FormSpecCache;
import io.github.cyfko.inputspec.model.FormSpecModel;
import io.github.cyfko.inputspec.spring.ai.InputSpecMcpTools;
import io.github.cyfko.inputspec.spring.bootstrap.FormHandlerRegistry;
import io.github.cyfko.inputspec.spring.ai.McpSubmitResult;
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationError;
import io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InputSpecMcpTools}.
 */
@ExtendWith(MockitoExtension.class)
class InputSpecMcpToolsTest {

    @Mock FormSpecCache       cache;
    @Mock FormSpecValidator   validator;
    @Mock
    FormHandlerRegistry registry;
    @Mock BundleResolver      bundleResolver;

    InputSpecMcpTools tools;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tools = new InputSpecMcpTools(cache, validator, registry, bundleResolver);
    }

    // ─── Tool annotations ────────────────────────────────────────────────────

    @Nested
    @DisplayName("@Tool annotation presence")
    class ToolAnnotationTests {

        @Test
        @DisplayName("All 4 MCP tool methods are annotated with @Tool")
        void allToolMethodsAreAnnotated() throws Exception {
            String[] expectedTools = {
                "inputspec_list_forms",
                "inputspec_get_form",
                "inputspec_validate_form",
                "inputspec_submit_form"
            };

            for (String methodName : expectedTools) {
                boolean found = false;
                for (Method method : InputSpecMcpTools.class.getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        found = true;
                        assertTrue(
                            method.isAnnotationPresent(
                                org.springframework.ai.tool.annotation.Tool.class),
                            methodName + " should be annotated with @Tool"
                        );
                    }
                }
                assertTrue(found, "Method " + methodName + " should exist");
            }
        }
    }

    // ─── inputspec_list_forms ────────────────────────────────────────────────

    @Nested
    @DisplayName("inputspec_list_forms")
    class ListFormsTests {

        @Test
        @DisplayName("Returns empty list when no forms are registered")
        void emptyWhenNoForms() {
            when(registry.registeredFormIds()).thenReturn(Set.of());

            List<InputSpecMcpTools.FormInfo> result = tools.inputspec_list_forms();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns form summaries sorted by id")
        void returnsSortedSummaries() {
            when(registry.registeredFormIds()).thenReturn(Set.of("z-form", "a-form"));

            JsonNode nameA = MAPPER.valueToTree("Form A");
            JsonNode nameZ = MAPPER.valueToTree("Form Z");

            FormSpecModel specA = new FormSpecModel("a-form", "2.1", nameA, null, List.of(), List.of(), null);
            FormSpecModel specZ = new FormSpecModel("z-form", "2.1", nameZ, null, List.of(), List.of(), null);

            when(cache.get("a-form")).thenReturn(Optional.of(specA));
            when(cache.get("z-form")).thenReturn(Optional.of(specZ));
            when(bundleResolver.resolve(any(JsonNode.class), anyString(), isNull()))
                .thenAnswer(inv -> ((JsonNode) inv.getArgument(0)).asText());

            List<InputSpecMcpTools.FormInfo> result = tools.inputspec_list_forms();

            assertEquals(2, result.size());
            assertEquals("a-form", result.get(0).id());
            assertEquals("z-form", result.get(1).id());
        }
    }

    // ─── inputspec_get_form ──────────────────────────────────────────────────

    @Nested
    @DisplayName("inputspec_get_form")
    class GetFormTests {

        @Test
        @DisplayName("Returns form spec for valid id")
        void returnsSpec() {
            FormSpecModel spec = new FormSpecModel("test-form", "2.1", null, null, List.of(), List.of(), null);
            when(cache.get("test-form")).thenReturn(Optional.of(spec));

            FormSpecModel result = tools.inputspec_get_form("test-form", null);

            assertEquals("test-form", result.id());
        }

        @Test
        @DisplayName("Throws for unknown form id")
        void throwsForUnknown() {
            when(cache.get("unknown")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                () -> tools.inputspec_get_form("unknown", null));
        }
    }

    // ─── inputspec_validate_form ──────────────────────────────────────────────

    @Nested
    @DisplayName("inputspec_validate_form")
    class ValidateFormTests {

        @Test
        @DisplayName("Returns valid result for correct data")
        void validData() {
            FormSpecModel spec = new FormSpecModel("test-form", "2.1", null, null, List.of(), List.of(), null);
            when(cache.get("test-form")).thenReturn(Optional.of(spec));
            when(validator.validateForm(eq(spec), anyMap())).thenReturn(
                new ValidationResult(true, List.of()));

            ValidationResult result = tools.inputspec_validate_form("test-form", Map.of("name", "John"));

            assertTrue(result.isValid());
            assertTrue(result.errors().isEmpty());
        }

        @Test
        @DisplayName("Returns errors for invalid data")
        void invalidData() {
            FormSpecModel spec = new FormSpecModel("test-form", "2.1", null, null, List.of(), List.of(), null);
            when(cache.get("test-form")).thenReturn(Optional.of(spec));

            ValidationError error = ValidationError.field("name", "required", "Name is required", null);
            when(validator.validateForm(eq(spec), anyMap())).thenReturn(
                new ValidationResult(false, List.of(error)));

            ValidationResult result = tools.inputspec_validate_form("test-form", Map.of());

            assertFalse(result.isValid());
            assertEquals(1, result.errors().size());
            assertEquals("name", result.errors().get(0).path());
        }
    }

    // ─── inputspec_submit_form ───────────────────────────────────────────────

    @Nested
    @DisplayName("inputspec_submit_form")
    class SubmitFormTests {

        @Test
        @DisplayName("Returns validation_failed when data is invalid")
        void validationFailed() {
            FormSpecModel spec = new FormSpecModel("test-form", "2.1", null, null, List.of(), List.of(), null);
            when(cache.get("test-form")).thenReturn(Optional.of(spec));

            ValidationError error = ValidationError.field("email", "pattern", "Invalid email", "bad");
            when(validator.validateForm(eq(spec), anyMap())).thenReturn(
                new ValidationResult(false, List.of(error)));

            McpSubmitResult result = tools.inputspec_submit_form("test-form", Map.of("email", "bad"));

            assertEquals("validation_failed", result.status());
            assertEquals(1, result.errors().size());
        }

        @Test
        @DisplayName("Returns accepted when handler accepts")
        void accepted() {
            FormSpecModel spec = new FormSpecModel("test-form", "2.1", null, null, List.of(), List.of(), null);
            when(cache.get("test-form")).thenReturn(Optional.of(spec));
            when(validator.validateForm(eq(spec), anyMap())).thenReturn(
                new ValidationResult(true, List.of()));

            FormHandlerRegistry.ResolvedHandler handler = mock(FormHandlerRegistry.ResolvedHandler.class);
            when(registry.find("test-form")).thenReturn(Optional.of(handler));
            when(handler.invoke(any(), anyMap())).thenReturn(SubmitResponse.ok(Map.of("id", 42)));

            McpSubmitResult result = tools.inputspec_submit_form("test-form", Map.of("name", "John"));

            assertEquals("accepted", result.status());
            assertNotNull(result.body());
        }

        @Test
        @DisplayName("Returns rejected when handler rejects")
        void rejected() {
            FormSpecModel spec = new FormSpecModel("test-form", "2.1", null, null, List.of(), List.of(), null);
            when(cache.get("test-form")).thenReturn(Optional.of(spec));
            when(validator.validateForm(eq(spec), anyMap())).thenReturn(
                new ValidationResult(true, List.of()));

            FormHandlerRegistry.ResolvedHandler handler = mock(FormHandlerRegistry.ResolvedHandler.class);
            when(registry.find("test-form")).thenReturn(Optional.of(handler));
            ValidationError error = ValidationError.field("email", "unique", "Already taken", "x@x.com");
            when(handler.invoke(any(), anyMap())).thenReturn(SubmitResponse.rejected(List.of(error)));

            McpSubmitResult result = tools.inputspec_submit_form("test-form", Map.of("email", "x@x.com"));

            assertEquals("rejected", result.status());
            assertEquals(1, result.errors().size());
        }
    }

    // ─── McpSubmitResult factory methods ─────────────────────────────────────

    @Nested
    @DisplayName("McpSubmitResult factory methods")
    class McpSubmitResultTests {

        @Test
        @DisplayName("accepted() creates correct result")
        void acceptedNoBody() {
            McpSubmitResult r = McpSubmitResult.accepted();
            assertEquals("accepted", r.status());
            assertNull(r.body());
            assertTrue(r.errors().isEmpty());
        }

        @Test
        @DisplayName("accepted(body) creates result with body")
        void acceptedWithBody() {
            McpSubmitResult r = McpSubmitResult.accepted(Map.of("id", 1));
            assertEquals("accepted", r.status());
            assertNotNull(r.body());
        }

        @Test
        @DisplayName("rejected(errors) creates correct result")
        void rejected() {
            ValidationError e = ValidationError.field("x", "y", "z", null);
            McpSubmitResult r = McpSubmitResult.rejected(List.of(e));
            assertEquals("rejected", r.status());
            assertEquals(1, r.errors().size());
        }

        @Test
        @DisplayName("validationFailed(errors) creates correct result")
        void validationFailed() {
            ValidationError e = ValidationError.field("x", "y", "z", null);
            McpSubmitResult r = McpSubmitResult.validationFailed(List.of(e));
            assertEquals("validation_failed", r.status());
            assertEquals(1, r.errors().size());
        }
    }
}
