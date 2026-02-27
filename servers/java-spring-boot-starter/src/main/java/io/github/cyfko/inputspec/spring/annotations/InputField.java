package io.github.cyfko.inputspec.spring.annotations;

import java.lang.annotation.*;

/**
 * Enriches field-level metadata for input-spec generation.
 * <p>
 * Optional annotation to provide human-friendly labels, descriptions, and UI hints
 * for entity fields. Works in conjunction with {@link InputSpecEnabled} on the class level.
 * </p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Enrichment</h3>
 * <pre>{@code
 * @InputField(
 *     displayName = "Full Name",
 *     description = "Legal full name as it appears on official documents"
 * )
 * private String name;
 * }</pre>
 *
 * <h3>With Format Hints</h3>
 * <pre>{@code
 * @InputField(
 *     displayName = "Email Address",
 *     formatHint = "email"  // UI renders as <input type="email">
 * )
 * private String email;
 *
 * @InputField(
 *     displayName = "Website",
 *     formatHint = "url"
 * )
 * private String website;
 *
 * @InputField(
 *     displayName = "Favorite Color",
 *     formatHint = "color"  // UI renders color picker
 * )
 * private String color;
 * }</pre>
 *
 * <h3>Remote Values Endpoint</h3>
 * <pre>{@code
 * @InputField(
 *     displayName = "Country",
 *     description = "Country of residence",
 *     valuesEndpoint = @ValuesEndpointConfig(
 *         uri = "/api/countries",
 *         searchable = true,
 *         debounceMs = 300
 *     )
 * )
 * private String countryCode;
 * }</pre>
 *
 * <h3>Override Auto-Detection</h3>
 * <pre>{@code
 * @InputField(
 *     required = true,  // Override even if @NotNull is absent
 *     expectMultipleValues = false  // Force single value even if List type
 * )
 * private String tags;
 * }</pre>
 *
 * @author cyfko
 * @since 2.1.0
 * @see InputSpecEnabled
 * @see ValuesEndpointConfig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Documented
public @interface InputField {

    /**
     * Human-readable label for UI display.
     * <p>
     * If not specified, field name is converted to title case (e.g., "firstName" → "First Name").
     * </p>
     *
     * @return display name
     */
    String displayName() default "";

    /**
     * Optional free-form description for tooltips or help text.
     *
     * @return field description
     */
    String description() default "";

    /**
     * UI formatting hint (e.g., "email", "url", "tel", "color", "date-time", "markdown").
     * <p>
     * Common values:
     * <ul>
     *   <li>{@code email} - Email input with validation</li>
     *   <li>{@code url} - URL input with validation</li>
     *   <li>{@code tel} - Phone number input</li>
     *   <li>{@code color} - Color picker</li>
     *   <li>{@code date-time} - Date-time picker</li>
     *   <li>{@code markdown} - Markdown editor</li>
     *   <li>{@code password} - Password input (masked)</li>
     *   <li>{@code autocomplete} - Autocomplete field</li>
     * </ul>
     * </p>
     *
     * @return format hint
     */
    String formatHint() default "";

    /**
     * Whether the field expects multiple values (array/list).
     * <p>
     * By default, auto-detected from field type (Collection, array, etc.).
     * Set explicitly to override auto-detection.
     * </p>
     *
     * @return true if field accepts multiple values
     */
    boolean expectMultipleValues() default false;

    /**
     * Whether a non-empty value is mandatory.
     * <p>
     * By default, auto-detected from {@code @NotNull}, {@code @NotBlank}, {@code @Column(nullable=false)}.
     * Set explicitly to override auto-detection.
     * </p>
     *
     * @return true if field is required
     */
    boolean required() default false;

    /**
     * Configuration for remote values endpoint (for dropdowns, autocomplete, etc.).
     * <p>
     * Only applicable for fields with restricted value domains.
     * </p>
     *
     * @return values endpoint configuration
     */
    ValuesEndpointConfig valuesEndpoint() default @ValuesEndpointConfig;

    /**
     * Whether to exclude this field from the generated input-spec.
     * <p>
     * Useful for internal fields that shouldn't be exposed in forms.
     * </p>
     *
     * @return true to exclude this field
     */
    boolean exclude() default false;
}
