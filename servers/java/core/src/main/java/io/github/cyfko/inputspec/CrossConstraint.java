package io.github.cyfko.inputspec;

import java.lang.annotation.*;

import io.github.cyfko.inputspec.protocol.ComparisonOperator;
import io.github.cyfko.inputspec.protocol.CrossConstraintType;

/**
 * Declares a cross-field constraint on a {@literal @}FormSpec class.
 * Repeatable: stack multiple {@literal @}CrossConstraint on the same class.
 *
 * Maps to CrossConstraintDescriptor in the protocol spec (§2.10).
 *
 * <p><b>i18n:</b></p>
 * {@code errorMessage} serves as the default fallback text.
 * Translations are resolved from the form bundle under:
 * <pre>
 *   {formId}.crossConstraints.{name}.errorMessage
 *   {formId}.crossConstraints.{name}.description
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(CrossConstraints.class)
@Documented
public @interface CrossConstraint {

    /** Stable unique name within the form. */
    String name();

    CrossConstraintType type();

    /**
     * Names of the involved fields, in order.
     * For FIELD_COMPARISON: [fieldA, fieldB] — rule is "fieldA operator fieldB".
     */
    String[] fields();

    // ── Type-specific params ──────────────────────────────────────────────────

    /** For FIELD_COMPARISON: the comparison operator. */
    ComparisonOperator operator() default ComparisonOperator.GT;

    /** For AT_LEAST_ONE: minimum number of non-empty fields (default 1). */
    int min() default 1;

    /** For MUTUALLY_EXCLUSIVE: maximum number of simultaneously filled fields (default 1). */
    int max() default 1;

    /**
     * For DEPENDS_ON: source field values that trigger the dependency.
     * Empty means "any non-empty value of the source field triggers it".
     */
    String[] sourceValues() default {};

    /** For CUSTOM: the registered handler key. */
    String customKey() default "";

    // ── Human-readable (i18n-aware) ───────────────────────────────────────────

    /**
     * Default error message (shown when no bundle entry resolves).
     * Bundle key: {@code {formId}.crossConstraints.{name}.errorMessage}
     */
    String errorMessage() default "";

    /**
     * Default description / UX hint.
     * Bundle key: {@code {formId}.crossConstraints.{name}.description}
     */
    String description() default "";
}

