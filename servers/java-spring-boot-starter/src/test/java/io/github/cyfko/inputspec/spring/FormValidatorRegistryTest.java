package io.github.cyfko.inputspec.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import io.github.cyfko.inputspec.validation.FormValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormValidatorRegistryTest {

    private ApplicationContext context;
    private FormSpecValidator validator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        context = mock(ApplicationContext.class);
        validator = new FormSpecValidator();
        mapper = new ObjectMapper();
    }

    @Test
    void shouldRegisterConstraintHandler_whenReturnIsOptionalType() {
        Map<String, Object> beans = new HashMap<>();
        beans.put("testService", new TestServiceConstraintHandler());
        when(context.getBeansOfType(Object.class)).thenReturn(beans);

        FormValidatorRegistry registry = new FormValidatorRegistry(context, validator, mapper);
        
        // Should not throw, inherently registering the constraint
        registry.afterSingletonsInstantiated();
    }

    @Test
    void shouldRegisterGlobalHandler_whenReturnIsMapType() {
        Map<String, Object> beans = new HashMap<>();
        beans.put("testService", new TestServiceGlobalHandler());
        when(context.getBeansOfType(Object.class)).thenReturn(beans);

        FormValidatorRegistry registry = new FormValidatorRegistry(context, validator, mapper);
        
        // Should not throw, inherently registering the global form handler
        registry.afterSingletonsInstantiated();
    }

    @Test
    void shouldThrow_whenSignatureHasWrongParamCount() {
        Map<String, Object> beans = new HashMap<>();
        beans.put("invalidService", new InvalidServiceWrongParams());
        when(context.getBeansOfType(Object.class)).thenReturn(beans);

        FormValidatorRegistry registry = new FormValidatorRegistry(context, validator, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
            .isInstanceOf(FormValidatorRegistry.FormValidatorConfigurationException.class)
            .hasMessageContaining("must accept exactly ONE parameter");
    }

    @Test
    void shouldThrow_whenSignatureReturnsWrongType() {
        Map<String, Object> beans = new HashMap<>();
        beans.put("invalidService", new InvalidServiceWrongReturn());
        when(context.getBeansOfType(Object.class)).thenReturn(beans);

        FormValidatorRegistry registry = new FormValidatorRegistry(context, validator, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
            .isInstanceOf(FormValidatorRegistry.FormValidatorConfigurationException.class)
            .hasMessageContaining("Must return Optional<String> (for custom keys) or Map<String, String>");
    }

    // --- Mock Classes ---

    static class TestServiceConstraintHandler {
        @FormValidator("checkUniqueValue")
        public Optional<String> validateConstraint(SampleForm form) {
            return Optional.empty();
        }
    }

    static class TestServiceGlobalHandler {
        @FormValidator("sample-form")
        public Map<String, String> validateEntireForm(SampleForm form) {
            return new HashMap<>();
        }
    }

    static class InvalidServiceWrongParams {
        @FormValidator("broken")
        public Optional<String> handle(SampleForm a, String b) {
            return Optional.empty();
        }
    }

    static class InvalidServiceWrongReturn {
        @FormValidator("broken2")
        public String handle(SampleForm a) {
            return "error";
        }
    }

    @FormSpec(id = "sample")
    static class SampleForm {
        private String field1;
        public String getField1() { return field1; }
        public void setField1(String field1) { this.field1 = field1; }
    }
}
