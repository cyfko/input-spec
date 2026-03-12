package io.github.cyfko.inputspec.spi;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

/**
 * Default implementation of {@link FieldContext}, constructed by the
 * InputSpec annotation processor for each @FieldMeta element it encounters.
 *
 * <p>Encapsulates all javax.lang.model access so that SPI implementors
 * never need to depend on the annotation processing API directly.
 */
public final class DefaultFieldContext implements FieldContext {

    private static final String TYPE_STRING  = "STRING";
    private static final String TYPE_NUMBER  = "NUMBER";
    private static final String TYPE_DATE    = "DATE";
    private static final String TYPE_BOOLEAN = "BOOLEAN";
    private static final String TYPE_OBJECT  = "OBJECT";

    private final Element      element;
    private final String       fieldName;
    private final TypeMirror   fieldType;
    private final String       formId;
    private final List<String> locales;
    private final Types        typeUtils;
    private final Elements     elementUtils;

    // Lazy-computed
    private String  cachedDifspType;
    private Boolean cachedIsEnum;
    private Boolean cachedIsMulti;

    public DefaultFieldContext(Element element,
                               String fieldName,
                               TypeMirror fieldType,
                               String formId,
                               List<String> locales,
                               Types typeUtils,
                               Elements elementUtils) {
        this.element      = element;
        this.fieldName    = fieldName;
        this.fieldType    = fieldType;
        this.formId       = formId;
        this.locales      = Collections.unmodifiableList(new ArrayList<>(locales));
        this.typeUtils    = typeUtils;
        this.elementUtils = elementUtils;
    }

    // ─── Identity ─────────────────────────────────────────────────────────────

    @Override public Element    element()   { return element; }
    @Override public String     fieldName() { return fieldName; }
    @Override public TypeMirror fieldType() { return fieldType; }
    @Override public String     formId()    { return formId; }
    @Override public List<String> locales() { return locales; }

    // ─── Annotations ──────────────────────────────────────────────────────────

    @Override
    public Optional<AnnotationMirror> fieldMeta() {
        return findAnnotation("io.github.cyfko.inputspec.FieldMeta");
    }

    @Override
    public Optional<AnnotationMirror> findAnnotation(String qualifiedName) {
        for (AnnotationMirror mirror : elementUtils.getAllAnnotationMirrors(element)) {
            String name = ((TypeElement) mirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (name.equals(qualifiedName)) return Optional.of(mirror);
        }
        return Optional.empty();
    }

    @Override
    public boolean hasAnnotation(String qualifiedName) {
        return findAnnotation(qualifiedName).isPresent();
    }

    @Override
    public Optional<String> annotationStringValue(AnnotationMirror mirror, String attribute) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> all =
                elementUtils.getElementValuesWithDefaults(mirror);
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : all.entrySet()) {
            if (e.getKey().getSimpleName().toString().equals(attribute)) {
                Object v = e.getValue().getValue();
                return v != null ? Optional.of(v.toString()) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> annotationStringList(AnnotationMirror mirror, String attribute) {
        return collectFromArray(mirror, attribute, v -> v.getValue().toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> annotationEnumList(AnnotationMirror mirror, String attribute) {
        return collectFromArray(mirror, attribute, av -> {
            Object v = av.getValue();
            // Enum constant → VariableElement
            if (v instanceof VariableElement) {
                return ((VariableElement) v).getSimpleName().toString();
            }
            return v.toString();
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnnotationMirror> annotationMirrorList(AnnotationMirror mirror, String attribute) {
        return collectFromArray(mirror, attribute, av -> {
            Object v = av.getValue();
            return (v instanceof AnnotationMirror) ? (AnnotationMirror) v : null;
        });
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> collectFromArray(AnnotationMirror mirror, String attribute,
                                          java.util.function.Function<AnnotationValue, T> extractor) {
        List<T> result = new ArrayList<>();
        Map<? extends ExecutableElement, ? extends AnnotationValue> all =
                elementUtils.getElementValuesWithDefaults(mirror);
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : all.entrySet()) {
            if (!e.getKey().getSimpleName().toString().equals(attribute)) continue;
            Object raw = e.getValue().getValue();
            if (raw instanceof List) {
                for (AnnotationValue av : (List<? extends AnnotationValue>) raw) {
                    T item = extractor.apply(av);
                    if (item != null) result.add(item);
                }
            }
            break;
        }
        return result;
    }

    // ─── Type utilities ───────────────────────────────────────────────────────

    @Override
    public boolean isEnum() {
        if (cachedIsEnum == null) {
            TypeMirror scalar = scalarType(fieldType);
            cachedIsEnum = scalar.getKind() == TypeKind.DECLARED
                    && typeUtils.asElement(scalar).getKind() == ElementKind.ENUM;
        }
        return cachedIsEnum;
    }

    @Override
    public List<String> enumConstants() {
        if (!isEnum()) return Collections.emptyList();
        TypeElement enumEl = (TypeElement) typeUtils.asElement(scalarType(fieldType));
        List<String> constants = new ArrayList<>();
        for (Element e : enumEl.getEnclosedElements()) {
            if (e.getKind() == ElementKind.ENUM_CONSTANT) {
                constants.add(e.getSimpleName().toString());
            }
        }
        return constants;
    }

    @Override
    public boolean isMultiValued() {
        if (cachedIsMulti == null) {
            cachedIsMulti = isCollection(fieldType) || fieldType.getKind() == TypeKind.ARRAY;
        }
        return cachedIsMulti;
    }

    @Override
    public String difspDataType() {
        if (cachedDifspType == null) {
            cachedDifspType = resolveDifspType(scalarType(fieldType));
        }
        return cachedDifspType;
    }

    @Override
    public Optional<String> formatHint() {
        String qn = qualifiedName(scalarType(fieldType));
        if (isDateType(qn)) return Optional.of("iso8601");
        return Optional.empty();
    }

    // ─── Internal type resolution ─────────────────────────────────────────────

    private TypeMirror scalarType(TypeMirror t) {
        if (t.getKind() == TypeKind.ARRAY) return ((ArrayType) t).getComponentType();
        if (isCollection(t)) {
            if (t instanceof DeclaredType) {
                List<? extends TypeMirror> args = ((DeclaredType) t).getTypeArguments();
                if (!args.isEmpty()) return scalarType(args.get(0)); // recurse for Optional<T>
            }
        }
        // Unbox
        try { return typeUtils.unboxedType(t); } catch (IllegalArgumentException e) { return t; }
    }

    private boolean isCollection(TypeMirror t) {
        if (t.getKind() != TypeKind.DECLARED) return false;
        String qn = qualifiedName(t);
        return qn.equals("java.util.List")   || qn.equals("java.util.Set")
            || qn.equals("java.util.Collection") || qn.equals("java.util.Optional")
            || qn.equals("java.util.ArrayList")  || qn.equals("java.util.LinkedList");
    }

    private String resolveDifspType(TypeMirror t) {
        if (t.getKind() == TypeKind.DECLARED
                && typeUtils.asElement(t).getKind() == ElementKind.ENUM) return TYPE_STRING;

        String qn = qualifiedName(t);

        if (matches(qn, "boolean", "java.lang.Boolean"))         return TYPE_BOOLEAN;
        if (isNumeric(qn))                                        return TYPE_NUMBER;
        if (isDateType(qn))                                       return TYPE_DATE;
        if (t.getKind() == TypeKind.DECLARED
                && !qualifiedName(t).startsWith("java.lang"))     return TYPE_OBJECT;
        return TYPE_STRING;
    }

    private boolean matches(String qn, String... candidates) {
        for (String c : candidates) if (qn.equals(c)) return true;
        return false;
    }

    private boolean isNumeric(String qn) {
        return matches(qn, "int","long","double","float","short","byte",
                "java.lang.Integer","java.lang.Long","java.lang.Double",
                "java.lang.Float","java.lang.Short","java.lang.Byte",
                "java.math.BigDecimal","java.math.BigInteger");
    }

    private boolean isDateType(String qn) {
        return matches(qn,
                "java.time.LocalDate","java.time.LocalDateTime","java.time.Instant",
                "java.time.ZonedDateTime","java.time.OffsetDateTime",
                "java.util.Date","java.sql.Date","java.sql.Timestamp");
    }

    private String qualifiedName(TypeMirror t) {
        if (t.getKind() == TypeKind.DECLARED)
            return ((TypeElement) typeUtils.asElement(t)).getQualifiedName().toString();
        return t.toString();
    }
}
