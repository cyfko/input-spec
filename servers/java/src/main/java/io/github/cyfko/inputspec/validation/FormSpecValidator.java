package io.github.cyfko.inputspec.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.cache.BundleResolver;
import io.github.cyfko.inputspec.model.*;
import io.github.cyfko.inputspec.protocol.ComparisonOperator;
import io.github.cyfko.inputspec.protocol.ConstraintType;
import io.github.cyfko.inputspec.protocol.CrossConstraintType;
import io.github.cyfko.inputspec.protocol.DataType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * Server-side implementation of the DIFSP validation pipeline (§2.7).
 *
 * <p>All switch expressions are on protocol enums — the compiler enforces
 * exhaustiveness. Unknown/future constraint types fall through to the
 * UNKNOWN arm and are silently tolerated (protocol §2.7 forward-compat rule).</p>
 *
 * <h3>Custom constraint handlers</h3>
 * Register handlers for {@code custom} constraints (§2.6) and cross-constraints (§2.10):
 * <pre>
 * validator.registerCustomHandler("promoCode", (value, params) -> {
 *     // ... business logic ...
 *     return isValid ? Optional.empty() : Optional.of("Invalid promo code");
 * });
 *
 * validator.registerCustomCrossHandler("complexRule", (fieldValues, params) -> {
 *     // ... cross-field business logic ...
 *     return isValid ? Optional.empty() : Optional.of("Fields are inconsistent");
 * });
 * </pre>
 */
public class FormSpecValidator {

    private final Map<String, CustomConstraintHandler>      customHandlers      = new HashMap<>();
    private final Map<String, CustomCrossConstraintHandler> customCrossHandlers = new HashMap<>();
    private final Map<String, GlobalFormValidatorHandler>   globalHandlers      = new HashMap<>();

    // ─── Custom handler registration ─────────────────────────────────────────

    /**
     * Registers a global form validator handler for an entire form (Phase 3).
     *
     * @param formId  the form ID
     * @param handler the handler to invoke
     */
    public void registerGlobalFormHandler(String formId, GlobalFormValidatorHandler handler) {
        globalHandlers.put(Objects.requireNonNull(formId), Objects.requireNonNull(handler));
    }

    /**
     * Registers a handler for a {@code custom} constraint key (§2.6).
     *
     * @param key     the {@code params.key} value from the constraint descriptor
     * @param handler the handler to invoke during validation
     */
    public void registerCustomHandler(String key, CustomConstraintHandler handler) {
        customHandlers.put(Objects.requireNonNull(key), Objects.requireNonNull(handler));
    }

    /**
     * Registers a handler for a {@code custom} cross-field constraint key (§2.10).
     *
     * @param key     the {@code params.key} value from the cross-constraint descriptor
     * @param handler the handler to invoke during validation
     */
    public void registerCustomCrossHandler(String key, CustomCrossConstraintHandler handler) {
        customCrossHandlers.put(Objects.requireNonNull(key), Objects.requireNonNull(handler));
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public ValidationResult validateForm(FormSpecModel spec, Map<String, Object> values) {
        return validateForm(spec, values, null);
    }

    public ValidationResult validateForm(FormSpecModel spec, Map<String, Object> values, Locale locale) {
        List<ValidationError> errors = new ArrayList<>();

        // ─── PHASE 1: Standard Stateless Validation ───
        for (InputFieldSpec field : spec.fields()) {
            errors.addAll(validateFieldInternal(field, values.get(field.name()), field.name(), spec.id(), locale, false));
        }

        if (spec.crossConstraints() != null) {
            for (CrossConstraintDescriptor cc : spec.crossConstraints()) {
                if (cc.type() != CrossConstraintType.CUSTOM) {
                    evaluateCrossConstraint(cc, values, spec.id(), locale).ifPresent(errors::add);
                }
            }
        }

        if (!errors.isEmpty()) {
            return new ValidationResult(false, Collections.unmodifiableList(errors));
        }

        // ─── PHASE 2: Custom Constraints Validation ───
        for (InputFieldSpec field : spec.fields()) {
            errors.addAll(validateFieldInternal(field, values.get(field.name()), field.name(), spec.id(), locale, true));
        }

        if (spec.crossConstraints() != null) {
            for (CrossConstraintDescriptor cc : spec.crossConstraints()) {
                if (cc.type() == CrossConstraintType.CUSTOM) {
                    evaluateCrossConstraint(cc, values, spec.id(), locale).ifPresent(errors::add);
                }
            }
        }

        if (!errors.isEmpty()) {
            return new ValidationResult(false, Collections.unmodifiableList(errors));
        }

        // ─── PHASE 3: Global Form Validation ───
        GlobalFormValidatorHandler globalHandler = globalHandlers.get(spec.id());
        if (globalHandler != null) {
            Map<String, String> globalMap = globalHandler.validate(values);
            if (globalMap != null && !globalMap.isEmpty()) {
                globalMap.forEach((path, msg) ->
                    errors.add(ValidationError.field(path, "global", msg, values.get(path))));
            }
        }

        return new ValidationResult(errors.isEmpty(), Collections.unmodifiableList(errors));
    }

    public List<ValidationError> validateField(InputFieldSpec spec, Object value, String path) {
        return validateField(spec, value, path, null, null);
    }

    public List<ValidationError> validateField(InputFieldSpec spec, Object value,
                                                String path, String formId, Locale locale) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Phase 1: Standard constraints
        errors.addAll(validateFieldInternal(spec, value, path, formId, locale, false));
        if (!errors.isEmpty()) return errors;
        
        // Phase 2: Custom constraints
        errors.addAll(validateFieldInternal(spec, value, path, formId, locale, true));
        return errors;
    }

    private List<ValidationError> validateFieldInternal(InputFieldSpec spec, Object value,
                                                String path, String formId, Locale locale, boolean customOnly) {
        List<ValidationError> errors = new ArrayList<>();

        if (!customOnly) {
            // 1. REQUIRED
            if (spec.required() && isEmpty(value)) {
                errors.add(ValidationError.field(path, "required",
                    "This field is required", value));
                return errors;
            }
            if (isEmpty(value)) return errors;

            // 2. TYPE
            if (!matchesType(value, spec.dataType(), spec.expectMultipleValues())) {
                errors.add(ValidationError.field(path, "type",
                    "Expected type " + spec.dataType().name().toLowerCase()
                    + (spec.expectMultipleValues() ? "[]" : ""), value));
                return errors;
            }

            // 3. INLINE CLOSED membership (synchronous)
            if (spec.valuesEndpoint() != null
                    && spec.valuesEndpoint().isInline()
                    && spec.valuesEndpoint().isClosed()) {
                errors.addAll(checkInlineMembership(spec, value, path));
                if (!errors.isEmpty()) return errors;
            }
        } else {
            if (isEmpty(value)) return errors;
        }

        // 4. OBJECT RECURSION
        if (spec.dataType() == DataType.OBJECT) {
            if (spec.expectMultipleValues()) {
                List<?> items = asList(value);
                for (int i = 0; i < items.size(); i++) {
                    int idx = i;
                    for (InputFieldSpec sub : spec.subFields()) {
                        Object subVal = getNestedValue(items.get(i), sub.name());
                        errors.addAll(validateFieldInternal(sub, subVal,
                            path + "[" + idx + "]." + sub.name(), formId, locale, customOnly));
                    }
                }
            } else {
                for (InputFieldSpec sub : spec.subFields()) {
                    errors.addAll(validateFieldInternal(sub, getNestedValue(value, sub.name()),
                        path + "." + sub.name(), formId, locale, customOnly));
                }
            }
            errors.addAll(runConstraints(spec, value, path, formId, locale, customOnly));
            return errors;
        }

        // 5. ORDERED CONSTRAINTS
        errors.addAll(runConstraints(spec, value, path, formId, locale, customOnly));
        return errors;
    }

    // ─── Membership ──────────────────────────────────────────────────────────

    private List<ValidationError> checkInlineMembership(InputFieldSpec spec,
                                                          Object value, String path) {
        Set<String> domain = new HashSet<>();
        for (ValueAlias alias : spec.valuesEndpoint().items()) {
            if (alias.value() != null && !alias.value().isNull())
                domain.add(alias.value().asText());
        }

        List<ValidationError> errors = new ArrayList<>();
        if (spec.expectMultipleValues()) {
            List<?> items = asList(value);
            for (int i = 0; i < items.size(); i++) {
                String v = Objects.toString(items.get(i), "");
                if (!domain.contains(v))
                    errors.add(new ValidationError(path, "membership",
                        "Value '" + v + "' is not in the allowed set",
                        v, null, null, i));
            }
        } else {
            String v = Objects.toString(value, "");
            if (!domain.contains(v))
                errors.add(ValidationError.field(path, "membership",
                    "Value '" + v + "' is not in the allowed set", v));
        }
        return errors;
    }

    // ─── Constraints pipeline ─────────────────────────────────────────────────

    private List<ValidationError> runConstraints(InputFieldSpec spec, Object value,
                                                 String path, String formId, Locale locale, boolean customOnly) {
        List<ValidationError> errors = new ArrayList<>();
        if (spec.expectMultipleValues()) {
            List<?> items = asList(value);
            for (ConstraintDescriptor c : spec.constraints()) {
                if (customOnly && c.type() != ConstraintType.CUSTOM) continue;
                if (!customOnly && c.type() == ConstraintType.CUSTOM) continue;

                applyArrayLevel(c, items, path, formId, locale).ifPresent(errors::add);
                for (int i = 0; i < items.size(); i++) {
                    final int idx = i;
                    applyConstraint(c, items.get(i), spec.dataType(),
                                    path + "[" + idx + "]", formId, locale)
                        .map(e -> new ValidationError(e.path(), e.constraintName(),
                                      e.message(), e.value(), null, null, idx))
                        .ifPresent(errors::add);
                }
            }
        } else {
            for (ConstraintDescriptor c : spec.constraints()) {
                if (customOnly && c.type() != ConstraintType.CUSTOM) continue;
                if (!customOnly && c.type() == ConstraintType.CUSTOM) continue;

                applyConstraint(c, value, spec.dataType(), path, formId, locale)
                    .ifPresent(errors::add);
            }
        }
        return errors;
    }

    private Optional<ValidationError> applyArrayLevel(ConstraintDescriptor c,
                                                        List<?> array, String path,
                                                        String formId, Locale locale) {
        JsonNode p = c.params();
        // Only a subset of constraint types are meaningful at the array level
        return switch (c.type()) {
            case MIN_VALUE -> {
                long min = p.path("value").asLong();
                yield array.size() < min
                    ? Optional.of(err(path, c, "Minimum " + min + " items required",
                                      array, formId, locale))
                    : Optional.empty();
            }
            case MAX_VALUE -> {
                long max = p.path("value").asLong();
                yield array.size() > max
                    ? Optional.of(err(path, c, "Maximum " + max + " items allowed",
                                      array, formId, locale))
                    : Optional.empty();
            }
            case RANGE -> {
                long min = p.path("min").asLong(), max = p.path("max").asLong();
                yield (array.size() < min || array.size() > max)
                    ? Optional.of(err(path, c,
                        "Array length must be between " + min + " and " + max,
                        array, formId, locale))
                    : Optional.empty();
            }
            // Other constraint types apply per-element, not to the array itself
            default -> Optional.empty();
        };
    }

    private Optional<ValidationError> applyConstraint(ConstraintDescriptor c,
                                                      Object value, DataType dataType,
                                                      String path, String formId, Locale locale) {
        JsonNode p   = c.params();
        String   str = value != null ? value.toString() : "";

        return switch (c.type()) {

            case PATTERN -> {
                String flags   = p.path("flags").asText("");
                int    flagBits = 0;
                if (flags.contains("i")) flagBits |= Pattern.CASE_INSENSITIVE;
                if (flags.contains("m")) flagBits |= Pattern.MULTILINE;
                yield Pattern.compile(p.path("regex").asText(), flagBits).matcher(str).matches()
                    ? Optional.empty()
                    : Optional.of(err(path, c, "Invalid format", value, formId, locale));
            }

            case MIN_LENGTH -> {
                int min = p.path("value").asInt();
                yield str.length() >= min ? Optional.empty()
                    : Optional.of(err(path, c, "Minimum " + min + " characters",
                                      value, formId, locale));
            }

            case MAX_LENGTH -> {
                int max = p.path("value").asInt();
                yield str.length() <= max ? Optional.empty()
                    : Optional.of(err(path, c, "Maximum " + max + " characters",
                                      value, formId, locale));
            }

            case MIN_VALUE -> {
                if (dataType != DataType.NUMBER) yield Optional.empty();
                BigDecimal val = new BigDecimal(str);
                BigDecimal min = new BigDecimal(p.path("value").asText());
                yield val.compareTo(min) >= 0 ? Optional.empty()
                    : Optional.of(err(path, c, "Minimum value is " + min,
                                      value, formId, locale));
            }

            case MAX_VALUE -> {
                if (dataType != DataType.NUMBER) yield Optional.empty();
                BigDecimal val = new BigDecimal(str);
                BigDecimal max = new BigDecimal(p.path("value").asText());
                yield val.compareTo(max) <= 0 ? Optional.empty()
                    : Optional.of(err(path, c, "Maximum value is " + max,
                                      value, formId, locale));
            }

            case MIN_DATE -> {
                OffsetDateTime val = parseDate(str);
                OffsetDateTime min = resolveDate(p.path("iso").asText());
                yield val.isBefore(min)
                    ? Optional.of(err(path, c,
                        "Date must be after " + p.path("iso").asText(),
                        value, formId, locale))
                    : Optional.empty();
            }

            case MAX_DATE -> {
                OffsetDateTime val = parseDate(str);
                OffsetDateTime max = resolveDate(p.path("iso").asText());
                yield val.isAfter(max)
                    ? Optional.of(err(path, c,
                        "Date must be before " + p.path("iso").asText(),
                        value, formId, locale))
                    : Optional.empty();
            }

            case RANGE -> {
                if (dataType == DataType.DATE) {
                    OffsetDateTime v   = parseDate(str);
                    OffsetDateTime min = resolveDate(p.path("min").asText());
                    OffsetDateTime max = resolveDate(p.path("max").asText());
                    yield (v.isBefore(min) || v.isAfter(max))
                        ? Optional.of(err(path, c, "Date must be between "
                              + p.path("min").asText() + " and " + p.path("max").asText(),
                              value, formId, locale))
                        : Optional.empty();
                } else {
                    BigDecimal v   = new BigDecimal(str);
                    BigDecimal min = new BigDecimal(p.path("min").asText());
                    BigDecimal max = new BigDecimal(p.path("max").asText());
                    if (v.compareTo(min) < 0 || v.compareTo(max) > 0)
                        yield Optional.of(err(path, c,
                            "Value must be between " + min + " and " + max,
                            value, formId, locale));
                    JsonNode stepNode = p.path("step");
                    if (!stepNode.isMissingNode()) {
                        BigDecimal step = new BigDecimal(stepNode.asText());
                        if (v.subtract(min).remainder(step).compareTo(BigDecimal.ZERO) != 0)
                            yield Optional.of(err(path, c,
                                "Value must be a multiple of " + step + " from " + min,
                                value, formId, locale));
                    }
                    yield Optional.empty();
                }
            }

            case CUSTOM -> {
                // §2.6: delegate to implementation-registered handler; MUST NOT crash if unregistered
                String key = p.path("key").asText("");
                CustomConstraintHandler handler = customHandlers.get(key);
                if (handler != null) {
                    Optional<String> result = handler.validate(value, p);
                    yield result.map(msg -> err(path, c, msg, value, formId, locale));
                }
                // Unknown custom key → tolerated silently (no error)
                yield Optional.empty();
            }

            case UNKNOWN ->
                // Future extension → tolerated per spec
                Optional.empty();
        };
    }

    // ─── Cross-constraint evaluation ──────────────────────────────────────────

    private Optional<ValidationError> evaluateCrossConstraint(
            CrossConstraintDescriptor cc, Map<String, Object> values,
            String formId, Locale locale) {

        JsonNode     p      = cc.params();
        List<String> fields = cc.fields();

        return switch (cc.type()) {

            case FIELD_COMPARISON -> {
                ComparisonOperator op = ComparisonOperator.fromJson(p.path("operator").asText("gt"));
                yield compareValues(values.get(fields.get(0)), values.get(fields.get(1)), op)
                    ? Optional.empty()
                    : Optional.of(ValidationError.cross(cc,
                        resolveMessage(cc.errorMessage(),
                            fields.get(0) + " must be " + op.jsonValue + " " + fields.get(1),
                            formId, locale)));
            }

            case AT_LEAST_ONE -> {
                int min = p.path("min").asInt(1);
                long filled = fields.stream().filter(f -> !isEmpty(values.get(f))).count();
                yield filled >= min ? Optional.empty()
                    : Optional.of(ValidationError.cross(cc,
                        resolveMessage(cc.errorMessage(),
                            "At least " + min + " of [" + String.join(", ", fields)
                            + "] must be filled", formId, locale)));
            }

            case MUTUALLY_EXCLUSIVE -> {
                int max = p.path("max").asInt(1);
                long filled = fields.stream().filter(f -> !isEmpty(values.get(f))).count();
                yield filled <= max ? Optional.empty()
                    : Optional.of(ValidationError.cross(cc,
                        resolveMessage(cc.errorMessage(),
                            "At most " + max + " of [" + String.join(", ", fields)
                            + "] may be filled", formId, locale)));
            }

            case DEPENDS_ON -> {
                Object source = values.get(fields.get(1));
                if (isEmpty(source)) yield Optional.empty();
                JsonNode sv = p.path("sourceValues");
                if (!sv.isMissingNode() && sv.isArray()) {
                    boolean matches = StreamSupport.stream(sv.spliterator(), false)
                        .anyMatch(n -> n.asText().equals(Objects.toString(source, "")));
                    if (!matches) yield Optional.empty();
                }
                yield !isEmpty(values.get(fields.get(0))) ? Optional.empty()
                    : Optional.of(ValidationError.cross(cc,
                        resolveMessage(cc.errorMessage(),
                            fields.get(0) + " is required when " + fields.get(1) + " is set",
                            formId, locale)));
            }

            case CUSTOM -> {
                // §2.10: delegate to implementation-registered handler; MUST NOT crash
                String key = p.path("key").asText("");
                CustomCrossConstraintHandler handler = customCrossHandlers.get(key);
                if (handler != null) {
                    // Build a map of only the involved fields
                    Map<String, Object> involved = new LinkedHashMap<>();
                    for (String f : fields) involved.put(f, values.get(f));
                    Optional<String> result = handler.validate(involved, p);
                    yield result.map(msg -> ValidationError.cross(cc,
                        resolveMessage(cc.errorMessage(), msg, formId, locale)));
                }
                yield Optional.empty();
            }

            case UNKNOWN -> Optional.empty();
        };
    }

    // ─── i18n message resolution ──────────────────────────────────────────────

    private String resolveMessage(JsonNode node, String fallback, String formId, Locale locale) {
        if (node == null || node.isNull() || node.isMissingNode()) return fallback;
        if (node.isTextual()) return node.asText();
        String defaultText = node.path("default").asText(fallback);
        String i18nKey     = node.path("i18nKey").asText("");
        if (!i18nKey.isEmpty() && formId != null && locale != null) {
            return BundleResolver.getInstance()
                .resolveKey(formId, i18nKey, locale)
                .orElse(defaultText);
        }
        return defaultText;
    }

    private ValidationError err(String path, ConstraintDescriptor c,
                                 String fallback, Object value,
                                 String formId, Locale locale) {
        return new ValidationError(path, c.name(),
            resolveMessage(c.errorMessage(), fallback, formId, locale),
            value, null, null, null);
    }

    // ─── Type checking ────────────────────────────────────────────────────────

    private boolean matchesType(Object value, DataType dataType, boolean multiple) {
        if (multiple) {
            if (!(value instanceof List<?>)) return false;
            return ((List<?>) value).stream().allMatch(v -> matchesScalar(v, dataType));
        }
        return matchesScalar(value, dataType);
    }

    private boolean matchesScalar(Object v, DataType dataType) {
        return switch (dataType) {
            case STRING  -> v instanceof String;
            case NUMBER  -> v instanceof Number && !Double.isNaN(((Number) v).doubleValue());
            case BOOLEAN -> v instanceof Boolean;
            case DATE    -> {
                try { parseDate(v.toString()); yield true; }
                catch (Exception e) { yield false; }
            }
            case OBJECT  -> v instanceof Map;
        };
    }

    // ─── Comparison ──────────────────────────────────────────────────────────

    private boolean compareValues(Object a, Object b, ComparisonOperator op) {
        if (a == null || b == null) return false;
        int cmp;
        try {
            cmp = new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()));
        } catch (NumberFormatException e) {
            try {
                cmp = parseDate(a.toString()).compareTo(parseDate(b.toString()));
            } catch (Exception ex) { return false; }
        }
        return switch (op) {
            case LT  -> cmp <  0;
            case LTE -> cmp <= 0;
            case GT  -> cmp >  0;
            case GTE -> cmp >= 0;
            case EQ  -> cmp == 0;
            case NEQ -> cmp != 0;
        };
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean isEmpty(Object v) {
        return switch (v) {
            case null -> true;
            case String s -> s.isBlank();
            case Collection<?> c -> c.isEmpty();
            default -> false;
        };
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object v) {
        return v instanceof List<?> l ? (List<Object>) l : List.of(v);
    }

    private Object getNestedValue(Object obj, String key) {
        return obj instanceof Map<?, ?> m ? m.get(key) : null;
    }

    private OffsetDateTime resolveDate(String iso) {
        return "$NOW".equals(iso) ? OffsetDateTime.now() : parseDate(iso);
    }

    private OffsetDateTime parseDate(String dateStr) {
        try {
            return OffsetDateTime.parse(dateStr);
        } catch (Exception e1) {
            try {
                return java.time.LocalDateTime.parse(dateStr).atOffset(java.time.ZoneOffset.UTC);
            } catch (Exception e2) {
                return java.time.LocalDate.parse(dateStr).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            }
        }
    }

    // ─── Result types (protocol §2.8 / §2.10) ────────────────────────────────

    public record ValidationResult(boolean isValid, List<ValidationError> errors) {}

    /**
     * Validation error aligned with protocol §2.8 (field errors) and §2.10 (cross errors).
     *
     * <p><b>Field error shape (§2.8):</b></p>
     * <pre>{ "constraintName": "...", "message": "...", "value": ..., "index": 0 }</pre>
     *
     * <p><b>Cross-constraint error shape (§2.10):</b></p>
     * <pre>{ "crossConstraintName": "...", "message": "...", "fields": [...] }</pre>
     *
     * <p>{@code path} is a server-side extension (not in the protocol) useful for debugging.
     * It is omitted from JSON serialization when null.</p>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ValidationError(
        String       path,                // server extension (not in protocol)
        String       constraintName,      // §2.8
        String       message,             // §2.8 / §2.10
        Object       value,               // §2.8 — the rejected input value
        String       crossConstraintName, // §2.10
        List<String> fields,              // §2.10 — involved field names
        Integer      index                // §2.8 — multi-value element index
    ) {
        /** Creates a field-level validation error (§2.8). */
        public static ValidationError field(String path, String constraint,
                                            String msg, Object value) {
            return new ValidationError(path, constraint, msg, value, null, null, null);
        }

        /** Creates a cross-constraint validation error (§2.10). */
        public static ValidationError cross(CrossConstraintDescriptor cc, String msg) {
            return new ValidationError(null, null, msg, null, cc.name(), cc.fields(), null);
        }
    }
}