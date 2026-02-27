package io.github.cyfko.inputspec.model;// ─── InputFieldSpec ───────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.protocol.DataType;

import java.util.List;

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
    public InputFieldSpec {
        constraints = constraints != null ? List.copyOf(constraints) : List.of();
        subFields   = subFields   != null ? List.copyOf(subFields)   : List.of();
        if (dataType == null) dataType = DataType.STRING;
    }
}