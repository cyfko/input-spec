package io.github.cyfko.inputspec.spi;

import java.util.List;

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
 *       both {@link #additionalFields} and {@link #additionalCrossConstraints}
 *       are skipped entirely.</li>
 *   <li>Multiple contributors may activate for the same form. Their contributions
 *       are appended in ServiceLoader order.</li>
 * </ol>
 *
 * <h2>Registration</h2>
 * Register via {@code META-INF/services/io.github.cyfko.inputspec.spi.FormContributor}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class FilterQlFormContributor implements FormContributor {
 *
 *     public boolean supports(FormContext ctx) {
 *         // activate only when FilterQL fields are present
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
     * @return non-null, possibly empty list
     */
    List<String> additionalCrossConstraints(FormContext context);
}
