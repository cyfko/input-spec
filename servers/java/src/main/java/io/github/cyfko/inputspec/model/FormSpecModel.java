package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

// ─── FormSpecModel ────────────────────────────────────────────────────────────

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
    public FormSpecModel {
        fields           = fields           != null ? List.copyOf(fields)           : List.of();
        crossConstraints = crossConstraints != null ? List.copyOf(crossConstraints) : List.of();
    }
}