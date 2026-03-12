package io.github.cyfko.inputspec.spi;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;

/**
 * Immutable view of a single @FieldMeta-annotated element (field or method)
 * as seen during annotation processing.
 *
 * <p>Passed to {@link FieldTransformer#supports} and {@link FieldTransformer#transform}.
 * All access to annotation mirrors goes through this interface so that
 * transformers never need a direct dependency on the javax.annotation.processing API.
 *
 * <p><b>Design note:</b> annotation qualified names are passed as strings so that
 * a transformer (e.g. in filterql-inputspec-starter) can reference annotations
 * from a third library (e.g. FilterQL's @ExposedAs) without adding it as a
 * compile-time dependency.
 */
public interface FieldContext {

    // ─── Identity ─────────────────────────────────────────────────────────────

    /**
     * The underlying element: a {@code VariableElement} (field) or
     * {@code ExecutableElement} (method).
     */
    Element element();

    /**
     * Logical field name, derived from the element:
     * <ul>
     *   <li>{@code getName()} → {@code "name"}</li>
     *   <li>{@code isActive()} → {@code "active"}</li>
     *   <li>{@code status} (field) → {@code "status"}</li>
     * </ul>
     */
    String fieldName();

    /**
     * The declared type of the field or return type of the method.
     * For {@code List<UserStatus>} this is the full parameterized type.
     */
    TypeMirror fieldType();

    // ─── Annotations ──────────────────────────────────────────────────────────

    /**
     * The resolved @FieldMeta mirror for this element, if present.
     * Absent when the element carries Jakarta constraints but no explicit @FieldMeta.
     */
    Optional<AnnotationMirror> fieldMeta();

    /**
     * Finds the first annotation with the given fully-qualified class name.
     *
     * <p>Use this to access annotations from other libraries without a compile
     * dependency on them:
     * <pre>{@code
     * ctx.findAnnotation("io.github.cyfko.filterql.annotation.ExposedAs")
     * }</pre>
     */
    Optional<AnnotationMirror> findAnnotation(String qualifiedName);

    /**
     * Returns true if the element carries an annotation with this qualified name.
     */
    boolean hasAnnotation(String qualifiedName);

    /**
     * Reads a {@code String} attribute from an annotation mirror.
     * Handles both explicit values and annotation-defined defaults.
     *
     * @param mirror    any annotation mirror accessible via {@link #findAnnotation}
     * @param attribute the annotation method name (e.g. {@code "value"})
     */
    Optional<String> annotationStringValue(AnnotationMirror mirror, String attribute);

    /**
     * Reads a {@code String[]} attribute from an annotation mirror.
     */
    List<String> annotationStringList(AnnotationMirror mirror, String attribute);

    /**
     * Reads an enum array attribute, returning the simple names of the constants.
     *
     * <p>Example: {@code @ExposedAs(operators = {Op.EQ, Op.GT})} → {@code ["EQ", "GT"]}
     *
     * @param mirror    the annotation mirror
     * @param attribute the annotation method name (e.g. {@code "operators"})
     */
    List<String> annotationEnumList(AnnotationMirror mirror, String attribute);

    /**
     * Reads all nested annotation mirrors from an annotation array attribute.
     *
     * <p>Example: reads {@code @ValuesSource.items} → list of {@code @Inline} mirrors.
     */
    List<AnnotationMirror> annotationMirrorList(AnnotationMirror mirror, String attribute);

    // ─── Type utilities ───────────────────────────────────────────────────────

    /**
     * Returns true if the field type is a Java enum.
     */
    boolean isEnum();

    /**
     * If {@link #isEnum()} is true, returns the simple names of all enum constants
     * in declaration order. Otherwise returns an empty list.
     */
    List<String> enumConstants();

    /**
     * Returns true if the field type is a collection (List, Set, Collection)
     * or an array — i.e. {@code expectMultipleValues} should be true in DIFSP.
     */
    boolean isMultiValued();

    /**
     * Returns the DIFSP {@code dataType} string for this field's scalar type:
     * {@code STRING}, {@code NUMBER}, {@code DATE}, {@code BOOLEAN}, {@code OBJECT}.
     *
     * <p>For collections, returns the type of the element (not the container).
     * For enums, returns {@code STRING}.
     */
    String difspDataType();

    /**
     * Returns the format hint for this field type, if applicable.
     * For date/time types: {@code "iso8601"}. Otherwise empty.
     */
    Optional<String> formatHint();

    // ─── Form context ─────────────────────────────────────────────────────────

    /** The form ID declared in {@code @FormSpec}. */
    String formId();

    /**
     * The active locales declared for this form (from {@code @FormSpec} or defaults).
     * First element is the default locale.
     */
    List<String> locales();
}
