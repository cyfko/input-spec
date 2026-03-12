package io.github.cyfko.inputspec.spring.ai;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.cache.BundleResolver;
import io.github.cyfko.inputspec.cache.FormSpecCache;
import io.github.cyfko.inputspec.model.FormSpecModel;
import io.github.cyfko.inputspec.spring.bootstrap.FormHandlerRegistry;
import io.github.cyfko.inputspec.spring.config.InputSpecMcpAutoConfiguration;
import io.github.cyfko.inputspec.spring.SubmitResponse;
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP (Model Context Protocol) tools exposing InputSpec forms to AI agents.
 *
 * <p>Registers 4 tools that an AI agent can call through the MCP protocol
 * to discover, read, validate, and submit forms — enabling a fully autonomous
 * form-filling workflow:</p>
 *
 * <ol>
 *   <li>{@link #inputspec_list_forms()} — discover what forms are available</li>
 *   <li>{@link #inputspec_get_form(String, String)} — understand the form's fields and constraints</li>
 *   <li>{@link #inputspec_validate_form(String, Map)} — pre-check data before submitting</li>
 *   <li>{@link #inputspec_submit_form(String, Map)} — submit for processing</li>
 * </ol>
 *
 * <p>This class is only instantiated when:</p>
 * <ul>
 *   <li>{@code inputspec.mcp.enabled=true} in the application configuration</li>
 *   <li>{@code spring-ai-starter-mcp-server} is on the classpath</li>
 * </ul>
 *
 * @see InputSpecMcpAutoConfiguration
 * @see McpSubmitResult
 */
public class InputSpecMcpTools {

    private final FormSpecCache       cache;
    private final FormSpecValidator   validator;
    private final FormHandlerRegistry registry;
    private final BundleResolver      bundleResolver;

    /**
     * Creates a new MCP tools instance.
     *
     * @param cache          the form specification cache
     * @param validator      the form validator
     * @param registry       the form handler registry
     * @param bundleResolver the i18n bundle resolver
     */
    public InputSpecMcpTools(FormSpecCache cache,
                             FormSpecValidator validator,
                             FormHandlerRegistry registry,
                             BundleResolver bundleResolver) {
        this.cache          = cache;
        this.validator      = validator;
        this.registry       = registry;
        this.bundleResolver = bundleResolver;
    }

    // ─── Tool 1: List forms ──────────────────────────────────────────────────

    /**
     * MCP tool record for form summaries.
     *
     * @param id          the form identifier
     * @param displayName the human-readable name of the form
     */
    public record FormInfo(String id, String displayName) {}

    @Tool(description = "List all available InputSpec forms in this application. "
            + "Returns an array of form summaries with their id and display name. "
            + "Use this to discover what forms the user can fill out.")
    public List<FormInfo> inputspec_list_forms() {
        return registry.registeredFormIds().stream()
            .map(id -> cache.get(id).orElse(null))
            .filter(Objects::nonNull)
            .map(spec -> new FormInfo(
                spec.id(),
                resolveDisplayName(spec.displayName(), spec.id())
            ))
            .sorted(Comparator.comparing(FormInfo::id))
            .collect(Collectors.toList());
    }

    // ─── Tool 2: Get form spec ───────────────────────────────────────────────

    @Tool(description = "Get the full specification of a form by its ID. "
            + "Returns all fields with their data types, validation constraints, "
            + "cross-field rules, and submission endpoint. "
            + "Use this to understand what data is needed to fill out the form, "
            + "what values are valid, and what constraints apply.")
    public FormSpecModel inputspec_get_form(
            @ToolParam(description = "The form identifier (e.g. 'booking-form')") String formId,
            @ToolParam(description = "BCP-47 locale for i18n (e.g. 'fr', 'en-US'). "
                    + "Leave empty for default language.", required = false) String locale) {

        return cache.get(formId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Form '" + formId + "' not found. "
                + "Use inputspec_list_forms to see available forms."));
    }

    // ─── Tool 3: Validate form ───────────────────────────────────────────────

    @Tool(description = "Validate form data against the specification without submitting. "
            + "Returns { isValid: true/false, errors: [...] }. "
            + "Use this to pre-check data before calling inputspec_submit_form. "
            + "If errors are returned, fix the data and validate again.")
    public ValidationResult inputspec_validate_form(
            @ToolParam(description = "The form identifier") String formId,
            @ToolParam(description = "The form data as key-value pairs, "
                    + "where keys are field names from the form spec") Map<String, Object> data) {

        FormSpecModel spec = cache.get(formId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Form '" + formId + "' not found. "
                + "Use inputspec_list_forms to see available forms."));

        return validator.validateForm(spec, data);
    }

    // ─── Tool 4: Submit form ─────────────────────────────────────────────────

    @Tool(description = "Submit validated form data for processing by the application. "
            + "The form data is first validated, then delegated to the application's handler. "
            + "Returns { status: 'accepted'|'rejected'|'validation_failed', body/errors }. "
            + "IMPORTANT: Always validate first with inputspec_validate_form before submitting.")
    public McpSubmitResult inputspec_submit_form(
            @ToolParam(description = "The form identifier") String formId,
            @ToolParam(description = "The form data as key-value pairs") Map<String, Object> data) {

        // 1. Resolve spec
        FormSpecModel spec = cache.get(formId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Form '" + formId + "' not found. "
                + "Use inputspec_list_forms to see available forms."));

        // 2. Validate
        ValidationResult validation = validator.validateForm(spec, data);
        if (!validation.isValid()) {
            return McpSubmitResult.validationFailed(validation.errors());
        }

        // 3. Delegate to @FormHandler
        FormHandlerRegistry.HandlerResolution handler = registry.find(formId)
            .orElseThrow(() -> new IllegalStateException(
                "No @FormHandler registered for form '" + formId + "'"));

        return switch (handler.invoke(formId, data)) {
            case SubmitResponse.Accepted a when a.body() != null ->
                McpSubmitResult.accepted(a.body());

            case SubmitResponse.Accepted a ->
                McpSubmitResult.accepted();

            case SubmitResponse.Rejected r ->
                McpSubmitResult.rejected(r.errors());
        };
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String resolveDisplayName(JsonNode displayName, String formId) {
        if (displayName == null) return formId;
        return bundleResolver.resolve(displayName, formId, null);
    }
}
