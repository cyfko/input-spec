package io.github.cyfko.inputspec.spi;

import java.util.Map;

/**
 * SPI extension point: transforms a single @FieldMeta-annotated element
 * into a custom DIFSP {@code InputFieldSpec} JSON object.
 *
 * <h2>Contract</h2>
 * <ol>
 *   <li>The InputSpec processor calls {@link #supports(FieldContext)} for every
 *       @FieldMeta element (field or method) it encounters.</li>
 *   <li>If {@code supports} returns {@code true}, the processor delegates
 *       entirely to {@link #transform(FieldContext)} and skips its own
 *       default field generation for that element.</li>
 *   <li>If multiple transformers claim the same element (both return {@code true}),
 *       the first one in ServiceLoader order wins. A warning is emitted.</li>
 *   <li>If no transformer claims an element, the InputSpec default generation applies.</li>
 * </ol>
 *
 * <h2>i18n — bundle entries</h2>
 * All human-readable texts in the JSON produced by {@link #transform} MUST
 * use the standard DIFSP LocalizedString pattern:
 * <pre>{ "default": "...", "i18nKey": "..." }</pre>
 *
 * The corresponding bundle entries (key → default text) are contributed via
 * {@link #bundleEntries(FieldContext)}. The InputSpec processor merges these
 * entries into the bundle skeleton it writes to
 * {@code META-INF/difsp/i18n/{formId}.properties}.
 *
 * <p>Default implementation returns an empty map. Override when the transformer
 * emits i18nKey references so that the generated bundle skeleton is complete.</p>
 *
 * <h2>Isolation guarantee</h2>
 * A transformer must never import or reference annotations from libraries
 * it bridges (e.g. FilterQL). All annotation access goes through
 * {@link FieldContext#findAnnotation(String)} using qualified name strings.
 * This ensures the transformer JAR has no transitive dependency on the
 * bridged library at compile time.
 *
 * <h2>Registration</h2>
 * Register via {@code META-INF/services/io.github.cyfko.inputspec.spi.FieldTransformer}
 * in the processor JAR (on the {@code annotationProcessorPath}, not {@code dependencies}).
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // FilterQL transformer — activated when @ExposedAs is present
 * public class FilterQlFieldTransformer implements FieldTransformer {
 *
 *     private static final String EXPOSED_AS =
 *         "io.github.cyfko.filterql.spring.ExposedAs";
 *
 *     public boolean supports(FieldContext ctx) {
 *         return ctx.hasAnnotation(EXPOSED_AS);
 *     }
 *
 *     public String fieldRefName(FieldContext ctx) {
 *         return ctx.findAnnotation(EXPOSED_AS)
 *             .flatMap(m -> ctx.annotationStringValue(m, "value"))
 *             .orElse(ctx.fieldName().toUpperCase());
 *     }
 *
 *     public String transform(FieldContext ctx) {
 *         // ... build OBJECT{op, value} JSON using i18nKey ...
 *     }
 *
 *     // Override to populate the bundle skeleton
 *     public Map<String, String> bundleEntries(FieldContext ctx) {
 *         String f = ctx.formId();
 *         String n = ctx.fieldName();
 *         return Map.of(
 *             f + ".fields." + n + ".op.displayName", "Operator",
 *             f + ".fields." + n + ".value.displayName", "Value"
 *             // ...
 *         );
 *     }
 * }
 * }</pre>
 */
public interface FieldTransformer {

    /**
     * Returns {@code true} if this transformer handles the given element.
     *
     * <p>This method must be side-effect-free and fast — it is called for
     * every @FieldMeta element in every @FormSpec class on the compilation
     * classpath.
     */
    boolean supports(FieldContext context);

    /**
     * Returns the stable reference name for this field as it should appear
     * in {@link FormContext#transformedFieldRefs()} and in the generated JSON.
     *
     * <p>For FilterQL: this is the {@code @ExposedAs.value} (e.g. {@code "AGE"}).
     * For other transformers: this could simply be {@code ctx.fieldName()}.
     *
     * <p>Called only when {@link #supports(FieldContext)} returns {@code true}.
     */
    String fieldRefName(FieldContext context);

    /**
     * Produces the DIFSP {@code InputFieldSpec} JSON string for this element.
     *
     * <p>The returned string MUST be a valid, self-contained JSON object
     * conforming to DIFSP v2.1 §2.1. The processor writes it verbatim
     * into the {@code fields} array of the FormSpec — no further transformation.
     *
     * <p>All human-readable text in the JSON MUST use
     * {@code { "default": "...", "i18nKey": "..." }} — never embed locale-keyed
     * objects ({@code "fr": "...", "en": "..."}). Translations belong in the
     * bundle, not in the spec.
     *
     * <p>The {@code "name"} attribute in the returned JSON MUST equal
     * {@link #fieldRefName(FieldContext)} for the same context.
     *
     * @return JSON string, never {@code null} or blank
     */
    String transform(FieldContext context);

    /**
     * Returns the i18n bundle entries (key → default text) for all
     * {@code i18nKey} references emitted by {@link #transform}.
     *
     * <p>The InputSpec processor merges these entries into the bundle skeleton
     * ({@code META-INF/difsp/i18n/{formId}.properties}) so that every
     * {@code i18nKey} in the generated FormSpec has a corresponding entry.
     *
     * <p>Key convention follows InputSpec's standard scheme, extended to
     * sub-fields produced by the transformer:
     * <pre>
     *   {formId}.fields.{fieldName}.displayName
     *   {formId}.fields.{fieldName}.description
     *   {formId}.fields.{fieldName}.op.displayName
     *   {formId}.fields.{fieldName}.op.items.{OP}.label
     *   {formId}.fields.{fieldName}.value.displayName
     *   {formId}.fields.{fieldName}.value.description
     *   {formId}.fields.{fieldName}.value.items.{VALUE}.label
     *   {formId}.fields.{fieldName}.value.constraints.{name}.description
     * </pre>
     *
     * <p>Default implementation returns an empty map — override when
     * the transformer emits {@code i18nKey} references.
     *
     * @return map of bundle key → default text; never {@code null}
     */
    default Map<String, String> bundleEntries(FieldContext context) {
        return Map.of();
    }
}