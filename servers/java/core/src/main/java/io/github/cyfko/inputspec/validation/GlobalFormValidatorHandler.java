package io.github.cyfko.inputspec.validation;

import java.util.Map;

/**
 * Functional handler for global form validation (Phase 3).
 *
 * Implementations receive the map of all field values representing the entire
 * form payload. They evaluate complex, cross-field business logic and return a
 * map mapping specific distinct field paths to their corresponding error messages.
 *
 * <p>If validation is successful, the handler should return an empty map or {@code null}.</p>
 *
 * <p>Register via {@link FormSpecValidator#registerGlobalFormHandler(String, GlobalFormValidatorHandler)}.</p>
 */
@FunctionalInterface
public interface GlobalFormValidatorHandler {

    /**
     * Validates business logic across the entire populated form payload.
     *
     * @param fieldValues map of field name → value representing the form
     * @return a map of (path → error message), or an empty map / null if valid
     */
    Map<String, String> validate(Map<String, Object> fieldValues);
}
