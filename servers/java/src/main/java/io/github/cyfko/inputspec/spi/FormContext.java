package io.github.cyfko.inputspec.spi;

import java.util.List;
import java.util.Optional;

/**
 * Immutable view of a partially-assembled FormSpec, passed to
 * {@link FormContributor#supports} and {@link FormContributor#additionalFields}.
 *
 * <p>Provided after all individual fields have been processed —
 * either by the default InputSpec generator or by a {@link FieldTransformer}.
 * A {@link FormContributor} uses this to append synthetic fields and
 * cross-constraints that don't map to any single @FieldMeta element.
 */
public interface FormContext {

    // ─── Form identity ────────────────────────────────────────────────────────

    /** The form ID declared in {@code @FormSpec}. */
    String formId();

    /** The submit URI declared in {@code @FormSpec.submitUri}. */
    String submitUri();

    /** The HTTP method for submission: {@code POST} or {@code PUT}. */
    String submitMethod();

    /** The active locale list. First element is the default locale. */
    List<String> locales();

    // ─── Field inventory ──────────────────────────────────────────────────────

    /**
     * Names of all fields claimed by a {@link FieldTransformer} during processing.
     *
     * <p>A transformer signals a claim by returning a non-null JSON string from
     * {@link FieldTransformer#transform}. The name stored here is the value
     * returned by {@link FieldTransformer#fieldRefName} for that field
     * (e.g. the {@code @ExposedAs.value}: {@code "NAME"}, {@code "AGE"}).
     *
     * <p>A {@link FormContributor} uses this list to:
     * <ul>
     *   <li>Know which filter refs to reference in {@code combineWith}</li>
     *   <li>Decide whether to activate at all (empty → no-op)</li>
     * </ul>
     */
    List<String> transformedFieldRefs();

    /**
     * Logical names (camelCase) of ALL fields in the form,
     * both transformed and default-generated.
     *
     * <p>A {@link FormContributor} uses this for projection suggestions.
     * Example: {@code ["name", "email", "age", "status", "createdAt"]}.
     */
    List<String> allFieldNames();

    /**
     * Returns the generated JSON string for a transformed field, by its ref.
     *
     * <p>Allows a contributor to inspect what was generated — for example,
     * to detect which fields are sortable by examining their DIFSP dataType.
     *
     * @param ref the ref name as returned by {@link FieldTransformer#fieldRefName}
     */
    Optional<String> transformedFieldJson(String ref);

    // ─── Pagination config ────────────────────────────────────────────────────

    /**
     * Default page size hint.
     * Sourced from a companion annotation or a processor option.
     * Defaults to 20.
     */
    int defaultPageSize();

    /**
     * Maximum page size enforced as a {@code maxValue} constraint.
     * Defaults to 100.
     */
    int maxPageSize();
}
