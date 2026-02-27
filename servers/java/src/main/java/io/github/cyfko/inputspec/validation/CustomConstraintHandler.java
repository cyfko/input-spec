package io.github.cyfko.inputspec.validation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Functional handler for {@code custom} constraints (§2.6).
 *
 * Implementations receive the field value and the constraint's {@code params}
 * node, and return an error message if validation fails, or
 * {@link Optional#empty()} if the value is valid.
 *
 * <p>Register via {@link FormSpecValidator#registerCustomHandler(String, CustomConstraintHandler)}.</p>
 *
 * <pre>
 * validator.registerCustomHandler("promoCode", (value, params) -> {
 *     int minDiscount = params.path("minDiscount").asInt(0);
 *     // … business logic …
 *     return isValid ? Optional.empty() : Optional.of("Invalid code");
 * });
 * </pre>
 */
@FunctionalInterface
public interface CustomConstraintHandler {

    /**
     * Validates a single value against custom business logic.
     *
     * @param value  the field value (never null when called by the pipeline)
     * @param params the constraint's {@code params} node from the spec
     * @return {@link Optional#empty()} if valid, or an error message string
     */
    Optional<String> validate(Object value, JsonNode params);
}
