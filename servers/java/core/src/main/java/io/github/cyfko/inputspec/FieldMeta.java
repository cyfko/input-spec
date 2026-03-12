package io.github.cyfko.inputspec;

import java.lang.annotation.*;

/**
 * Enriches a field with DIFSP display metadata and ValuesEndpoint configuration.
 *
 * Jakarta Validation annotations (@NotNull, @Size, @Min, @Max, @Pattern, …)
 * on the same field are automatically mapped to ConstraintDescriptors.
 * This annotation adds the human-readable layer and value source configuration.
 *
 * Fields without @FieldMeta are still included if they carry Jakarta annotations;
 * {@code displayName} defaults to the camel-cased Java field name.
 *
 * <p><b>i18n:</b></p>
 * {@code displayName} and {@code description} serve as default fallback texts.
 * Translations are resolved from:
 * <pre>
 *   META-INF/difsp/i18n/{formId}_{locale}.properties
 * </pre>
 * under the deduced keys:
 * <pre>
 *   {formId}.fields.{fieldName}.displayName
 *   {formId}.fields.{fieldName}.description
 * </pre>
 * No explicit key attribute needed — the key is always deduced from context.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface FieldMeta {

    /**
     * Default human-readable label.
     * Used as the ultimate fallback when no bundle entry resolves.
     * If omitted, the Java field name is converted to a readable label
     * (e.g. {@code assigneeId} → {@code "Assignee Id"}).
     */
    String displayName() default "";

    /**
     * Default human-readable help text / description.
     * Bundle key: {@code {formId}.fields.{fieldName}.description}
     */
    String description() default "";

    /**
     * Non-enforced format hint for UI rendering (e.g. {@code "iso8601"},
     * {@code "phone"}, {@code "email"}, {@code "postal-FR"}).
     * Maps to {@code InputFieldSpec.formatHint} — never validated server-side.
     */
    String formatHint() default "";

    /**
     * Configures the ValuesEndpoint for this field.
     * Leave at default (@ValuesSource with empty protocol) to omit from the spec.
     */
    ValuesSource valuesSource() default @ValuesSource;

    /**
     * Explicit ordering index within the form (0-based).
     * Defaults to field declaration order in the source file.
     */
    int order() default Integer.MAX_VALUE;
}