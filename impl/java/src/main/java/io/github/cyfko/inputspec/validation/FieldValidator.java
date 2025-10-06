package io.github.cyfko.inputspec.validation;

import io.github.cyfko.inputspec.model.ConstraintDescriptor;
import io.github.cyfko.inputspec.model.ConstraintType;
import io.github.cyfko.inputspec.model.InputFieldSpec;
import io.github.cyfko.inputspec.model.ValuesEndpoint;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minimal reference validator implementing protocol v2 order semantics.
 * Not optimized; focuses on correctness & clear mapping to spec.
 */
public class FieldValidator {
    public ValidationResult validate(InputFieldSpec spec, Object input) {
        return validate(spec, input, false);
    }

    /**
     * Validate with optional short-circuit: stops collecting further errors for the field after the first error.
     * Short-circuit still ensures REQUIRED and TYPE checks run in order.
     */
    public ValidationResult validate(InputFieldSpec spec, Object input, boolean shortCircuit) {
        return validate(spec, input, ValidationOptions.builder().shortCircuit(shortCircuit).build());
    }

    /**
     * Preferred extensible variant using {@link ValidationOptions}.
     */
    public ValidationResult validate(InputFieldSpec spec, Object input, ValidationOptions options) {
        boolean shortCircuit = options != null && options.isShortCircuit();
        List<ValidationError> errors = new ArrayList<>();
        if (spec == null) {
            errors.add(ValidationError.builder().constraintName("spec").message("Spec is null").build());
            return new ValidationResult(false, errors);
        }

        // 1. REQUIRED
        if (spec.isRequired() && isEmpty(input)) {
            errors.add(ValidationError.builder().constraintName("required").message("Field is required").build());
            return new ValidationResult(false, errors);
        }
        if (isEmpty(input)) {
            return new ValidationResult(true, errors); // optional & empty
        }

        // 2. TYPE (basic checks)
        if (!typeMatches(spec, input)) {
            errors.add(ValidationError.builder().constraintName("type").message("Type mismatch").build());
            return new ValidationResult(false, errors);
        }

        // 3. CLOSED DOMAIN MEMBERSHIP
        if (spec.getValuesEndpoint() != null && spec.getValuesEndpoint().getMode() != ValuesEndpoint.Mode.SUGGESTIONS) {
            ValuesEndpoint ve = spec.getValuesEndpoint();
            if (ve.getProtocol() == ValuesEndpoint.Protocol.INLINE && ve.getItems() != null) {
                List<Object> domain = ve.getItems().stream().map(v -> v.getValue()).toList();
                if (spec.isExpectMultipleValues()) {
                    List<?> arr = (List<?>) input;
                    for (int i = 0; i < arr.size(); i++) {
                        Object el = arr.get(i);
                        if (!domain.contains(el)) {
                            errors.add(ValidationError.builder().constraintName("membership").message("Value not allowed").index(i).value(el).build());
                            if (shortCircuit) return new ValidationResult(false, errors);
                        }
                    }
                } else {
                    if (!domain.contains(input)) {
                        errors.add(ValidationError.builder().constraintName("membership").message("Value not allowed").value(input).build());
                        if (shortCircuit) return new ValidationResult(false, errors);
                    }
                }
            }
        }

        // 4. ORDERED CONSTRAINTS
        if (spec.getConstraints() != null) {
            for (ConstraintDescriptor cd : spec.getConstraints()) {
                applyConstraint(spec, cd, input, errors, shortCircuit);
                if (shortCircuit && !errors.isEmpty()) {
                    return new ValidationResult(false, errors);
                }
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    private void applyConstraint(InputFieldSpec spec, ConstraintDescriptor cd, Object input, List<ValidationError> errors, boolean shortCircuit) {
    ConstraintType t = cd.getType();
    Object params = cd.getParams();
    if (t == null) return;
        if (spec.isExpectMultipleValues()) {
            if (!(input instanceof List)) return; // already type error earlier if mismatch
            List<?> arr = (List<?>) input;
            switch (t) {
                case MIN_LENGTH: { // collection size
                    Integer min = extractInt(params, "value");
                    if (min != null && arr.size() < min) {
                        errors.add(ValidationError.builder().constraintName(cd.getName()).message(cd.getErrorMessage() != null ? cd.getErrorMessage() : ("min length=" + min)).value(arr.size()).build());
                        if (shortCircuit) return; 
                    }
                    break; }
                case MAX_LENGTH: { // collection size
                    Integer max = extractInt(params, "value");
                    if (max != null && arr.size() > max) {
                        errors.add(ValidationError.builder().constraintName(cd.getName()).message(cd.getErrorMessage() != null ? cd.getErrorMessage() : ("max length=" + max)).value(arr.size()).build());
                        if (shortCircuit) return; 
                    }
                    break; }
                case PATTERN:
                case MIN_VALUE:
                case MAX_VALUE:
                case MIN_DATE:
                case MAX_DATE:
                case RANGE:
                    for (int i = 0; i < arr.size(); i++) {
                        if (!elementValid(t, params, arr.get(i))) {
                            errors.add(ValidationError.builder().constraintName(cd.getName()).message(cd.getErrorMessage() != null ? cd.getErrorMessage() : cd.getName()).index(i).value(arr.get(i)).build());
                            if (shortCircuit) return; 
                        }
                    }
                    break;
                default: // custom or unknown -> ignore per spec tolerance
                    break;
            }
        } else {
            // For single-value fields MIN_LENGTH / MAX_LENGTH do not apply (string length not validated here per spec semantics Option A)
            if (t == ConstraintType.MIN_LENGTH || t == ConstraintType.MAX_LENGTH) {
                return; // ignore silently
            }
            if (!elementValid(t, params, input)) {
                errors.add(ValidationError.builder().constraintName(cd.getName()).message(cd.getErrorMessage() != null ? cd.getErrorMessage() : cd.getName()).value(input).build());
                if (shortCircuit) return; 
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean elementValid(ConstraintType type, Object params, Object value) {
        try {
            switch (type) {
                case PATTERN: {
                    String regex = (String) ((Map<String, Object>) params).get("regex");
                    if (value == null) return true;
                    return value.toString().matches(regex);
                }
                case MIN_LENGTH: // handled at collection level when multi-value
                case MAX_LENGTH: // handled at collection level when multi-value
                    return true;
                case MIN_VALUE: {
                    double min = ((Number) ((Map<String, Object>) params).get("value")).doubleValue();
                    return value == null || ((Number) value).doubleValue() >= min;
                }
                case MAX_VALUE: {
                    double max = ((Number) ((Map<String, Object>) params).get("value")).doubleValue();
                    return value == null || ((Number) value).doubleValue() <= max;
                }
                case MIN_DATE: {
                    String iso = (String) ((Map<String, Object>) params).get("iso");
                    if (value == null) return true;
                    Instant v = Instant.parse(value.toString());
                    return !v.isBefore(Instant.parse(iso));
                }
                case MAX_DATE: {
                    String iso = (String) ((Map<String, Object>) params).get("iso");
                    if (value == null) return true;
                    Instant v = Instant.parse(value.toString());
                    return !v.isAfter(Instant.parse(iso));
                }
                case RANGE: {
                    Map<String, Object> m = (Map<String, Object>) params;
                    Object minObj = m.get("min");
                    Object maxObj = m.get("max");
                    Object stepObj = m.get("step");
                    if (value instanceof Number) {
                        double v = ((Number) value).doubleValue();
                        double min = ((Number) minObj).doubleValue();
                        double max = ((Number) maxObj).doubleValue();
                        if (v < min || v > max) return false;
                        if (stepObj != null) {
                            double step = ((Number) stepObj).doubleValue();
                            if (step > 0) {
                                double diff = v - min;
                                double mod = Math.abs(diff % step);
                                double epsilon = 1e-9;
                                if (mod > epsilon && Math.abs(mod - step) > epsilon) return false;
                            }
                        }
                        return true;
                    }
                    if (value instanceof String) { // date/time ISO
                        Instant v = Instant.parse(value.toString());
                        Instant min = Instant.parse(minObj.toString());
                        Instant max = Instant.parse(maxObj.toString());
                        return !v.isBefore(min) && !v.isAfter(max); // step ignored for dates
                    }
                    return true; // unsupported type; tolerate
                }
                default:
                    return true; // custom/unknown ignored
            }
        } catch (ClassCastException | DateTimeParseException | NullPointerException ex) {
            return false; // treat malformed params as validation failure for safety
        }
    }

    private Integer extractInt(Object params, String key) {
        if (!(params instanceof Map)) return null;
        Object v = ((Map<?, ?>) params).get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return null;
    }

    private boolean typeMatches(InputFieldSpec spec, Object input) {
        if (spec.isExpectMultipleValues()) {
            return input instanceof List; // deeper per-element type checks omitted for brevity
        }
        switch (spec.getDataType()) {
            case STRING: return input instanceof String;
            case NUMBER: return input instanceof Number;
            case DATE: return input instanceof String; // ISO 8601 string accepted
            case BOOLEAN: return input instanceof Boolean;
            default: return false;
        }
    }

    private boolean isEmpty(Object v) {
        if (v == null) return true;
        if (v instanceof String) return ((String) v).isBlank();
        if (v instanceof List) return ((List<?>) v).isEmpty();
        return false;
    }
}
