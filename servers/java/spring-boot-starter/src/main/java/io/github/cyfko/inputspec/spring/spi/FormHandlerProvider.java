package io.github.cyfko.inputspec.spring.spi;

import io.github.cyfko.inputspec.spring.SubmitResponse;

import java.util.Map;
import java.util.Set;

/**
 * SPI for handling forms whose structure is not known at compile time — typically
 * forms contributed by an external module, a plugin, or a dynamic configuration.
 *
 * <h2>Role in the handler resolution chain</h2>
 * {@code FormHandlerProvider} is the <strong>fallback</strong> handler strategy.
 * The {@code FormHandlerRegistry} resolves handlers in the following order:
 * <ol>
 *   <li><strong>Primary</strong> — a method annotated with {@code @FormHandler} whose
 *       parameter type carries {@code @FormSpec(id = formId)}.
 *       This is the preferred strategy for forms defined in the same module.</li>
 *   <li><strong>Fallback</strong> — a {@code FormHandlerProvider} bean that declares
 *       the formId in {@link #getSupportedForms()}.
 *       This strategy is used when no {@code @FormHandler} is registered for the form,
 *       for example because the form was contributed by another module at runtime.</li>
 * </ol>
 * If a {@code @FormHandler} and a {@code FormHandlerProvider} both cover the same
 * formId, the {@code @FormHandler} wins silently — the provider is never invoked.
 *
 * <h2>Startup contract</h2>
 * <ul>
 *   <li>Every formId declared by {@link #getSupportedForms()} must exist in the
 *       {@code FormSpecCache} — orphan declarations cause a startup failure.</li>
 *   <li>Exactly one provider may declare a given formId. Two providers claiming the
 *       same formId cause a startup failure with an actionable error message.</li>
 *   <li>A form in {@code FormSpecCache} not covered by either a {@code @FormHandler}
 *       or a provider causes a startup failure.</li>
 * </ul>
 *
 * <h2>Runtime contract</h2>
 * <p>The starter guarantees that:</p>
 * <ul>
 *   <li>{@link #validate} is called <em>before</em> {@link #submit} — never submit
 *       without a prior successful validation in the same request.</li>
 *   <li>Stateless validation (field types, required fields, closed-domain values) has
 *       already been performed by {@code FormSpecValidator} before either method is
 *       invoked. {@link #validate} is responsible for <strong>business/stateful</strong>
 *       validation only (uniqueness checks, availability, cross-system rules, etc.).</li>
 *   <li>{@link #submit} is only called if {@link #validate} returns
 *       {@link SubmitResponse#ok()}.</li>
 * </ul>
 *
 * <h2>Minimal implementation example</h2>
 * <pre>{@code
 * @Component
 * public class PluginFormProvider implements FormHandlerProvider {
 *
 *     @Override
 *     public Set<String> getSupportedForms() {
 *         return Set.of("plugin-registration-form", "plugin-settings-form");
 *     }
 *
 *     @Override
 *     public SubmitResponse validate(String formId, Map<String, Object> rawForm) {
 *         if ("plugin-registration-form".equals(formId)) {
 *             String email = (String) rawForm.get("email");
 *             if (userRepository.existsByEmail(email)) {
 *                 return SubmitResponse.rejected("Email already registered");
 *             }
 *         }
 *         return SubmitResponse.ok();
 *     }
 *
 *     @Override
 *     public SubmitResponse submit(String formId, Map<String, Object> rawForm) {
 *         return switch (formId) {
 *             case "plugin-registration-form" -> register(rawForm);
 *             case "plugin-settings-form"     -> updateSettings(rawForm);
 *             default -> SubmitResponse.rejected("Unsupported form: " + formId);
 *         };
 *     }
 * }
 * }</pre>
 *
 * <h2>When to use this interface vs {@code @FormHandler}</h2>
 * <ul>
 *   <li>Use {@code @FormHandler} when the form class is available at compile time
 *       in the same module — it provides type safety and automatic deserialization.</li>
 *   <li>Use {@code FormHandlerProvider} when the form is contributed dynamically,
 *       comes from an external plugin, or when a single bean must handle a family
 *       of related forms sharing the same processing logic.</li>
 * </ul>
 *
 * @see io.github.cyfko.inputspec.spring.FormHandler
 * @see io.github.cyfko.inputspec.spring.SubmitResponse
 */
public interface FormHandlerProvider {

    /**
     * Returns the set of form ids this provider handles.
     *
     * <p>Called once at startup by {@code FormHandlerRegistry} to build the
     * fallback routing table. The returned set must be stable — it must not
     * change after the application context is initialized.</p>
     *
     * <p>Each formId in the returned set must correspond to a spec registered
     * in the {@code FormSpecCache}. An unknown formId causes a startup failure.</p>
     *
     * @return a non-null, non-empty, immutable set of formIds handled by this provider
     */
    Set<String> getSupportedForms();

    /**
     * Performs business/stateful validation for the given form submission.
     *
     * <p>This method is called <em>after</em> stateless validation (field constraints,
     * required fields, closed-domain values) has already passed. Its responsibility
     * is to enforce rules that require external state — for example:</p>
     * <ul>
     *   <li>Uniqueness checks (email already registered, slot already booked)</li>
     *   <li>Availability checks (resource exists and is not locked)</li>
     *   <li>Cross-system consistency rules</li>
     * </ul>
     *
     * <p>{@link #submit} is only called if this method returns
     * {@link SubmitResponse#ok()}. If validation fails, return
     * {@link SubmitResponse#rejected(String)} or
     * {@link SubmitResponse#rejected(java.util.List)} — {@link #submit}
     * will not be invoked.</p>
     *
     * @param formId  the id of the form being validated (one of {@link #getSupportedForms()})
     * @param rawForm the raw field values submitted by the client, keyed by field name
     * @return {@link SubmitResponse#ok()} if validation passes,
     *         or a {@link SubmitResponse#rejected} response with error details otherwise
     */
    SubmitResponse validate(String formId, Map<String, Object> rawForm);

    /**
     * Processes a validated form submission and produces a result.
     *
     * <p>This method is called only after both stateless validation and
     * {@link #validate} have passed successfully. At this point the data
     * is guaranteed to be structurally and semantically valid — this method
     * should focus exclusively on the business action (persist, notify, etc.).</p>
     *
     * @param formId  the id of the form being submitted (one of {@link #getSupportedForms()})
     * @param rawForm the raw field values submitted by the client, keyed by field name
     * @return {@link SubmitResponse#ok()} or {@link SubmitResponse#ok(Object)} on success,
     *         or a {@link SubmitResponse#rejected} response if a last-moment business
     *         condition prevents the submission (e.g. optimistic lock conflict)
     */
    SubmitResponse submit(String formId, Map<String, Object> rawForm);
}
