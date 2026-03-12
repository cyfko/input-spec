package io.github.cyfko.inputspec.spi;

import java.util.List;
import java.util.Map;

/**
 * SPI extension point: contributes additional fields and cross-constraints
 * to a FormSpec <em>after</em> all individual elements have been processed.
 *
 * <h2>Purpose</h2>
 * Some fields in a FormSpec don't correspond to any @FieldMeta element —
 * they are synthetic, generated from the set of all processed fields.
 *
 * <p>Canonical example: in a FilterQL search form, {@code combineWith},
 * {@code projection}, and {@code pagination} are not bound to any single
 * method of the DTO interface. They are derived from the full list of
 * transformed filter fields and injected here.
 *
 * <h2>Contract</h2>
 * <ol>
 *   <li>Called once per @FormSpec type, after all FieldTransformers have run.</li>
 *   <li>{@link #supports(FormContext)} is evaluated first. If {@code false},
 *       all other methods are skipped entirely.</li>
 *   <li>Multiple contributors may activate for the same form. Their contributions
 *       are appended in ServiceLoader order.</li>
 * </ol>
 *
 * <h2>i18n — bundle entries</h2>
 * All human-readable texts in the JSON produced by {@link #additionalFields}
 * and {@link #additionalCrossConstraints} MUST use the standard DIFSP
 * LocalizedString pattern:
 * <pre>{ "default": "...", "i18nKey": "..." }</pre>
 *
 * The corresponding bundle entries (key → default text) are contributed via
 * {@link #bundleEntries(FormContext)}. The InputSpec processor merges these
 * entries into the bundle skeleton it writes to
 * {@code META-INF/difsp/i18n/{formId}.properties}.
 *
 * <p>Default implementation returns an empty map. Override when the contributor
 * emits i18nKey references so that the generated bundle skeleton is complete.</p>
 *
 * <h2>Registration</h2>
 * Register via {@code META-INF/services/io.github.cyfko.inputspec.spi.FormContributor}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class FilterQlFormContributor implements FormContributor {
 *
 *     public boolean supports(FormContext ctx) {
 *         return !ctx.transformedFieldRefs().isEmpty();
 *     }
 *
 *     public List<String> additionalFields(FormContext ctx) {
 *         return List.of(
 *             buildCombineWithJson(ctx),
 *             buildProjectionJson(ctx),
 *             buildPaginationJson(ctx)
 *         );
 *     }
 *
 *     public List<String> additionalCrossConstraints(FormContext ctx) {
 *         return List.of(buildRefsExistConstraintJson(ctx));
 *     }
 *
 *     public Map<String, String> bundleEntries(FormContext ctx) {
 *         String f = ctx.formId();
 *         return Map.of(
 *             f + ".fields.combineWith.displayName", "Logical combination",
 *             f + ".fields.projection.displayName",  "Fields to return",
 *             f + ".fields.pagination.displayName",  "Pagination"
 *             // ...
 *         );
 *     }
 * }
 * }</pre>
 */
public interface FormContributor {

    /**
     * Returns {@code true} if this contributor should activate for the given form.
     *
     * <p>Implementations typically inspect {@link FormContext#transformedFieldRefs()}
     * to check whether their companion {@link FieldTransformer} processed any fields.
     */
    boolean supports(FormContext context);

    /**
     * Returns additional {@code InputFieldSpec} JSON strings to append to
     * the {@code fields} array of the FormSpec, in the returned list order.
     *
     * <p>Each string MUST be a valid DIFSP v2.1 §2.1 InputFieldSpec JSON object.
     *
     * <p>All human-readable text MUST use {@code { "default": "...", "i18nKey": "..." }}.
     * Never embed locale-keyed objects ({@code "fr": "...", "en": "..."}).
     *
     * @return non-null, possibly empty list
     */
    List<String> additionalFields(FormContext context);

    /**
     * Returns additional {@code CrossConstraintDescriptor} JSON strings to
     * append to the {@code crossConstraints} array of the FormSpec.
     *
     * <p>Each string MUST be a valid DIFSP v2.1 §2.10 CrossConstraintDescriptor
     * JSON object.
     *
     * <p>All human-readable text MUST use {@code { "default": "...", "i18nKey": "..." }}.
     *
     * @return non-null, possibly empty list
     */
    List<String> additionalCrossConstraints(FormContext context);

    /**
     * Returns the i18n bundle entries (key → default text) for all
     * {@code i18nKey} references emitted by {@link #additionalFields}
     * and {@link #additionalCrossConstraints}.
     *
     * <p>The InputSpec processor merges these entries into the bundle skeleton
     * ({@code META-INF/difsp/i18n/{formId}.properties}) so that every
     * {@code i18nKey} in the generated FormSpec has a corresponding entry.
     *
     * <p>Default implementation returns an empty map — override when
     * the contributor emits {@code i18nKey} references.
     *
     * @return map of bundle key → default text; never {@code null}
     */
    default Map<String, String> bundleEntries(FormContext context) {
        return Map.of();
    }
}