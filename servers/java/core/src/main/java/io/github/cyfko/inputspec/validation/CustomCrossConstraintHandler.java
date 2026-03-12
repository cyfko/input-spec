package io.github.cyfko.inputspec.validation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

/**
 * Functional handler for {@code custom} cross-field constraints (§2.10).
 *
 * Implementations receive the map of all field values and the cross-constraint's
 * {@code params} node, and return an error message if validation fails, or
 * {@link Optional#empty()} if valid.
 *
 * <p>Register via {@link FormSpecValidator#registerCustomCrossHandler(String, CustomCrossConstraintHandler)}.</p>
 *
 * <pre>
 * validator.registerCustomCrossHandler("complexRule", (fieldValues, params) -> {
 *     Object a = fieldValues.get("fieldA");
 *     Object b = fieldValues.get("fieldB");
 *     // … business logic …
 *     return isValid ? Optional.empty() : Optional.of("A and B are inconsistent");
 * });
 * </pre>
 */
@FunctionalInterface
public interface CustomCrossConstraintHandler {

    /**
     * Validates cross-field business logic.
     *
     * @param fieldValues map of field name → value for the involved fields
     * @param params      the cross-constraint's {@code params} node from the spec
     * @return {@link Optional#empty()} if valid, or an error message string
     */
    Optional<String> validate(Map<String, Object> fieldValues, JsonNode params);
}
