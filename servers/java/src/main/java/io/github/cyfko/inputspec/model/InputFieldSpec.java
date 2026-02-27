package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.protocol.DataType;

import java.util.List;

/**
 * Runtime representation of a single input field within a {@link FormSpecModel}.
 *
 * <p>Corresponds to the {@code InputFieldSpec} entity of the DIFSP protocol (§2.1).
 * Each field describes the data type, display metadata, validation constraints,
 * and optional value source for one input element of a form.</p>
 *
 * <p>Instances are deserialized from the JSON spec files generated at compile time
 * by the annotation processor. They are immutable once constructed — all list fields
 * are defensively copied via {@link List#copyOf(java.util.Collection)}.</p>
 *
 * <p><b>Example JSON (excerpt):</b></p>
 * <pre>
 * {
 *   "name": "hotelName",
 *   "displayName": "Hotel",
 *   "dataType": "STRING",
 *   "required": true,
 *   "constraints": [
 *     { "name": "notBlank", "type": "minLength", "params": { "value": 1 } }
 *   ]
 * }
 * </pre>
 *
 * @param name                 the stable field identifier (matches the Java field name)
 * @param displayName          human-readable label — may be a plain string or an i18n key reference
 *                             ({@link JsonNode} supports both {@code "Hotel"} and {@code {"key":"...","default":"Hotel"}})
 * @param description          human-readable help text or tooltip — same i18n structure as {@code displayName}
 * @param dataType             the primitive type of the field value (§2.1): {@code STRING}, {@code NUMBER},
 *                             {@code BOOLEAN}, {@code DATE}, or {@code OBJECT}
 * @param expectMultipleValues when {@code true}, the field accepts a JSON array of values
 *                             (e.g. {@code List<String>} in the form class)
 * @param required             whether the field must have a non-null, non-empty value
 * @param formatHint           non-enforced UI hint for rendering (e.g. {@code "phone"}, {@code "email"},
 *                             {@code "iso8601"}) — never validated server-side
 * @param valuesEndpoint       optional value source configuration (§2.2): {@code INLINE} with static items,
 *                             or a remote endpoint ({@code HTTPS}/{@code GRPC}) with pagination and search
 * @param subFields            nested fields when {@code dataType = OBJECT} — enables composite structures
 * @param constraints          the list of validation constraints (§2.7) applied to this field
 *
 * @see FormSpecModel
 * @see ConstraintDescriptor
 * @see ValuesEndpoint
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InputFieldSpec(
    String name,
    @JsonProperty("displayName") JsonNode displayName,
    @JsonProperty("description") JsonNode description,
    DataType dataType,
    boolean expectMultipleValues,
    boolean required,
    String formatHint,
    ValuesEndpoint valuesEndpoint,
    List<InputFieldSpec> subFields,
    List<ConstraintDescriptor> constraints
) {
    /**
     * Compact constructor — applies defensive copying and defaults.
     *
     * <ul>
     *   <li>{@code constraints} and {@code subFields} are copied via {@link List#copyOf} (immutable)</li>
     *   <li>{@code dataType} defaults to {@link DataType#STRING} when {@code null}</li>
     * </ul>
     */
    public InputFieldSpec {
        constraints = constraints != null ? List.copyOf(constraints) : List.of();
        subFields   = subFields   != null ? List.copyOf(subFields)   : List.of();
        if (dataType == null) dataType = DataType.STRING;
    }
}