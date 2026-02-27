package io.github.cyfko.inputspec;

import java.lang.annotation.*;

/**
 * Marks a class as a DIFSP FormSpec.
 *
 * The annotation processor reads Jakarta Validation annotations on each field
 * (@NotNull, @Size, @Min, @Max, @Pattern, @Email, @Past, @Future, …)
 * and combines them with @FieldMeta metadata to generate at compile time:
 * <ul>
 *   <li>{@code META-INF/difsp/{formId}.json}        — the FormSpec JSON</li>
 *   <li>{@code META-INF/difsp/i18n/{formId}.properties} — bundle skeleton with default texts</li>
 * </ul>
 *
 * <h3>i18n resolution</h3>
 * All human-readable texts ({@code displayName}, {@code description}, error messages, …)
 * are resolved at runtime by looking up the bundle:
 * <pre>
 *   META-INF/difsp/i18n/{formId}_{locale}.properties
 * </pre>
 * If no entry is found for the requested locale, the {@code displayName} declared
 * in the annotation is used as the ultimate fallback.
 * i18n keys are <b>deduced automatically</b> from the form and field structure:
 * <pre>
 *   {formId}.displayName
 *   {formId}.description
 *   {formId}.fields.{fieldName}.displayName
 *   {formId}.fields.{fieldName}.description
 *   {formId}.crossConstraints.{name}.errorMessage
 *   {formId}.fields.{fieldName}.constraints.{constraintName}.errorMessage
 *   {formId}.fields.{fieldName}.items.{value}.label      (INLINE items)
 * </pre>
 *
 * Usage:
 * <pre>
 * {@literal @}FormSpec(id = "booking-form", displayName = "Booking Form")
 * {@literal @}CrossConstraint(
 *     name         = "dateRange",
 *     type         = CrossConstraintType.FIELD_COMPARISON,
 *     fields       = {"endDate", "startDate"},
 *     operator     = ComparisonOperator.GT,
 *     errorMessage = "End date must be after start date"
 * )
 * public class BookingForm { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface FormSpec {

    /** Stable unique identifier for the form (used as filename and API key). */
    String id();

    /**
     * Default human-readable title.
     * Used as the ultimate fallback when no bundle entry resolves.
     * The bundle key {@code {formId}.displayName} is deduced automatically.
     */
    String displayName() default "";

    /**
     * Default human-readable description.
     * Bundle key: {@code {formId}.description}
     */
    String description() default "";

    /** Submission endpoint URI. Maps to {@code FormSpec.submitEndpoint.uri}. */
    String submitUri() default "";

    /** HTTP method for submission: {@code POST} (default) or {@code PUT}. */
    String submitMethod() default "POST";

    /** Protocol hint for submission: {@code HTTPS} (default), {@code HTTP}, {@code GRPC}. */
    String submitProtocol() default "HTTPS";
}