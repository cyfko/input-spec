package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.protocol.ConstraintType;

/**
 * Runtime representation of a single validation constraint on an input field.
 *
 * <p>Corresponds to the {@code ConstraintDescriptor} entity of the DIFSP protocol (§2.7).
 * Each descriptor captures the constraint type, its parameters, and optional i18n-aware
 * error messages. The annotation processor maps Jakarta Validation annotations
 * ({@code @NotNull}, {@code @Size}, {@code @Min}, {@code @Max}, {@code @Pattern},
 * {@code @Email}, {@code @Past}, {@code @Future}, …) to these descriptors at compile time.</p>
 *
 * <p><b>Mapping examples:</b></p>
 * <pre>
 * Jakarta                 → ConstraintDescriptor
 * ─────────────────────────────────────────────
 * @Size(min=2, max=50)   → { name:"size", type:"range", params:{min:2, max:50} }
 * @Min(0)                → { name:"min",  type:"minValue", params:{value:0} }
 * @Pattern("\\d+")       → { name:"pattern", type:"pattern", params:{value:"\\d+"} }
 * @NotNull               → (sets required=true on the InputFieldSpec instead)
 * </pre>
 *
 * @param name         the constraint identifier — typically lowercase, derived from the Jakarta
 *                     annotation name (e.g. {@code "size"}, {@code "min"}, {@code "pattern"})
 * @param type         the protocol constraint type (§2.7): {@link ConstraintType#PATTERN},
 *                     {@link ConstraintType#MIN_LENGTH}, {@link ConstraintType#RANGE}, etc.
 * @param params       a JSON object containing the constraint's parameters.
 *                     Structure depends on the {@code type} — for example, {@code {"value": 5}}
 *                     for {@code minLength}, {@code {"min": 2, "max": 50}} for {@code range}
 * @param errorMessage human-readable error message — may be a plain string or an i18n key reference.
 *                     {@link JsonNode} supports both static text and structured i18n objects
 * @param description  human-readable description of what the constraint enforces — same i18n
 *                     structure as {@code errorMessage}
 *
 * @see InputFieldSpec
 * @see ConstraintType
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConstraintDescriptor(
    String name,
    ConstraintType type,
    @JsonProperty("params") JsonNode params,
    @JsonProperty("errorMessage") JsonNode errorMessage,
    @JsonProperty("description") JsonNode description
) {}