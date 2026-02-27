package io.github.cyfko.inputspec;

import java.lang.annotation.*;

/**
 * Configures the ValuesEndpoint for a field (§2.2 of the DIFSP protocol).
 *
 * Used as a nested element inside {@literal @}FieldMeta.
 *
 * <h3>INLINE example (static enumeration)</h3>
 * <pre>
 * {@literal @}FieldMeta(
 *     displayName  = "Status",
 *     valuesSource = {@literal @}ValuesSource(
 *         protocol = "INLINE",
 *         items    = {
 *             {@literal @}Inline(value = "ACTIVE",   label = "Active"),
 *             {@literal @}Inline(value = "INACTIVE", label = "Inactive"),
 *             {@literal @}Inline(value = "PENDING",  label = "Pending")
 *         }
 *     )
 * )
 * </pre>
 *
 * <h3>Remote paginated example with typed search params</h3>
 * <pre>
 * {@literal @}FieldMeta(
 *     displayName  = "Assigned To",
 *     valuesSource = {@literal @}ValuesSource(
 *         protocol     = "HTTPS",
 *         uri          = "/api/users",
 *         mode         = ValuesMode.CLOSED,
 *         pagination   = PaginationStrategy.PAGE_NUMBER,
 *         dataField    = "data",
 *         totalField   = "total",
 *         hasNextField = "hasNext",
 *         searchParams = {
 *             {@literal @}SearchParam(
 *                 name        = "name",
 *                 type        = SchemaType.STRING,
 *                 description = "Partial user name for search"
 *             )
 *         },
 *         cacheStrategy = "SESSION"
 *     )
 * )
 * </pre>
 */
@Target({})
@Retention(RetentionPolicy.SOURCE)
public @interface ValuesSource {

    // ── Protocol & mode ───────────────────────────────────────────────────────

    /** {@code INLINE} | {@code HTTPS} | {@code HTTP} | {@code GRPC} */
    String protocol() default "";

    /** {@code CLOSED} (default) or {@code SUGGESTIONS} */
    ValuesMode mode() default ValuesMode.CLOSED;

    // ── INLINE items ──────────────────────────────────────────────────────────

    /**
     * Static value aliases for INLINE protocol.
     * Each {@literal @}Inline maps to a {@code ValueAlias} in the generated spec.
     * Labels are used as defaults; translations are resolved from the bundle under:
     * {@code {formId}.fields.{fieldName}.items.{value}.label}
     */
    Inline[] items() default {};

    // ── Remote endpoint ───────────────────────────────────────────────────────

    /** Endpoint URI — required for remote protocols (HTTPS / HTTP / GRPC). */
    String uri() default "";

    /** HTTP method: {@code GET} (default) or {@code POST}. */
    String method() default "GET";

    PaginationStrategy pagination() default PaginationStrategy.NONE;

    // ── Typed search parameters ───────────────────────────────────────────────

    /**
     * Describes each search / filter parameter accepted by the endpoint.
     *
     * The processor generates two things from this array:
     * <ul>
     *   <li>{@code searchParamsSchema} — a JSON Schema object describing all params</li>
     *   <li>{@code searchParams}       — a concrete default map (from {@code defaultValue})</li>
     * </ul>
     *
     * Validated at compile time: incompatible attribute combinations emit errors.
     */
    SearchParam[] searchParams() default {};

    // ── Response mapping ──────────────────────────────────────────────────────

    /** Field name in the response body containing the items array. */
    String dataField() default "";

    String totalField()   default "";
    String hasNextField() default "";

    // ── Pagination params ─────────────────────────────────────────────────────

    String pageParam()    default "page";
    String limitParam()   default "limit";
    int    defaultLimit() default 50;

    // ── Performance hints ─────────────────────────────────────────────────────

    /** {@code NONE} | {@code SESSION} | {@code SHORT_TERM} | {@code LONG_TERM} */
    String cacheStrategy()  default "NONE";

    /** Client-side debounce in milliseconds before triggering a search call. */
    int debounceMs()        default 0;

    /** Minimum character count before triggering a search call. */
    int minSearchLength()   default 0;

    // ─── Nested enums ─────────────────────────────────────────────────────────

    enum ValuesMode         { CLOSED, SUGGESTIONS }
    enum PaginationStrategy { NONE, PAGE_NUMBER }
}