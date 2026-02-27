package io.github.cyfko.inputspec.spring.annotations;

import java.lang.annotation.*;

/**
 * Marks an entity class for automatic input-spec endpoint generation.
 * <p>
 * When applied to a JPA entity, this annotation triggers the generation of a REST endpoint
 * that returns an {@link io.github.cyfko.inputspec.model.InputSpec} document describing
 * the entity's fields according to the Dynamic Input Field Specification Protocol v2.1.
 * </p>
 *
 * <h2>Basic Usage (Zero-Config)</h2>
 * <pre>{@code
 * @Entity
 * @InputSpecEnabled
 * public class User {
 *     private String username;
 *     private String email;
 *     private LocalDate birthDate;
 * }
 * }</pre>
 *
 * <p>This generates: {@code GET /api/users/input-spec}</p>
 *
 * <h2>Custom Endpoint Path</h2>
 * <pre>{@code
 * @Entity
 * @InputSpecEnabled(path = "/forms/user-registration")
 * public class User {
 *     // ...
 * }
 * }</pre>
 *
 * <h2>Field-Level Enrichment</h2>
 * <pre>{@code
 * @Entity
 * @InputSpecEnabled
 * public class User {
 *     @InputField(
 *         displayName = "Username",
 *         description = "Unique identifier for login",
 *         formatHint = "lowercase"
 *     )
 *     @Size(min = 3, max = 20)
 *     private String username;
 *
 *     @InputField(
 *         displayName = "Email Address",
 *         formatHint = "email"
 *     )
 *     @Email
 *     @Column(nullable = false)
 *     private String email;
 * }
 * }</pre>
 *
 * <h2>Auto-Detection Features</h2>
 * <ul>
 *   <li><b>Data Types:</b> Auto-mapped from Java types (String→STRING, Integer→NUMBER, LocalDate→DATE, etc.)</li>
 *   <li><b>Required Fields:</b> Detected from {@code @NotNull}, {@code @NotBlank}, {@code @Column(nullable=false)}</li>
 *   <li><b>Constraints:</b> Generated from Bean Validation annotations ({@code @Size}, {@code @Min}, {@code @Max}, {@code @Pattern})</li>
 *   <li><b>Enums:</b> Automatically converted to INLINE {@link io.github.cyfko.inputspec.model.ValuesEndpoint}</li>
 *   <li><b>Collections:</b> Sets {@code expectMultipleValues = true}</li>
 * </ul>
 *
 * @author cyfko
 * @since 2.1.0
 * @see InputField
 * @see InputSpecProvider
 * @see <a href="https://github.com/cyfko/input-spec">Input Spec Protocol v2.1</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface InputSpecEnabled {

    /**
     * Custom path for the input-spec endpoint.
     * <p>
     * If not specified, defaults to {@code /{entityName}/input-spec} where entityName
     * is the lowercase, pluralized entity class name.
     * </p>
     *
     * @return custom endpoint path
     */
    String path() default "";

    /**
     * Base path prefix for the endpoint (e.g., "/api/v1").
     * <p>
     * If not specified, uses the application's default base path from configuration.
     * </p>
     *
     * @return base path prefix
     */
    String basePath() default "";

    /**
     * Protocol version to include in the generated {@link io.github.cyfko.inputspec.model.InputSpec}.
     * <p>
     * Defaults to "2.1" (latest).
     * </p>
     *
     * @return protocol version string
     */
    String protocolVersion() default "2.1";

    /**
     * Whether to include only explicitly annotated fields with {@link InputField}.
     * <p>
     * If {@code false} (default), all entity fields are included with auto-detected metadata.
     * If {@code true}, only fields annotated with {@code @InputField} are exposed.
     * </p>
     *
     * @return true to include only explicit fields
     */
    boolean explicitFieldsOnly() default false;
}
