package io.github.cyfko.inputspec;

import java.lang.annotation.*;

/**
 * Describes a single search/filter parameter for a remote ValuesEndpoint.
 *
 * A set of {@code @SearchParam} on a {@code @ValuesSource} is compiled by the
 * annotation processor into a JSON Schema object (the {@code searchParamsSchema}
 * field of the protocol's ValuesEndpoint — §2.2), and into a concrete
 * {@code searchParams} default map.
 *
 * This replaces the error-prone {@code searchParamsSchema = "{\"type\":...}"} string.
 * The processor validates the combination of attributes at compile time:
 * - {@code enumValues} requires {@code type = STRING}
 * - {@code minimum}/{@code maximum} require {@code type = NUMBER} or {@code INTEGER}
 * - {@code format} is informational only and is passed through as-is
 *
 * Usage:
 * <pre>
 * {@literal @}ValuesSource(
 *     protocol = "HTTPS",
 *     uri      = "/api/products",
 *     searchParams = {
 *         {@literal @}SearchParam(
 *             name        = "name",
 *             type        = SchemaType.STRING,
 *             description = "Partial product name",
 *             required    = true
 *         ),
 *         {@literal @}SearchParam(
 *             name       = "status",
 *             type       = SchemaType.STRING,
 *             enumValues = {"active", "archived", "pending"}
 *         ),
 *         {@literal @}SearchParam(
 *             name    = "minPrice",
 *             type    = SchemaType.NUMBER,
 *             minimum = "0"
 *         )
 *     }
 * )
 * </pre>
 *
 * Generates:
 * <pre>
 * {
 *   "type": "object",
 *   "properties": {
 *     "name":     { "type": "string",  "description": "Partial product name" },
 *     "status":   { "type": "string",  "enum": ["active", "archived", "pending"] },
 *     "minPrice": { "type": "number",  "minimum": 0 }
 *   },
 *   "required": ["name"]
 * }
 * </pre>
 */
@Target({})                        // usable only as an annotation element value
@Retention(RetentionPolicy.SOURCE)
public @interface SearchParam {

    /** Parameter name (the JSON property key). */
    String name();

    /** JSON Schema type. */
    SchemaType type();

    /**
     * Human-readable description embedded in the schema.
     * Used by AI agents and tooling to understand the parameter's semantics.
     */
    String description() default "";

    /** Whether this parameter is required in the searchParams object. */
    boolean required() default false;

    /**
     * Restricts the value to a fixed set of strings.
     * Only valid when {@code type = STRING}.
     * Generates a JSON Schema {@code "enum"} constraint.
     */
    String[] enumValues() default {};

    /**
     * Inclusive numeric minimum.
     * Only valid when {@code type = NUMBER} or {@code INTEGER}.
     * Accepts decimal strings (e.g. {@code "0"}, {@code "0.5"}).
     * Generates a JSON Schema {@code "minimum"} constraint.
     */
    String minimum() default "";

    /**
     * Inclusive numeric maximum.
     * Only valid when {@code type = NUMBER} or {@code INTEGER}.
     * Generates a JSON Schema {@code "maximum"} constraint.
     */
    String maximum() default "";

    /**
     * JSON Schema {@code "format"} hint (e.g. {@code "date-time"}, {@code "email"}).
     * Informational only — not enforced by the DIFSP validator.
     * Passed through as-is into the generated schema.
     */
    String format() default "";

    /**
     * Default value for this parameter, included in the concrete
     * {@code searchParams} map of the generated ValuesEndpoint.
     * Leave empty to omit from the defaults map.
     */
    String defaultValue() default "";

    // ─── Type enum ────────────────────────────────────────────────────────────

    /**
     * Closed set of JSON Schema primitive types.
     * Covers all types needed for search/filter parameters.
     */
    enum SchemaType {
        STRING,
        NUMBER,
        INTEGER,
        BOOLEAN,
        ARRAY;

        /** Returns the lowercase JSON Schema type string. */
        public String jsonValue() { return name().toLowerCase(); }
    }
}