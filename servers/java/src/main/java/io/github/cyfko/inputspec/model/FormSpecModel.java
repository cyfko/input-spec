package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Root runtime representation of a complete InputSpec form.
 *
 * <p>Corresponds to the top-level {@code FormSpec} entity of the DIFSP protocol (§2.0).
 * A {@code FormSpecModel} is the entry point for rendering a dynamic form on the client:
 * it describes every input field, its validation constraints, cross-field rules,
 * and the submission endpoint.</p>
 *
 * <p>Instances are deserialized from the JSON spec files generated at compile time
 * by the annotation processor. They are loaded and cached by the
 * {@link io.github.cyfko.inputspec.cache.FormSpecCache} and served to clients
 * via the Spring Boot starter's REST controller.</p>
 *
 * <p><b>JSON structure (top-level):</b></p>
 * <pre>
 * {
 *   "id": "booking-form",
 *   "version": "2.1",
 *   "displayName": "Booking Form",
 *   "fields": [ ... ],
 *   "crossConstraints": [ ... ],
 *   "submitEndpoint": { "protocol": "HTTPS", "uri": "/api/bookings", "method": "POST" }
 * }
 * </pre>
 *
 * @param id               the stable unique identifier for the form (matches the JSON filename
 *                         and the {@code @FormSpec(id = "...")} value)
 * @param version          the protocol version this spec conforms to (e.g. {@code "2.1"})
 * @param displayName      human-readable form title — may be a plain string or an i18n key reference.
 *                         {@link JsonNode} supports both {@code "Booking Form"} and
 *                         {@code {"key":"booking-form.displayName","default":"Booking Form"}}
 * @param description      human-readable form description — same i18n structure as {@code displayName}
 * @param fields           the ordered list of input fields (§2.1) composing this form
 * @param crossConstraints the list of cross-field validation constraints (§2.10) applicable to this form
 * @param submitEndpoint   the endpoint configuration for form submission (§2.4)
 *
 * @see InputFieldSpec
 * @see CrossConstraintDescriptor
 * @see SubmitEndpoint
 * @see io.github.cyfko.inputspec.cache.FormSpecCache
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FormSpecModel(
    String id,
    String version,
    @JsonProperty("displayName") JsonNode displayName,
    @JsonProperty("description") JsonNode description,
    List<InputFieldSpec> fields,
    @JsonProperty("crossConstraints") List<CrossConstraintDescriptor> crossConstraints,
    SubmitEndpoint submitEndpoint
) {
    /**
     * Compact constructor — applies defensive copying.
     *
     * <ul>
     *   <li>{@code fields} is copied via {@link List#copyOf} (immutable)</li>
     *   <li>{@code crossConstraints} is copied via {@link List#copyOf} (immutable)</li>
     * </ul>
     */
    public FormSpecModel {
        fields           = fields           != null ? List.copyOf(fields)           : List.of();
        crossConstraints = crossConstraints != null ? List.copyOf(crossConstraints) : List.of();
    }
}