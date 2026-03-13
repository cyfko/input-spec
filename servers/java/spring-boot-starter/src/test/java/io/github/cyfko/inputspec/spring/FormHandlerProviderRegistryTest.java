package io.github.cyfko.inputspec.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.cache.FormSpecCache;
import io.github.cyfko.inputspec.model.FormSpecModel;
import io.github.cyfko.inputspec.spring.bootstrap.FormHandlerRegistry;
import io.github.cyfko.inputspec.spring.spi.FormHandlerProvider;
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Unit tests for {@link FormHandlerProvider}.
 * {@link FormSpecCache} and {@link ApplicationContext} are mocked — no Spring context needed.
 */
@DisplayName("FormHandlerProviderRegistry")
class FormHandlerProviderRegistryTest {

    private FormSpecCache      cache;
    private ApplicationContext ctx;
    private ObjectMapper       mapper;

    @BeforeEach
    void setUp() {
        cache  = mock(FormSpecCache.class);
        ctx    = mock(ApplicationContext.class);
        mapper = new ObjectMapper();
    }

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    /** Annotated with @FormSpec — formId inferred as "sample-form". */
    @FormSpec(id = "sample-form")
    record SampleForm(String name) {}

    /** Not annotated with @FormSpec — used to test the missing-annotation failure. */
    record NonAnnotatedForm(String name) {}

    /** Annotated with an id that is not present in the cache — used to test the orphan failure. */
    @FormSpec(id = "orphan-form")
    record OrphanForm(String value) {}

    static class GoodService {
        @FormHandler
        public SubmitResponse handle(SampleForm form) {
            return SubmitResponse.ok(form);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private FormHandlerRegistry registryWith(Object... beans) {
        return registryWithProviders(Map.of(), beans);
    }

    private FormHandlerRegistry registryWithProviders(
            Map<String, FormHandlerProvider> providers, Object... beans) {
        Map<String, Object> beanMap = new LinkedHashMap<>();
        for (int i = 0; i < beans.length; i++) beanMap.put("bean" + i, beans[i]);
        when(ctx.getBeansOfType(Object.class)).thenReturn(beanMap);
        when(ctx.getBeansOfType(FormHandlerProvider.class)).thenReturn(providers);
        // Mock resolveClass for every formId declared by any provider
        providers.values().forEach(p -> p.getSupportedForms().forEach(formId ->
                when(cache.resolveClass(formId)).thenReturn(Optional.of(SampleForm.class))
        ));
        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);
        registry.afterSingletonsInstantiated();
        return registry;
    }

    private void mockFormExists(String... ids) {
        Set<String> idSet = new HashSet<>(Arrays.asList(ids));
        for (String id : ids) when(cache.get(id)).thenReturn(Optional.of(dummyForm(id)));
        when(cache.knownFormIds()).thenReturn(idSet);
    }

    private FormSpecModel dummyForm(String id) {
        return new FormSpecModel(id, null, null, null, List.of(), List.of(), null);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

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

    @Test @DisplayName("find() with unknown id → empty")
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

    @Test @DisplayName("invoke() with rejected handler → Rejected response")
    void invoke_rejected() {
        class RejectingService {
            @FormHandler
            public SubmitResponse handle(SampleForm form) {
                return SubmitResponse.rejected("Not available", HttpStatus.BAD_REQUEST);
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

    @Test @DisplayName("Missing handler for a known form → startup failure")
    void missing_handler_fails_startup() {
        when(ctx.getBeansOfType(Object.class)).thenReturn(Map.of());
        when(cache.knownFormIds()).thenReturn(Set.of("sample-form"));
        when(cache.get("sample-form")).thenReturn(Optional.of(dummyForm("sample-form")));

        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
                .isInstanceOf(FormHandlerRegistry.FormHandlerConfigurationException.class)
                .hasMessageContaining("sample-form")
                .hasMessageContaining("@FormHandler");
    }

    @Test @DisplayName("Duplicate @FormHandler for the same form → startup failure")
    void duplicate_handler_fails_startup() {
        class AnotherService {
            @FormHandler
            public SubmitResponse handle(SampleForm form) { return SubmitResponse.ok(); }
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

    @Test @DisplayName("@FormHandler parameter type not annotated with @FormSpec → startup failure")
    void parameter_without_formspec_fails_startup() {
        when(cache.knownFormIds()).thenReturn(Set.of());
        when(ctx.getBeansOfType(Object.class)).thenReturn(Map.of(
                "svc", new Object() {
                    @FormHandler
                    public SubmitResponse handle(NonAnnotatedForm f) { return SubmitResponse.ok(); }
                }
        ));

        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
                .isInstanceOf(FormHandlerRegistry.FormHandlerConfigurationException.class)
                .hasMessageContaining("NonAnnotatedForm")
                .hasMessageContaining("@FormSpec");
    }

    @Test @DisplayName("@FormHandler formId not present in cache → startup failure")
    void orphan_form_id_fails_startup() {
        when(cache.get("orphan-form")).thenReturn(Optional.empty());
        when(cache.knownFormIds()).thenReturn(Set.of());
        when(ctx.getBeansOfType(Object.class)).thenReturn(Map.of(
                "svc", new Object() {
                    @FormHandler
                    public SubmitResponse handle(OrphanForm f) { return SubmitResponse.ok(); }
                }
        ));

        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
                .isInstanceOf(FormHandlerRegistry.FormHandlerConfigurationException.class)
                .hasMessageContaining("orphan-form")
                .hasMessageContaining("OrphanForm");
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


    // ─── FormHandlerProvider (fallback) ──────────────────────────────────────

    /** A provider that handles "contributed-form". Validates that "field" is non-blank. */
    static class SampleProvider implements FormHandlerProvider {
        @Override
        public Set<String> getSupportedForms() { return Set.of("contributed-form"); }

        @Override
        public SubmitResponse validate(Class<?> spec, Map<String, Object> rawForm) {
            Object field = rawForm.get("field");
            if (field == null || field.toString().isBlank()) {
                return SubmitResponse.rejected("field must not be blank", HttpStatus.BAD_REQUEST);
            }
            return SubmitResponse.ok();
        }

        @Override
        public SubmitResponse submit(Class<?> spec, Map<String, Object> rawForm) {
            return SubmitResponse.ok(rawForm.get("field"));
        }
    }

    @Test @DisplayName("Form covered only by a provider → startup succeeds, find() returns ResolvedProvider")
    void provider_covers_form() {
        mockFormExists("contributed-form");
        var registry = registryWithProviders(Map.of("p", new SampleProvider()));
        assertThat(registry.find("contributed-form"))
                .isPresent()
                .get().isInstanceOf(FormHandlerRegistry.ResolvedProvider.class);
    }

    @Test @DisplayName("ResolvedProvider.validate() passes → ok()")
    void provider_validate_passes() {
        mockFormExists("contributed-form");
        var registry    = registryWithProviders(Map.of("p", new SampleProvider()));
        var resolution  = registry.find("contributed-form").orElseThrow();

        SubmitResponse result = resolution.validate("contributed-form", Map.of("field", "hello"));

        assertThat(result).isInstanceOf(SubmitResponse.Accepted.class);
    }

    @Test @DisplayName("ResolvedProvider.validate() fails → rejected, submit not called")
    void provider_validate_rejects() {
        mockFormExists("contributed-form");
        var registry   = registryWithProviders(Map.of("p", new SampleProvider()));
        var resolution = registry.find("contributed-form").orElseThrow();

        SubmitResponse result = resolution.validate("contributed-form", Map.of("field", ""));

        assertThat(result).isInstanceOf(SubmitResponse.Rejected.class);
        assertThat(((SubmitResponse.Rejected) result).errors().getFirst().message())
                .isEqualTo("field must not be blank");
    }

    @Test @DisplayName("ResolvedProvider.invoke() delegates to provider.submit()")
    void provider_invoke_delegates_to_submit() {
        mockFormExists("contributed-form");
        var registry   = registryWithProviders(Map.of("p", new SampleProvider()));
        var resolution = registry.find("contributed-form").orElseThrow();

        SubmitResponse result = resolution.invoke("contributed-form", Map.of("field", "hello"));

        assertThat(result).isInstanceOf(SubmitResponse.Accepted.class);
        assertThat(((SubmitResponse.Accepted) result).body()).isEqualTo("hello");
    }

    @Test @DisplayName("ResolvedHandler.validate() is a no-op → always ok()")
    void resolved_handler_validate_is_noop() {
        mockFormExists("sample-form");
        var registry   = registryWith(new GoodService());
        var resolution = registry.find("sample-form").orElseThrow();

        SubmitResponse result = resolution.validate("sample-form", Map.of("name", "Alice"));

        assertThat(result).isInstanceOf(SubmitResponse.Accepted.class);
    }

    @Test @DisplayName("@FormHandler wins over provider for the same formId")
    void formhandler_wins_over_provider() {
        mockFormExists("sample-form");
        var registry = registryWithProviders(
                Map.of("p", new FormHandlerProvider() {
                    @Override public Set<String> getSupportedForms() { return Set.of("sample-form"); }
                    @Override public SubmitResponse validate(Class<?> spec, Map<String, Object> f) {
                        return SubmitResponse.rejected("should not be called", HttpStatus.BAD_REQUEST);
                    }
                    @Override public SubmitResponse submit(Class<?> spec, Map<String, Object> f) {
                        return SubmitResponse.rejected("should not be called", HttpStatus.BAD_REQUEST);
                    }
                }),
                new GoodService()
        );

        var resolution = registry.find("sample-form").orElseThrow();
        assertThat(resolution).isInstanceOf(FormHandlerRegistry.ResolvedHandler.class);
    }

    @Test @DisplayName("Two providers for the same formId → startup failure")
    void duplicate_provider_fails_startup() {
        mockFormExists("contributed-form");
        FormHandlerProvider p1 = new SampleProvider();
        FormHandlerProvider p2 = new FormHandlerProvider() {
            @Override public Set<String> getSupportedForms() { return Set.of("contributed-form"); }
            @Override public SubmitResponse validate(Class<?> spec, Map<String, Object> f) {
                return SubmitResponse.ok();
            }
            @Override public SubmitResponse submit(Class<?> spec, Map<String, Object> f) {
                return SubmitResponse.ok();
            }
        };
        when(ctx.getBeansOfType(Object.class)).thenReturn(Map.of());
        when(ctx.getBeansOfType(FormHandlerProvider.class))
                .thenReturn(new LinkedHashMap<>(Map.of("p1", p1, "p2", p2)));

        FormHandlerRegistry registry = new FormHandlerRegistry(ctx, cache, mapper);

        assertThatThrownBy(registry::afterSingletonsInstantiated)
                .isInstanceOf(FormHandlerRegistry.FormHandlerConfigurationException.class)
                .hasMessageContaining("Duplicate FormHandlerProvider")
                .hasMessageContaining("contributed-form");
    }

    @Test @DisplayName("registeredFormIds() includes ids from providers")
    void registeredFormIds_includes_provider_ids() {
        mockFormExists("sample-form", "contributed-form");
        var registry = registryWithProviders(
                Map.of("p", new SampleProvider()),
                new GoodService()
        );
        assertThat(registry.registeredFormIds())
                .containsExactlyInAnyOrder("sample-form", "contributed-form");
    }

    @Test @DisplayName("Form covered by a provider is not reported as missing at startup")
    void provider_satisfies_coverage_check() {
        mockFormExists("contributed-form");
        assertThatNoException().isThrownBy(() ->
                registryWithProviders(Map.of("p", new SampleProvider()))
        );
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
            var r = SubmitResponse.rejected("Room not available", HttpStatus.BAD_REQUEST);
            assertThat(r).isInstanceOf(SubmitResponse.Rejected.class);
            var errors = ((SubmitResponse.Rejected) r).errors();
            assertThat(errors).hasSize(1);
            assertThat(errors.getFirst().constraintName()).isEqualTo("server");
            assertThat(errors.getFirst().path()).isNull();
            assertThat(errors.getFirst().message()).isEqualTo("Room not available");
        }

        @Test @DisplayName("rejected(List) → Rejected with all errors")
        void rejected_list() {
            var errors = List.of(
                    FormSpecValidator.ValidationError.field(null,      "server", "Error 1", null),
                    FormSpecValidator.ValidationError.field("endDate", "server", "Error 2", null)
            );
            var r = SubmitResponse.rejected(errors);
            assertThat(((SubmitResponse.Rejected) r).errors()).hasSize(2);
        }
    }
}
