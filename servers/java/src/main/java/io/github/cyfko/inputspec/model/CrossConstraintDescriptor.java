package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.protocol.CrossConstraintType;

import java.util.List;

/**
 * Runtime representation of a cross-field validation constraint.
 *
 * <p>Corresponds to the {@code CrossConstraintDescriptor} entity of the DIFSP protocol (§2.10).
 * Cross-constraints validate relationships between two or more fields — for example,
 * ensuring an end date is after a start date, or that at least one of several
 * optional fields is filled.</p>
 *
 * <p>The annotation processor maps {@code @CrossConstraint} declarations to these
 * descriptors at compile time. Developers declare cross-constraints at the class level:</p>
 *
 * <pre>
 * {@literal @}CrossConstraint(
 *     name     = "dateRange",
 *     type     = CrossConstraintType.FIELD_COMPARISON,
 *     fields   = {"endDate", "startDate"},
 *     operator = ComparisonOperator.GT
 * )
 * </pre>
 *
 * <p><b>Supported types:</b></p>
 * <ul>
 *   <li>{@link CrossConstraintType#FIELD_COMPARISON} — compares two field values with an operator</li>
 *   <li>{@link CrossConstraintType#AT_LEAST_ONE} — requires at least N non-empty fields</li>
 *   <li>{@link CrossConstraintType#MUTUALLY_EXCLUSIVE} — at most N fields may be filled simultaneously</li>
 *   <li>{@link CrossConstraintType#DEPENDS_ON} — field B is required when field A has specific values</li>
 *   <li>{@link CrossConstraintType#CUSTOM} — delegates to a registered {@code CustomCrossConstraintHandler}</li>
 * </ul>
 *
 * @param name         the stable unique name within the form (e.g. {@code "dateRange"})
 * @param type         the cross-constraint type from the protocol (§2.10)
 * @param fields       ordered list of involved field names — interpretation depends on {@code type}
 * @param params       a JSON object containing type-specific parameters:
 *                     {@code {"operator":"GT"}} for FIELD_COMPARISON,
 *                     {@code {"min":1}} for AT_LEAST_ONE, etc.
 * @param errorMessage human-readable error message — may be a plain string or an i18n key reference
 * @param description  human-readable description of the cross-constraint's purpose
 *
 * @see CrossConstraintType
 * @see InputFieldSpec
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrossConstraintDescriptor(
    String name,
    CrossConstraintType type,
    List<String> fields,
    @JsonProperty("params") JsonNode params,
    @JsonProperty("errorMessage") JsonNode errorMessage,
    @JsonProperty("description") JsonNode description
) {
    /**
     * Compact constructor — applies defensive copying and defaults.
     *
     * <ul>
     *   <li>{@code fields} is copied via {@link List#copyOf} (immutable)</li>
     *   <li>{@code type} defaults to {@link CrossConstraintType#UNKNOWN} when {@code null}</li>
     * </ul>
     */
    public CrossConstraintDescriptor {
        fields = fields != null ? List.copyOf(fields) : List.of();
        if (type == null) type = CrossConstraintType.UNKNOWN;
    }
}