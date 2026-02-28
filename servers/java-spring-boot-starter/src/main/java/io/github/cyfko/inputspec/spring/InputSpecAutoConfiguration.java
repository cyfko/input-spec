package io.github.cyfko.inputspec.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cyfko.inputspec.cache.BundleResolver;
import io.github.cyfko.inputspec.cache.FormSpecCache;
import io.github.cyfko.inputspec.model.FormSpecModel;
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring Boot auto-configuration for InputSpec.
 *
 * <p>Activates automatically when Spring Web is on the classpath
 * and {@code inputspec.enabled=true} (default).
 *
 * <h3>Endpoints exposed at {@code /api/forms} (configurable via {@code inputspec.base-path}):</h3>
 * <ul>
 *   <li>{@code GET  /api/forms}              → list all available form summaries</li>
 *   <li>{@code GET  /api/forms/{id}}         → full FormSpec (i18n via Accept-Language)</li>
 *   <li>{@code POST /api/forms/{id}/validate} → stateless validation without submission</li>
 *   <li>{@code POST /api/forms/{id}/submit}   → validate then delegate to @FormHandler</li>
 * </ul>
 *
 * <h3>Startup guarantees (application refuses to start if violated):</h3>
 * <ul>
 *   <li>Every form spec has exactly one @FormHandler registered</li>
 *   <li>No duplicate @FormHandlers for the same form id</li>
 *   <li>All @FormHandlers reference valid form ids</li>
 * </ul>
 *
 * <p>Add to {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}:
 * <pre>io.github.cyfko.inputspec.spring.InputSpecAutoConfiguration</pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
@ConditionalOnProperty(prefix = "inputspec", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(InputSpecProperties.class)
public class InputSpecAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FormSpecCache formSpecCache() {
        FormSpecCache cache = FormSpecCache.getInstance();
        cache.preloadAll();
        return cache;
    }

    @Bean
    @ConditionalOnMissingBean
    public BundleResolver bundleResolver() {
        return BundleResolver.getInstance();
    }

    @Bean
    @ConditionalOnMissingBean
    public FormSpecValidator formSpecValidator() {
        return new FormSpecValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public FormHandlerRegistry formHandlerRegistry(ApplicationContext ctx,
                                                    FormSpecCache cache,
                                                    ObjectMapper mapper) {
        return new FormHandlerRegistry(ctx, cache, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public FormValidatorRegistry formValidatorRegistry(ApplicationContext ctx,
                                                       FormSpecValidator validator,
                                                       ObjectMapper mapper) {
        return new FormValidatorRegistry(ctx, validator, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public InputSpecController inputSpecController(FormSpecCache cache,
                                                    FormSpecValidator validator,
                                                    FormHandlerRegistry registry) {
        return new InputSpecController(cache, validator, registry);
    }

    // ─── REST Controller ──────────────────────────────────────────────────────

    @RestController
    @RequestMapping("${inputspec.base-path:/api/forms}")
    public static class InputSpecController {

        private final FormSpecCache       cache;
        private final FormSpecValidator   validator;
        private final FormHandlerRegistry registry;

        public InputSpecController(FormSpecCache cache,
                                    FormSpecValidator validator,
                                    FormHandlerRegistry registry) {
            this.cache     = cache;
            this.validator = validator;
            this.registry  = registry;
        }

        /**
         * GET /api/forms
         * Returns the list of available form summaries (id + displayName).
         * Only forms that have both a spec and a registered handler are listed.
         */
        @GetMapping
        public List<FormSummary> listForms() {
            return registry.registeredFormIds().stream()
                .map(id -> cache.get(id).orElse(null))
                .filter(Objects::nonNull)
                .map(spec -> new FormSummary(spec.id(), spec.displayName()))
                .sorted(Comparator.comparing(FormSummary::id))
                .collect(Collectors.toList());
        }

        /**
         * GET /api/forms/{id}
         * Returns the full FormSpec for a given form id.
         * Use Accept-Language for i18n resolution of display names and messages.
         */
        @GetMapping("/{id}")
        public ResponseEntity<FormSpecModel> getForm(@PathVariable String id) {
            return cache.get(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * POST /api/forms/{id}/validate
         * Runs stateless validation against the form spec without submitting.
         * Useful for step-by-step UX or pre-flight checks.
         *
         * Always returns 200 with { isValid, errors }.
         * Returns 404 if the form id is unknown.
         */
        @PostMapping("/{id}/validate")
        public ResponseEntity<ValidationResult> validate(
                @PathVariable String id,
                @RequestBody Map<String, Object> values,
                @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

            return cache.get(id)
                .map(spec -> ResponseEntity.ok(
                    validator.validateForm(spec, values, resolveLocale(acceptLanguage))
                ))
                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * POST /api/forms/{id}/submit
         * Full submission pipeline:
         * <ol>
         *   <li>Validates stateless (field constraints + cross-constraints)</li>
         *   <li>If invalid → 200 { isValid: false, errors: [...] }</li>
         *   <li>If valid   → delegates to the registered @FormHandler
         *     <ul>
         *       <li>Accepted with body  → 201 Created</li>
         *       <li>Accepted no body    → 204 No Content</li>
         *       <li>Rejected            → 200 { isValid: false, errors: [...] }</li>
         *     </ul>
         *   </li>
         * </ol>
         *
         * Returns 404 if the form id is unknown.
         * The @FormHandler is guaranteed to be present (startup validation ensures it).
         */
        @PostMapping("/{id}/submit")
        public ResponseEntity<?> submit(
                @PathVariable String id,
                @RequestBody Map<String, Object> values,
                @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

            // 1. Resolve spec — 404 if unknown
            Optional<FormSpecModel> specOpt = cache.get(id);
            if (specOpt.isEmpty()) return ResponseEntity.notFound().build();
            FormSpecModel spec = specOpt.get();

            // 2. Stateless validation
            ValidationResult validation =
                validator.validateForm(spec, values, resolveLocale(acceptLanguage));

            if (!validation.isValid()) {
                return ResponseEntity.ok(validation);
            }

            // 3. Delegate to @FormHandler
            FormHandlerRegistry.ResolvedHandler handler = registry.find(id)
                .orElseThrow(() -> new IllegalStateException(
                    "No @FormHandler found for form '" + id +
                    "' — this should have been caught at startup"));

            return switch (handler.invoke(values)) {
                case SubmitResponse.Accepted a when a.body() != null ->
                    ResponseEntity.status(HttpStatus.CREATED).body(a.body());

                case SubmitResponse.Accepted a ->
                    ResponseEntity.noContent().build();

                case SubmitResponse.Rejected r ->
                    ResponseEntity.ok(new ValidationResult(false, r.errors()));
            };
        }

        // ─── Helpers ─────────────────────────────────────────────────────────

        private static Locale resolveLocale(String acceptLanguage) {
            if (acceptLanguage == null || acceptLanguage.isBlank()) return null;
            return Locale.forLanguageTag(acceptLanguage.split(",")[0].trim());
        }
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    /**
     * Summary record returned by the list endpoint.
     * Includes the form id and its display name (as raw JsonNode for i18n compatibility).
     */
    public record FormSummary(String id, com.fasterxml.jackson.databind.JsonNode displayName) {}
}
