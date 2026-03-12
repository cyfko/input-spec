package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A single value alias within an INLINE {@link ValuesEndpoint}.
 *
 * <p>Corresponds to the {@code ValueAlias} entity of the DIFSP protocol (§2.3).
 * Each alias pairs a canonical {@code value} (submitted to the server) with a
 * human-readable {@code label} (displayed to the user).</p>
 *
 * <p>Both {@code value} and {@code label} are represented as {@link JsonNode} to support
 * both simple strings and structured i18n objects. Labels can be localized via the
 * form's {@code ResourceBundle} using the key:</p>
 * <pre>
 *   {formId}.fields.{fieldName}.items.{value}.label
 * </pre>
 *
 * <p><b>Example JSON:</b></p>
 * <pre>
 * { "value": "ACTIVE", "label": "Active" }
 * </pre>
 *
 * @param value the canonical value submitted to the server (e.g. {@code "ACTIVE"}, {@code "FR"})
 * @param label the display text shown to the user — may be a plain string or an i18n reference
 *
 * @see ValuesEndpoint
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ValueAlias(
    @JsonProperty("value") JsonNode value,
    @JsonProperty("label") JsonNode label
) {}
