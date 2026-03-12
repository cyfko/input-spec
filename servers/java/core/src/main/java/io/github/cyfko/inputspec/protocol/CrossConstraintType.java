package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The cross-field constraint handlers (§2.10 — {@code CrossConstraintDescriptor.type}).
 *
 * <p>Each constant represents a specific inter-field validation strategy that the
 * {@link io.github.cyfko.inputspec.validation.FormSpecValidator} evaluates at
 * form submission. Cross-constraints are declared via {@code @CrossConstraint}
 * on the {@code @FormSpec}-annotated class.</p>
 *
 * <p>{@link #UNKNOWN} is the forward-compatibility sentinel: unknown cross-constraint
 * types are silently ignored, allowing the protocol to evolve without breaking
 * older validators.</p>
 *
 * @see io.github.cyfko.inputspec.model.CrossConstraintDescriptor
 * @see io.github.cyfko.inputspec.CrossConstraint
 */
public enum CrossConstraintType {

    /**
     * Compares two field values with an operator (LT, LTE, GT, GTE, EQ, NEQ).
     * Params: {@code {"operator": "gt"}}. Fields: {@code [fieldA, fieldB]} — rule is
     * "{@code fieldA operator fieldB}".
     */
    FIELD_COMPARISON  ("fieldComparison"),

    /**
     * Requires at least N non-empty fields among the listed fields.
     * Params: {@code {"min": 1}}.
     */
    AT_LEAST_ONE      ("atLeastOne"),

    /**
     * At most N of the listed fields may be filled simultaneously.
     * Params: {@code {"max": 1}}.
     */
    MUTUALLY_EXCLUSIVE("mutuallyExclusive"),

    /**
     * Field B is required when field A has specific values.
     * Params: {@code {"sourceValues": ["value1","value2"]}} or empty for "any non-empty value".
     * Fields: {@code [sourceField, dependentField]}.
     */
    DEPENDS_ON        ("dependsOn"),

    /**
     * Delegates to a registered {@link io.github.cyfko.inputspec.validation.CustomCrossConstraintHandler}.
     * Params: {@code {"handlerKey": "myCustomRule"}}.
     */
    CUSTOM            ("custom"),

    /** Forward-compatibility sentinel — unknown types are silently ignored. */
    UNKNOWN           (null);

    /** The camelCase string used in the JSON spec (e.g. {@code "fieldComparison"}). */
    public final String jsonValue;

    CrossConstraintType(String jsonValue) { this.jsonValue = jsonValue; }

    /**
     * Deserializes a JSON string to the corresponding {@code CrossConstraintType}.
     *
     * @param v the JSON string value (may be {@code null})
     * @return the matching enum constant, or {@link #UNKNOWN}
     */
    @JsonCreator
    public static CrossConstraintType fromJson(String v) {
        if (v == null) return UNKNOWN;
        for (CrossConstraintType t : values()) {
            if (v.equals(t.jsonValue)) return t;
        }
        return UNKNOWN;
    }

    /** Serializes this constant to its JSON representation. */
    @JsonValue
    public String toJson() { return jsonValue; }
}