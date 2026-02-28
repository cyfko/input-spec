package io.github.cyfko.inputspec.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cyfko.inputspec.*;
import io.github.cyfko.inputspec.SearchParam.SchemaType;
import io.github.cyfko.inputspec.protocol.CrossConstraintType;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compile-time processor for {@literal @}FormSpec.
 *
 * <p>For each annotated class it:</p>
 * <ol>
 *   <li>Iterates declared fields in source order</li>
 *   <li>Maps Jakarta Validation annotations → ConstraintDescriptors</li>
 *   <li>Maps @FieldMeta → display metadata + ValuesEndpoint</li>
 *   <li>Generates searchParamsSchema JSON from @SearchParam[]</li>
 *   <li>Maps @Inline[] → ValueAlias items</li>
 *   <li>Maps nested @FormSpec classes → OBJECT sub-fields (recursive)</li>
 *   <li>Maps @CrossConstraint → CrossConstraintDescriptors</li>
 *   <li>Writes META-INF/difsp/{formId}.json</li>
 *   <li>Writes META-INF/difsp/i18n/{formId}.properties (bundle skeleton)</li>
 * </ol>
 *
 * <p>JSON is built programmatically via Jackson {@code ObjectNode} for
 * type-safety and proper escaping.</p>
 */
@SupportedAnnotationTypes("io.github.cyfko.inputspec.FormSpec")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class FormSpecProcessor extends AbstractProcessor {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private Types    typeUtils;
    private Elements elemUtils;
    private Filer    filer;
    private Messager messager;

    // Jakarta Validation annotation FQNs
    private static final String NOT_NULL          = "jakarta.validation.constraints.NotNull";
    private static final String NOT_EMPTY         = "jakarta.validation.constraints.NotEmpty";
    private static final String NOT_BLANK         = "jakarta.validation.constraints.NotBlank";
    private static final String SIZE              = "jakarta.validation.constraints.Size";
    private static final String MIN               = "jakarta.validation.constraints.Min";
    private static final String MAX               = "jakarta.validation.constraints.Max";
    private static final String DECIMAL_MIN       = "jakarta.validation.constraints.DecimalMin";
    private static final String DECIMAL_MAX       = "jakarta.validation.constraints.DecimalMax";
    private static final String PATTERN           = "jakarta.validation.constraints.Pattern";
    private static final String EMAIL             = "jakarta.validation.constraints.Email";
    private static final String PAST              = "jakarta.validation.constraints.Past";
    private static final String PAST_OR_PRESENT   = "jakarta.validation.constraints.PastOrPresent";
    private static final String FUTURE            = "jakarta.validation.constraints.Future";
    private static final String FUTURE_OR_PRESENT = "jakarta.validation.constraints.FutureOrPresent";
    private static final String POSITIVE          = "jakarta.validation.constraints.Positive";
    private static final String NEGATIVE          = "jakarta.validation.constraints.Negative";

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        typeUtils = env.getTypeUtils();
        elemUtils = env.getElementUtils();
        filer     = env.getFiler();
        messager  = env.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(FormSpec.class)) {
            if (element.getKind() != ElementKind.CLASS) continue;
            try {
                processTopLevelForm((TypeElement) element);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "DIFSP: Failed to process @FormSpec on " + element + ": " + e.getMessage()
                    + "\n" + sw,
                    element);
            }
        }
        return true;
    }

    // ─── Top-level form processing ───────────────────────────────────────────

    private void processTopLevelForm(TypeElement clazz) throws IOException {
        FormSpec formSpec = clazz.getAnnotation(FormSpec.class);
        String formId = formSpec.id();

        Map<String, String> bundle = new LinkedHashMap<>();
        ObjectNode root = buildFormNode(clazz, formSpec, formId, bundle);

        // Write JSON spec
        String json = MAPPER.writeValueAsString(root);
        writeResource("META-INF/difsp/" + formId + ".json", json, clazz);

        // Write bundle skeleton
        writeBundleSkeleton(formId, bundle, clazz);

        messager.printMessage(Diagnostic.Kind.NOTE,
            "DIFSP: Generated spec + bundle skeleton for form '" + formId + "'");
    }

    // ─── Form node building ──────────────────────────────────────────────────

    private ObjectNode buildFormNode(TypeElement clazz, FormSpec formSpec,
                                     String formId, Map<String, String> bundle) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("id", formId);

        // displayName
        addBundleEntry(bundle, formId + ".displayName", formSpec.displayName());
        root.set("displayName", i18nNode(formSpec.displayName(), formId + ".displayName"));

        // description
        if (!formSpec.description().isEmpty()) {
            addBundleEntry(bundle, formId + ".description", formSpec.description());
            root.set("description", i18nNode(formSpec.description(), formId + ".description"));
        }

        // fields
        ArrayNode fieldsArray = root.putArray("fields");
        List<VariableElement> fields = collectFields(clazz);
        for (VariableElement field : fields) {
            fieldsArray.add(buildFieldNode(field, formId, bundle));
        }

        // crossConstraints
        ArrayNode ccArray = root.putArray("crossConstraints");
        for (CrossConstraint cc : collectCrossConstraints(clazz)) {
            ccArray.add(buildCrossConstraintNode(cc, formId, bundle));
        }

        // submitEndpoint
        if (!formSpec.submitUri().isEmpty()) {
            ObjectNode submit = root.putObject("submitEndpoint");
            submit.put("protocol", formSpec.submitProtocol());
            submit.put("uri", formSpec.submitUri());
            submit.put("method", formSpec.submitMethod());
        }

        return root;
    }

    // ─── Field node building ─────────────────────────────────────────────────

    private ObjectNode buildFieldNode(VariableElement field, String formId,
                                       Map<String, String> bundle) {
        String fieldName = field.getSimpleName().toString();

        // Use AnnotationMirror API (not getAnnotation()) to avoid proxy creation
        // failures with nested annotation default values (@ValuesSource)
        AnnotationMirror metaMirror = findAnnotationMirror(field, "io.github.cyfko.inputspec.FieldMeta");
        Map<String, AnnotationValue> metaVals = metaMirror != null
            ? getAnnotationValues(metaMirror) : Map.of();

        ObjectNode node = MAPPER.createObjectNode();
        node.put("name", fieldName);

        // displayName
        String displayName = getMirrorString(metaVals, "displayName", "");
        if (displayName.isEmpty()) displayName = camelToLabel(fieldName);
        String dnKey = formId + ".fields." + fieldName + ".displayName";
        addBundleEntry(bundle, dnKey, displayName);
        node.set("displayName", i18nNode(displayName, dnKey));

        // description
        String description = getMirrorString(metaVals, "description", "");
        if (!description.isEmpty()) {
            String dKey = formId + ".fields." + fieldName + ".description";
            addBundleEntry(bundle, dKey, description);
            node.set("description", i18nNode(description, dKey));
        }

        // dataType + expectMultipleValues
        FieldTypeInfo typeInfo = resolveFieldType(field);
        node.put("dataType", typeInfo.dataType);
        node.put("expectMultipleValues", typeInfo.multiple);

        // required
        boolean required = isRequired(field);
        node.put("required", required);

        // formatHint
        String formatHint = getMirrorString(metaVals, "formatHint", "");
        if (!formatHint.isEmpty()) {
            node.put("formatHint", formatHint);
        }

        // valuesEndpoint — from @ValuesSource inside @FieldMeta
        boolean hasExplicitValues = false;
        if (metaMirror != null) {
            AnnotationValue vsValue = metaVals.get("valuesSource");
            if (vsValue != null && vsValue.getValue() instanceof AnnotationMirror vsMirror) {
                Map<String, AnnotationValue> vsVals = getAnnotationValues(vsMirror);
                String protocol = getMirrorString(vsVals, "protocol", "");
                if (!protocol.isEmpty()) {
                    node.set("valuesEndpoint",
                        buildValuesEndpointFromMirror(vsMirror, vsVals, fieldName, formId, bundle));
                    hasExplicitValues = true;
                }
            }
        }

        // Enum auto-detection → INLINE CLOSED ValuesEndpoint
        if (!hasExplicitValues && typeInfo.enumElement != null) {
            node.set("valuesEndpoint",
                buildEnumValuesEndpointNode(typeInfo.enumElement, fieldName, formId, bundle));
        }

        // OBJECT sub-fields (recursive)
        if ("OBJECT".equals(typeInfo.dataType) && typeInfo.nestedClass != null) {
            ArrayNode subFields = node.putArray("subFields");
            List<VariableElement> nested = collectFields(typeInfo.nestedClass);
            for (VariableElement sub : nested) {
                subFields.add(buildFieldNode(sub, formId, bundle));
            }
        }

        // constraints
        ArrayNode constraints = node.putArray("constraints");
        buildConstraintNodes(field, typeInfo.dataType, fieldName, formId, bundle, constraints);

        return node;
    }

    // ─── ValuesEndpoint node building ────────────────────────────────────────

    private ObjectNode buildValuesEndpointNode(ValuesSource vs, String fieldName,
                                                String formId, Map<String, String> bundle) {
        ObjectNode ve = MAPPER.createObjectNode();
        ve.put("protocol", vs.protocol());
        ve.put("mode", vs.mode().name());

        if ("INLINE".equalsIgnoreCase(vs.protocol())) {
            // ── @Inline[] → items array ────────────────────────────────────
            Inline[] items = vs.items();
            if (items.length == 0) {
                messager.printMessage(Diagnostic.Kind.WARNING,
                    "DIFSP: INLINE protocol without items on field '" + fieldName + "'");
            }
            ArrayNode itemsArray = ve.putArray("items");
            for (Inline item : items) {
                String labelKey = formId + ".fields." + fieldName
                                + ".items." + item.value() + ".label";
                addBundleEntry(bundle, labelKey, item.label());

                ObjectNode alias = MAPPER.createObjectNode();
                alias.put("value", item.value());
                alias.set("label", i18nNode(item.label(), labelKey));
                itemsArray.add(alias);
            }
        } else {
            // ── Remote endpoint ────────────────────────────────────────────
            ve.put("uri", vs.uri());
            ve.put("method", vs.method());
            ve.put("paginationStrategy", vs.pagination().name());

            // ── @SearchParam[] → searchParamsSchema + searchParams ─────────
            SearchParam[] searchParams = vs.searchParams();
            if (searchParams.length > 0) {
                validateSearchParams(searchParams, fieldName);
                ve.set("searchParamsSchema", buildSearchParamsSchemaNode(searchParams));

                // Concrete searchParams defaults map
                ObjectNode defaults = buildSearchParamsDefaultsNode(searchParams);
                if (defaults != null) {
                    ve.set("searchParams", defaults);
                }
            }

            // ── Response mapping ───────────────────────────────────────────
            if (!vs.dataField().isEmpty()) {
                ObjectNode responseMapping = ve.putObject("responseMapping");
                responseMapping.put("dataField", vs.dataField());
                if (!vs.totalField().isEmpty())
                    responseMapping.put("totalField", vs.totalField());
                if (!vs.hasNextField().isEmpty())
                    responseMapping.put("hasNextField", vs.hasNextField());
            }

            // ── requestParams ──────────────────────────────────────────────
            ObjectNode reqParams = ve.putObject("requestParams");
            reqParams.put("pageParam", vs.pageParam());
            reqParams.put("limitParam", vs.limitParam());
            reqParams.put("defaultLimit", vs.defaultLimit());

            // ── Performance hints ──────────────────────────────────────────
            ve.put("cacheStrategy", vs.cacheStrategy());
            if (vs.debounceMs() > 0)     ve.put("debounceMs", vs.debounceMs());
            if (vs.minSearchLength() > 0) ve.put("minSearchLength", vs.minSearchLength());
        }

        return ve;
    }

    // ─── Enum auto-detection → INLINE CLOSED ─────────────────────────────────

    /**
     * Builds an INLINE CLOSED ValuesEndpoint from a Java enum's constants.
     * Each constant becomes a ValueAlias: value = constant name, label = readable form.
     */
    private ObjectNode buildEnumValuesEndpointNode(TypeElement enumType,
                                                    String fieldName, String formId,
                                                    Map<String, String> bundle) {
        ObjectNode ve = MAPPER.createObjectNode();
        ve.put("protocol", "INLINE");
        ve.put("mode", "CLOSED");

        ArrayNode itemsArray = ve.putArray("items");
        for (Element enclosed : enumType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.ENUM_CONSTANT) {
                String constName = enclosed.getSimpleName().toString();
                String label     = camelToLabel(constName.charAt(0)
                    + constName.substring(1).toLowerCase().replace("_", " "));
                String labelKey  = formId + ".fields." + fieldName
                                 + ".items." + constName + ".label";
                addBundleEntry(bundle, labelKey, label);

                ObjectNode alias = MAPPER.createObjectNode();
                alias.put("value", constName);
                alias.set("label", i18nNode(label, labelKey));
                itemsArray.add(alias);
            }
        }

        return ve;
    }

    // ─── @SearchParam[] → JSON Schema node ───────────────────────────────────

    private ObjectNode buildSearchParamsSchemaNode(SearchParam[] params) {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ArrayNode requiredArray = MAPPER.createArrayNode();

        for (SearchParam p : params) {
            ObjectNode prop = properties.putObject(p.name());
            prop.put("type", p.type().jsonValue());

            if (!p.description().isEmpty())
                prop.put("description", p.description());

            if (p.enumValues().length > 0) {
                ArrayNode enumArr = prop.putArray("enum");
                for (String ev : p.enumValues()) enumArr.add(ev);
            }
            if (!p.minimum().isEmpty())
                prop.put("minimum", Double.parseDouble(p.minimum()));
            if (!p.maximum().isEmpty())
                prop.put("maximum", Double.parseDouble(p.maximum()));
            if (!p.format().isEmpty())
                prop.put("format", p.format());

            if (p.required()) requiredArray.add(p.name());
        }

        if (!requiredArray.isEmpty()) {
            schema.set("required", requiredArray);
        }

        return schema;
    }

    private ObjectNode buildSearchParamsDefaultsNode(SearchParam[] params) {
        ObjectNode defaults = MAPPER.createObjectNode();
        boolean hasDefaults = false;
        for (SearchParam p : params) {
            if (!p.defaultValue().isEmpty()) {
                defaults.put(p.name(), p.defaultValue());
                hasDefaults = true;
            }
        }
        return hasDefaults ? defaults : null;
    }

    // ─── Compile-time validation of @SearchParam ─────────────────────────────

    private void validateSearchParams(SearchParam[] params, String fieldName) {
        for (SearchParam p : params) {
            if (p.enumValues().length > 0 && p.type() != SchemaType.STRING) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "DIFSP: @SearchParam '" + p.name()
                    + "' on field '" + fieldName
                    + "': enumValues is only valid with type = STRING");
            }
            boolean numeric = p.type() == SchemaType.NUMBER || p.type() == SchemaType.INTEGER;
            if (!p.minimum().isEmpty() && !numeric) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "DIFSP: @SearchParam '" + p.name()
                    + "' on field '" + fieldName
                    + "': minimum is only valid with type = NUMBER or INTEGER");
            }
            if (!p.maximum().isEmpty() && !numeric) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "DIFSP: @SearchParam '" + p.name()
                    + "' on field '" + fieldName
                    + "': maximum is only valid with type = NUMBER or INTEGER");
            }
        }
    }

    // ─── Constraint building from Jakarta annotations ────────────────────────

    private void buildConstraintNodes(VariableElement field, String dataType,
                                       String fieldName, String formId,
                                       Map<String, String> bundle, ArrayNode constraints) {
        for (AnnotationMirror ann : field.getAnnotationMirrors()) {
            String annName = ann.getAnnotationType().toString();
            Map<String, AnnotationValue> vals = getAnnotationValues(ann);

            switch (annName) {
                case SIZE -> {
                    int min = getInt(vals, "min", 0);
                    int max = getInt(vals, "max", Integer.MAX_VALUE);
                    String msg = getConstraintMessage(vals, fieldName, "size", formId, bundle);
                    if (min > 0)
                        constraints.add(constraintNode("size-min", "minLength",
                            paramsWithValue(min), msg));
                    if (max < Integer.MAX_VALUE)
                        constraints.add(constraintNode("size-max", "maxLength",
                            paramsWithValue(max), msg));
                }
                case MIN -> {
                    long val = getLong(vals, "value", 0);
                    String msg = getConstraintMessage(vals, fieldName, "min", formId, bundle);
                    constraints.add(constraintNode("min", "minValue",
                        paramsWithValue(val), msg));
                }
                case MAX -> {
                    long val = getLong(vals, "value", Long.MAX_VALUE);
                    String msg = getConstraintMessage(vals, fieldName, "max", formId, bundle);
                    constraints.add(constraintNode("max", "maxValue",
                        paramsWithValue(val), msg));
                }
                case DECIMAL_MIN -> {
                    String val = getString(vals, "value", "0");
                    String msg = getConstraintMessage(vals, fieldName, "decimal-min", formId, bundle);
                    constraints.add(constraintNode("decimal-min", "minValue",
                        paramsWithStringValue(val), msg));
                }
                case DECIMAL_MAX -> {
                    String val = getString(vals, "value", "0");
                    String msg = getConstraintMessage(vals, fieldName, "decimal-max", formId, bundle);
                    constraints.add(constraintNode("decimal-max", "maxValue",
                        paramsWithStringValue(val), msg));
                }
                case PATTERN -> {
                    String regex = getString(vals, "regexp", "");
                    String msg = getConstraintMessage(vals, fieldName, "pattern", formId, bundle);
                    ObjectNode params = MAPPER.createObjectNode();
                    params.put("regex", regex);
                    // flags are an array of javax.validation.constraints.Pattern.Flag
                    // — for now emit without flags; the processor could be enhanced later
                    constraints.add(constraintNode("pattern", "pattern", params, msg));
                }
                case EMAIL -> {
                    String msg = getConstraintMessage(vals, fieldName, "email", formId, bundle);
                    ObjectNode params = MAPPER.createObjectNode();
                    params.put("regex", "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
                    constraints.add(constraintNode("email", "pattern", params, msg));
                }
                case POSITIVE -> {
                    String msg = getConstraintMessage(vals, fieldName, "positive", formId, bundle);
                    constraints.add(constraintNode("positive", "minValue",
                        paramsWithValue(1), msg));
                }
                case NEGATIVE -> {
                    String msg = getConstraintMessage(vals, fieldName, "negative", formId, bundle);
                    constraints.add(constraintNode("negative", "maxValue",
                        paramsWithValue(-1), msg));
                }
                case PAST, PAST_OR_PRESENT -> {
                    String msg = getConstraintMessage(vals, fieldName, "past", formId, bundle);
                    ObjectNode params = MAPPER.createObjectNode();
                    params.put("iso", "$NOW");
                    constraints.add(constraintNode("past", "maxDate", params, msg));
                }
                case FUTURE, FUTURE_OR_PRESENT -> {
                    String msg = getConstraintMessage(vals, fieldName, "future", formId, bundle);
                    ObjectNode params = MAPPER.createObjectNode();
                    params.put("iso", "$NOW");
                    constraints.add(constraintNode("future", "minDate", params, msg));
                }
                // @NotNull / @NotEmpty / @NotBlank → required:true (handled separately)
            }
        }
    }

    private ObjectNode constraintNode(String name, String type,
                                       ObjectNode params, String errorMessage) {
        ObjectNode c = MAPPER.createObjectNode();
        c.put("name", name);
        c.put("type", type);
        c.set("params", params);
        if (errorMessage != null) {
            // If we have a bundle-ready message, wrap as i18n
            c.put("errorMessage", errorMessage);
        }
        return c;
    }

    private String getConstraintMessage(Map<String, AnnotationValue> vals,
                                         String fieldName, String constraintName,
                                         String formId, Map<String, String> bundle) {
        String raw = getString(vals, "message", "");
        // Jakarta default messages look like {jakarta.validation.constraints.*.message} — skip
        if (raw.isEmpty() || (raw.startsWith("{") && raw.contains("."))) return null;
        String key = formId + ".fields." + fieldName + ".constraints."
                   + constraintName + ".errorMessage";
        addBundleEntry(bundle, key, raw);
        return raw;
    }

    // ─── CrossConstraint node building ────────────────────────────────────────

    private ObjectNode buildCrossConstraintNode(CrossConstraint cc, String formId,
                                                 Map<String, String> bundle) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("name", cc.name());
        node.put("type", crossConstraintTypeJson(cc.type()));

        ArrayNode fieldsArr = node.putArray("fields");
        for (String f : cc.fields()) fieldsArr.add(f);

        // params (type-specific)
        ObjectNode params = MAPPER.createObjectNode();
        switch (cc.type()) {
            case FIELD_COMPARISON -> params.put("operator", cc.operator().jsonValue);
            case AT_LEAST_ONE    -> params.put("min", cc.min());
            case MUTUALLY_EXCLUSIVE -> params.put("max", cc.max());
            case DEPENDS_ON -> {
                if (cc.sourceValues().length > 0) {
                    ArrayNode sv = params.putArray("sourceValues");
                    for (String v : cc.sourceValues()) sv.add(v);
                }
            }
            case CUSTOM -> {
                if (!cc.customKey().isEmpty()) params.put("key", cc.customKey());
            }
        }
        node.set("params", params);

        // errorMessage
        if (!cc.errorMessage().isEmpty()) {
            String errKey = formId + ".crossConstraints." + cc.name() + ".errorMessage";
            addBundleEntry(bundle, errKey, cc.errorMessage());
            node.set("errorMessage", i18nNode(cc.errorMessage(), errKey));
        }

        // description
        if (!cc.description().isEmpty()) {
            String descKey = formId + ".crossConstraints." + cc.name() + ".description";
            addBundleEntry(bundle, descKey, cc.description());
            node.set("description", i18nNode(cc.description(), descKey));
        }

        return node;
    }

    private String crossConstraintTypeJson(CrossConstraintType type) {
        return switch (type) {
            case FIELD_COMPARISON   -> "fieldComparison";
            case AT_LEAST_ONE       -> "atLeastOne";
            case MUTUALLY_EXCLUSIVE -> "mutuallyExclusive";
            case DEPENDS_ON         -> "dependsOn";
            case CUSTOM             -> "custom";
            case UNKNOWN            -> "unknown";
        };
    }

    // ─── i18n helpers ─────────────────────────────────────────────────────────

    /**
     * Creates a LocalizedString node: {@code {"default": "text", "i18nKey": "key"}}
     */
    private ObjectNode i18nNode(String defaultText, String i18nKey) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("default", defaultText);
        node.put("i18nKey", i18nKey);
        return node;
    }

    // ─── Bundle skeleton writer ───────────────────────────────────────────────

    private void writeBundleSkeleton(String formId, Map<String, String> entries,
                                      TypeElement origin) throws IOException {
        String path = "META-INF/difsp/i18n/" + formId + ".properties";
        FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", path, origin);
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(
                file.openOutputStream(), StandardCharsets.UTF_8))) {

            w.println("# DIFSP bundle skeleton for form: " + formId);
            w.println("# Generated at compile time — translate values for each target locale.");
            w.println("# Save translated versions as: " + formId + "_fr.properties, "
                      + formId + "_es.properties, ...");
            w.println("# Keys are deduced automatically from the form structure.");
            w.println("#");
            w.println("# Resolution order at runtime:");
            w.println("#   META-INF/difsp/i18n/{formId}_{locale}.properties");
            w.println("#   → locale language fallback  (e.g. fr-CA → fr)");
            w.println("#   → default text declared in the annotation");
            w.println();

            String lastSection = "";
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String section = entry.getKey().replaceAll("\\.[^.]+$", "");
                if (!section.equals(lastSection)) {
                    if (!lastSection.isEmpty()) w.println();
                    lastSection = section;
                }
                w.println(entry.getKey() + " = " + entry.getValue());
            }
        }
    }

    // ─── Resource writer ─────────────────────────────────────────────────────

    private void writeResource(String path, String content,
                                TypeElement origin) throws IOException {
        FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", path, origin);
        try (Writer w = file.openWriter()) { w.write(content); }
    }

    // ─── Type resolution ─────────────────────────────────────────────────────

    private FieldTypeInfo resolveFieldType(VariableElement field) {
        TypeMirror type    = field.asType();
        boolean    multiple = false;
        TypeElement nestedClass = null;

        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType dt  = (DeclaredType) type;
            Element      rawElem = typeUtils.asElement(typeUtils.erasure(dt));
            String       raw = (rawElem != null) ? rawElem.toString()
                                                 : typeUtils.erasure(dt).toString();
            if (raw.equals("java.util.List") || raw.equals("java.util.Set")
                    || raw.equals("java.util.Collection")
                    || raw.equals("List") || raw.equals("Set")
                    || raw.equals("Collection")) {
                multiple = true;
                if (!dt.getTypeArguments().isEmpty()) type = dt.getTypeArguments().get(0);
            }
        }

        String dataType = mapJavaTypeToDataType(type);
        TypeElement enumElement = null;

        if (type.getKind() == TypeKind.DECLARED) {
            TypeElement te = (TypeElement) typeUtils.asElement(type);
            if (te != null) {
                if (te.getKind() == ElementKind.ENUM) {
                    // Java enum → STRING + auto INLINE
                    dataType = "STRING";
                    enumElement = te;
                } else if ("OBJECT".equals(dataType) && te.getAnnotation(FormSpec.class) != null) {
                    nestedClass = te;
                }
            }
        }
        return new FieldTypeInfo(dataType, multiple, nestedClass, enumElement);
    }

    private String mapJavaTypeToDataType(TypeMirror type) {
        // For declared types, use asElement() to get the clean qualified name
        // because erasure().toString() may include type-use annotations
        // (e.g. "java.lang.@jakarta.validation.constraints.NotNull String")
        String name;
        if (type.getKind() == TypeKind.DECLARED) {
            Element elem = typeUtils.asElement(type);
            name = (elem != null) ? elem.toString() : typeUtils.erasure(type).toString();
        } else {
            name = type.toString();
        }
        return switch (name) {
            case "String", "java.lang.String",
                 "CharSequence", "java.lang.CharSequence" -> "STRING";
            case "int", "long", "float", "double", "short", "byte",
                 "Integer", "Long", "Float", "Double", "Short", "Byte",
                 "java.lang.Integer", "java.lang.Long",
                 "java.lang.Float", "java.lang.Double",
                 "java.lang.Short", "java.lang.Byte",
                 "java.math.BigDecimal", "java.math.BigInteger",
                 "BigDecimal", "BigInteger" -> "NUMBER";
            case "boolean", "Boolean", "java.lang.Boolean" -> "BOOLEAN";
            case "LocalDate", "LocalDateTime", "ZonedDateTime", "OffsetDateTime",
                 "java.time.LocalDate", "java.time.LocalDateTime",
                 "java.time.ZonedDateTime", "java.time.OffsetDateTime",
                 "Date", "java.util.Date", "java.sql.Date" -> "DATE";
            default -> "OBJECT";
        };
    }

    // ─── Field/annotation helpers ────────────────────────────────────────────

    private List<VariableElement> collectFields(TypeElement clazz) {
        return clazz.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.FIELD
                      && !e.getModifiers().contains(Modifier.STATIC))
            .map(e -> (VariableElement) e)
            .sorted(Comparator.comparingInt(f -> {
                AnnotationMirror m = findAnnotationMirror(f, "io.github.cyfko.inputspec.FieldMeta");
                if (m == null) return Integer.MAX_VALUE;
                Map<String, AnnotationValue> vals = getAnnotationValues(m);
                return getInt(vals, "order", Integer.MAX_VALUE);
            }))
            .collect(Collectors.toList());
    }

    private List<CrossConstraint> collectCrossConstraints(TypeElement clazz) {
        CrossConstraints container = clazz.getAnnotation(CrossConstraints.class);
        if (container != null) return Arrays.asList(container.value());
        CrossConstraint single = clazz.getAnnotation(CrossConstraint.class);
        return single != null ? List.of(single) : List.of();
    }

    private boolean isRequired(VariableElement field) {
        return field.getAnnotationMirrors().stream()
            .map(a -> a.getAnnotationType().toString())
            .anyMatch(n -> n.equals(NOT_NULL) || n.equals(NOT_EMPTY) || n.equals(NOT_BLANK));
    }

    // ─── Params node helpers ─────────────────────────────────────────────────

    private ObjectNode paramsWithValue(long value) {
        ObjectNode p = MAPPER.createObjectNode();
        p.put("value", value);
        return p;
    }

    private ObjectNode paramsWithStringValue(String value) {
        ObjectNode p = MAPPER.createObjectNode();
        p.put("value", value);
        return p;
    }

    // ─── AnnotationMirror value extraction ───────────────────────────────────

    private Map<String, AnnotationValue> getAnnotationValues(AnnotationMirror ann) {
        Map<String, AnnotationValue> result = new HashMap<>();
        // Include defaults from elemUtils
        Map<? extends ExecutableElement, ? extends AnnotationValue> allValues =
            elemUtils.getElementValuesWithDefaults(ann);
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : allValues.entrySet()) {
            result.put(e.getKey().getSimpleName().toString(), e.getValue());
        }
        return result;
    }

    private int getInt(Map<String, AnnotationValue> m, String key, int def) {
        AnnotationValue v = m.get(key);
        if (v == null) return def;
        Object val = v.getValue();
        if (val instanceof Number n) return n.intValue();
        return def;
    }

    private long getLong(Map<String, AnnotationValue> m, String key, long def) {
        AnnotationValue v = m.get(key);
        if (v == null) return def;
        Object val = v.getValue();
        if (val instanceof Number n) return n.longValue();
        return def;
    }

    /** Alias for annotation mirror string access (same as getMirrorString). */
    private String getString(Map<String, AnnotationValue> m, String key, String def) {
        AnnotationValue v = m.get(key);
        if (v == null) return def;
        return v.getValue().toString();
    }

    // ─── Bundle helpers ──────────────────────────────────────────────────────

    private void addBundleEntry(Map<String, String> bundle, String key, String value) {
        if (value != null && !value.isEmpty()) bundle.put(key, value);
    }

    // ─── String helpers ──────────────────────────────────────────────────────

    private String camelToLabel(String name) {
        return name.substring(0, 1).toUpperCase()
            + name.substring(1).replaceAll("([A-Z])", " $1");
    }

    // ─── AnnotationMirror lookup helpers ──────────────────────────────────────

    /** Finds an annotation mirror on an element by its fully-qualified type name. */
    private AnnotationMirror findAnnotationMirror(Element element, String annotationFqn) {
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            if (am.getAnnotationType().toString().equals(annotationFqn)) return am;
        }
        return null;
    }

    /** Reads a String attribute from a mirror value map, with a default. */
    private String getMirrorString(Map<String, AnnotationValue> vals, String key, String def) {
        AnnotationValue v = vals.get(key);
        if (v == null) return def;
        Object raw = v.getValue();
        return (raw != null) ? raw.toString() : def;
    }

    // ─── ValuesEndpoint from AnnotationMirror ────────────────────────────────

    /**
     * Builds a ValuesEndpoint JSON node from the @ValuesSource AnnotationMirror.
     * This avoids proxy creation failures that occur with getAnnotation().
     */
    @SuppressWarnings("unchecked")
    private ObjectNode buildValuesEndpointFromMirror(AnnotationMirror vsMirror,
                                                     Map<String, AnnotationValue> vsVals,
                                                     String fieldName, String formId,
                                                     Map<String, String> bundle) {
        ObjectNode ve = MAPPER.createObjectNode();
        String protocol = getMirrorString(vsVals, "protocol", "");
        ve.put("protocol", protocol);

        // mode — enum value
        AnnotationValue modeVal = vsVals.get("mode");
        String mode = (modeVal != null) ? modeVal.getValue().toString() : "CLOSED";
        ve.put("mode", mode);

        if ("INLINE".equalsIgnoreCase(protocol)) {
            // ── @Inline[] items from mirror ───────────────────────────────
            ArrayNode itemsArray = ve.putArray("items");
            AnnotationValue itemsVal = vsVals.get("items");
            if (itemsVal != null && itemsVal.getValue() instanceof List<?> itemList) {
                if (itemList.isEmpty()) {
                    messager.printMessage(Diagnostic.Kind.WARNING,
                        "DIFSP: INLINE protocol without items on field '" + fieldName + "'");
                }
                for (Object itemObj : itemList) {
                    AnnotationMirror inlineMirror = extractMirror(itemObj);
                    if (inlineMirror == null) continue;
                    Map<String, AnnotationValue> inVals = getAnnotationValues(inlineMirror);
                    String value = getMirrorString(inVals, "value", "");
                    String label = getMirrorString(inVals, "label", value);
                    String labelKey = formId + ".fields." + fieldName
                                    + ".items." + value + ".label";
                    addBundleEntry(bundle, labelKey, label);

                    ObjectNode alias = MAPPER.createObjectNode();
                    alias.put("value", value);
                    alias.set("label", i18nNode(label, labelKey));
                    itemsArray.add(alias);
                }
            }
        } else {
            // ── Remote endpoint ───────────────────────────────────────────
            ve.put("uri", getMirrorString(vsVals, "uri", ""));
            ve.put("method", getMirrorString(vsVals, "method", "GET"));

            AnnotationValue pagVal = vsVals.get("pagination");
            String pagination = (pagVal != null) ? pagVal.getValue().toString() : "NONE";
            ve.put("paginationStrategy", pagination);

            // ── @SearchParam[] from mirror ────────────────────────────────
            AnnotationValue spVal = vsVals.get("searchParams");
            if (spVal != null && spVal.getValue() instanceof List<?> spList && !spList.isEmpty()) {
                ve.set("searchParamsSchema", buildSearchSchemaFromMirrorList(spList));
                ObjectNode defaults = buildSearchDefaultsFromMirrorList(spList);
                if (defaults != null) ve.set("searchParams", defaults);
            }

            // ── Response mapping ──────────────────────────────────────────
            String dataField = getMirrorString(vsVals, "dataField", "");
            if (!dataField.isEmpty()) {
                ObjectNode responseMapping = ve.putObject("responseMapping");
                responseMapping.put("dataField", dataField);
                String totalField = getMirrorString(vsVals, "totalField", "");
                if (!totalField.isEmpty()) responseMapping.put("totalField", totalField);
                String hasNextField = getMirrorString(vsVals, "hasNextField", "");
                if (!hasNextField.isEmpty()) responseMapping.put("hasNextField", hasNextField);
            }

            // ── requestParams ─────────────────────────────────────────────
            ObjectNode reqParams = ve.putObject("requestParams");
            reqParams.put("pageParam", getMirrorString(vsVals, "pageParam", "page"));
            reqParams.put("limitParam", getMirrorString(vsVals, "limitParam", "limit"));
            reqParams.put("defaultLimit", getInt(vsVals, "defaultLimit", 20));

            // ── Performance hints ─────────────────────────────────────────
            ve.put("cacheStrategy", getMirrorString(vsVals, "cacheStrategy", "NONE"));
            int debounce = getInt(vsVals, "debounceMs", 0);
            if (debounce > 0) ve.put("debounceMs", debounce);
            int minSearch = getInt(vsVals, "minSearchLength", 0);
            if (minSearch > 0) ve.put("minSearchLength", minSearch);
        }

        return ve;
    }

    /** Extracts an AnnotationMirror from an AnnotationValue wrapper (used in arrays). */
    private AnnotationMirror extractMirror(Object annotationValueEntry) {
        if (annotationValueEntry instanceof AnnotationMirror am) return am;
        if (annotationValueEntry instanceof AnnotationValue av
                && av.getValue() instanceof AnnotationMirror am) return am;
        return null;
    }

    private ObjectNode buildSearchSchemaFromMirrorList(List<?> spList) {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode requiredArray = MAPPER.createArrayNode();

        for (Object spObj : spList) {
            AnnotationMirror spMirror = extractMirror(spObj);
            if (spMirror == null) continue;
            Map<String, AnnotationValue> spVals = getAnnotationValues(spMirror);
            String name = getMirrorString(spVals, "name", "");
            ObjectNode prop = properties.putObject(name);

            AnnotationValue typeVal = spVals.get("type");
            String type = (typeVal != null) ? typeVal.getValue().toString().toLowerCase() : "string";
            prop.put("type", type);

            String desc = getMirrorString(spVals, "description", "");
            if (!desc.isEmpty()) prop.put("description", desc);

            AnnotationValue reqVal = spVals.get("required");
            if (reqVal != null && Boolean.TRUE.equals(reqVal.getValue())) {
                requiredArray.add(name);
            }
        }

        if (!requiredArray.isEmpty()) schema.set("required", requiredArray);
        return schema;
    }

    private ObjectNode buildSearchDefaultsFromMirrorList(List<?> spList) {
        ObjectNode defaults = MAPPER.createObjectNode();
        boolean hasDefaults = false;
        for (Object spObj : spList) {
            AnnotationMirror spMirror = extractMirror(spObj);
            if (spMirror == null) continue;
            Map<String, AnnotationValue> spVals = getAnnotationValues(spMirror);
            String name = getMirrorString(spVals, "name", "");
            String defVal = getMirrorString(spVals, "defaultValue", "");
            if (!defVal.isEmpty()) { defaults.put(name, defVal); hasDefaults = true; }
        }
        return hasDefaults ? defaults : null;
    }

    // ─── Internal records ────────────────────────────────────────────────────

    private record FieldTypeInfo(String dataType, boolean multiple,
                                  TypeElement nestedClass, TypeElement enumElement) {}
}
