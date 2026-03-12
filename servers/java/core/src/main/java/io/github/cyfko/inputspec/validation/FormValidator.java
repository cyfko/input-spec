package io.github.cyfko.inputspec.validation;

import java.lang.annotation.*;

/**
 * Marks a method as a validation handler for an InputSpec form or specific constraint.
 *
 * <p>This annotation is unified. The execution phase and context of the method is determined dynamically
 * based on its return type by the integration layer (e.g., Spring Boot Starter FormValidatorRegistry).</p>
 *
 * <h3>Context A: Constraint-level (Phase 2 Custom Validator)</h3>
 * <p>If the method returns {@code java.util.Optional<String>}:</p>
 * <ul>
 *     <li>The {@code value()} is evaluated as the {@code customKey} of a specific CUSTOM constraint or cross-constraint.</li>
 *     <li>The method receives the instantiated form POJO.</li>
 *     <li>It returns {@code Optional.empty()} on success, or an error message if invalid.</li>
 * </ul>
 *
 * <h3>Context B: Form-level (Phase 3 Global Validator)</h3>
 * <p>If the method returns {@code java.util.Map<String, String>}:</p>
 * <ul>
 *     <li>The {@code value()} is evaluated as the global {@code formId}.</li>
 *     <li>The method receives the instantiated form POJO.</li>
 *     <li>It returns an empty map (or null) on success, or a map of {@code path -> errorMessage} if invalid.</li>
 *     <li>It executes <b>only if</b> all standard validations and Phase 2 custom validations succeeded without error.</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FormValidator {

    /**
     * The target identifier: either a custom routing key (for constraint-level validation)
     * or the actual form id (for global, form-level validation).
     */
    String value();
}
