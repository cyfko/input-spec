package io.github.cyfko.inputspec.spi;

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
 *         "io.github.cyfko.filterql.annotation.ExposedAs";
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
 *         // ... build OBJECT{op, value} JSON ...
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
     * <p>The {@code "name"} attribute in the returned JSON MUST equal
     * {@link #fieldRefName(FieldContext)} for the same context.
     *
     * @return JSON string, never {@code null} or blank
     */
    String transform(FieldContext context);
}
