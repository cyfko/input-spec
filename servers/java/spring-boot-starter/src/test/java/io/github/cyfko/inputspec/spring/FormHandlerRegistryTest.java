package io.github.cyfko.inputspec.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.cache.FormSpecCache;
import io.github.cyfko.inputspec.model.FormSpecModel;
import io.github.cyfko.inputspec.spring.bootstrap.FormHandlerRegistry;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FormHandlerRegistry.
 * FormSpecCache and ApplicationContext are mocked — no Spring context needed.
 */
@DisplayName("FormHandlerRegistry")
class FormHandlerRegistryTest {

    private FormSpecCache      cache;
    private ApplicationContext ctx;
    private ObjectMapper       mapper;

    @BeforeEach
    void setUp() {
        cache  = mock(FormSpecCache.class);
        ctx    = mock(ApplicationContext.class);
        mapper = new ObjectMapper();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private FormHandlerRegistry registryWith(Object... beans) {
        Map<String, Object> beanMap = new java.util.LinkedHashMap<>();
        for (int i = 0; i < beans.length; i++) {
            beanMap.put("bean" + i, beans[i]);
        }
        when(ctx.getBeansOfType(Object.class)).thenReturn(beanMap);
        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);
        registry.afterSingletonsInstantiated();
        return registry;
    }

    private void mockFormExists(String... ids) {
        java.util.Set<String> idSet = new java.util.HashSet<>();
        for (String id : ids) {
            when(cache.get(id)).thenReturn(Optional.of(dummyForm(id)));
            idSet.add(id);
        }
        when(cache.knownFormIds()).thenReturn(idSet);
    }

    private FormSpecModel dummyForm(String id) {
        return new FormSpecModel(id, "2.1", null, null,
            java.util.List.of(), java.util.List.of(), null);
    }

    // ─── Sample form + handler ────────────────────────────────────────────────

    @FormSpec(id = "sample-form")
    record SampleForm(String name) {}

    static class GoodService {
        @FormHandler
        public SubmitResponse handle(SampleForm form) {
            return SubmitResponse.ok(form);
        }
    }

    // ─── Happy path ──────────────────────────────────────────────────────────

    @Test @DisplayName("Valid handler → registered and findable")
    void valid_handler_registered() {
        mockFormExists("sample-form");
        var registry = registryWith(new GoodService());
        assertThat(registry.find("sample-form")).isPresent();
    }

    @Test @DisplayName("registeredFormIds() returns all registered ids")
    void registeredFormIds() {
        mockFormExists("sample-form");
        var registry = registryWith(new GoodService());
        assertThat(registry.registeredFormIds()).containsExactly("sample-form");
    }

    @Test @DisplayName("find() unknown id → empty")
    void find_unknown_returns_empty() {
        mockFormExists("sample-form");
        var registry = registryWith(new GoodService());
        assertThat(registry.find("unknown")).isEmpty();
    }

    // ─── ResolvedHandler.invoke() ─────────────────────────────────────────────

    @Test @DisplayName("invoke() deserializes map → form object → calls handler")
    void invoke_deserializes_and_calls() {
        mockFormExists("sample-form");
        var registry = registryWith(new GoodService());
        var handler  = registry.find("sample-form").orElseThrow();

        SubmitResponse result = handler.invoke("sample-form", Map.of("name", "Alice"));

        assertThat(result).isInstanceOf(SubmitResponse.Accepted.class);
        var body = ((SubmitResponse.Accepted) result).body();
        assertThat(body).isInstanceOf(SampleForm.class);
        assertThat(((SampleForm) body).name()).isEqualTo("Alice");
    }

    @Test @DisplayName("invoke() with rejected → Rejected response")
    void invoke_rejected() {
        class RejectingService {
            @FormHandler
            public SubmitResponse handle(SampleForm form) {
                return SubmitResponse.rejected("Not available");
            }
        }
        mockFormExists("sample-form");
        var registry = registryWith(new RejectingService());
        var handler  = registry.find("sample-form").orElseThrow();

        SubmitResponse result = handler.invoke("sample-form", Map.of("name", "Bob"));

        assertThat(result).isInstanceOf(SubmitResponse.Rejected.class);
        var errors = ((SubmitResponse.Rejected) result).errors();
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().constraintName()).isEqualTo("server");
        assertThat(errors.getFirst().message()).isEqualTo("Not available");
        assertThat(errors.getFirst().path()).isNull();
    }

    // ─── Startup failures ─────────────────────────────────────────────────────

    @Test @DisplayName("Missing handler for known form → startup failure")
    void missing_handler_fails_startup() {
        when(ctx.getBeansOfType(Object.class)).thenReturn(Map.of());
        when(cache.knownFormIds()).thenReturn(java.util.Set.of("sample-form"));
        when(cache.get("sample-form")).thenReturn(Optional.of(dummyForm("sample-form")));

        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
            .isInstanceOf(FormHandlerRegistry.FormHandlerConfigurationException.class)
            .hasMessageContaining("sample-form")
            .hasMessageContaining("@FormHandler");
    }

    @Test @DisplayName("Duplicate @FormHandler for same form → startup failure")
    void duplicate_handler_fails_startup() {
        class AnotherService {
            @FormHandler
            public SubmitResponse handle(SampleForm form) {
                return SubmitResponse.ok();
            }
        }
        mockFormExists("sample-form");
        when(ctx.getBeansOfType(Object.class))
            .thenReturn(Map.of("a", new GoodService(), "b", new AnotherService()));

        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
            .isInstanceOf(FormHandlerRegistry.FormHandlerConfigurationException.class)
            .hasMessageContaining("Duplicate")
            .hasMessageContaining("sample-form");
    }

    @Test @DisplayName("@FormHandler references unknown form id → startup failure")
    void orphan_handler_fails_startup() {
        when(cache.get("unknown-form")).thenReturn(Optional.empty());
        when(cache.knownFormIds()).thenReturn(java.util.Set.of());
        when(ctx.getBeansOfType(Object.class)).thenReturn(Map.of(
            "svc", new Object() {
                @FormHandler
                public SubmitResponse handle(SampleForm f) { return SubmitResponse.ok(); }
            }
        ));

        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
            .isInstanceOf(FormHandlerRegistry.FormHandlerConfigurationException.class)
            .hasMessageContaining("DIFSP — Unknown form id in @FormHandler")
            .hasMessageContaining("Method  : svc#handle")
            .hasMessageContaining("Form id : 'sample-form' (inferred from @FormSpec on SampleForm)")
            .hasMessageContaining("Problem : no spec was found for this form id.")
            .hasMessageContaining("Fix     : Check that the annotation processor has run and that")
            .hasMessageContaining("META-INF/input-spec/sample-form.json was generated")
            .hasMessageContaining("Also verify that @FormSpec(id = \"sample-form\") on SampleForm")
            .hasMessageContaining("matches an existing spec file exactly.");
    }

    @Test @DisplayName("@FormHandler method with wrong parameter count → startup failure")
    void wrong_parameter_count_fails_startup() {
        mockFormExists("sample-form");
        when(ctx.getBeansOfType(Object.class)).thenReturn(Map.of(
            "svc", new Object() {
                @FormHandler
                public SubmitResponse handle(SampleForm f, String extra) {
                    return SubmitResponse.ok();
                }
            }
        ));

        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
            .isInstanceOf(FormHandlerRegistry.FormHandlerConfigurationException.class)
            .hasMessageContaining("exactly one parameter");
    }

    @Test @DisplayName("@FormHandler method with wrong return type → startup failure")
    void wrong_return_type_fails_startup() {
        mockFormExists("sample-form");
        when(ctx.getBeansOfType(Object.class)).thenReturn(Map.of(
            "svc", new Object() {
                @FormHandler
                public String handle(SampleForm f) { return "oops"; }
            }
        ));

        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
            .isInstanceOf(FormHandlerRegistry.FormHandlerConfigurationException.class)
            .hasMessageContaining("SubmitResponse");
    }

    // ─── SubmitResponse factory methods ──────────────────────────────────────

    @Nested @DisplayName("SubmitResponse")
    class SubmitResponseTests {

        @Test @DisplayName("ok() → Accepted with null body")
        void ok_no_body() {
            var r = SubmitResponse.ok();
            assertThat(r).isInstanceOf(SubmitResponse.Accepted.class);
            assertThat(((SubmitResponse.Accepted) r).body()).isNull();
        }

        @Test @DisplayName("ok(body) → Accepted with body")
        void ok_with_body() {
            var r = SubmitResponse.ok("result");
            assertThat(((SubmitResponse.Accepted) r).body()).isEqualTo("result");
        }

        @Test @DisplayName("rejected(message) → Rejected with server constraint")
        void rejected_message() {
            var r = SubmitResponse.rejected("Room not available");
            assertThat(r).isInstanceOf(SubmitResponse.Rejected.class);
            var errors = ((SubmitResponse.Rejected) r).errors();
            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().constraintName()).isEqualTo("server");
            assertThat(errors.getFirst().path()).isNull();
            assertThat(errors.getFirst().message()).isEqualTo("Room not available");
        }

        @Test @DisplayName("rejected(List) → Rejected with all errors")
        void rejected_list() {
            var errors = java.util.List.of(
                io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationError
                    .field(null, "server", "Error 1", null),
                io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationError
                    .field("endDate", "server", "Error 2", null)
            );
            var r = SubmitResponse.rejected(errors);
            assertThat(((SubmitResponse.Rejected) r).errors()).hasSize(2);
        }
    }
}
