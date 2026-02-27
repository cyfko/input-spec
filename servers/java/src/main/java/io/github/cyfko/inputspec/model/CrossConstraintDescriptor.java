package io.github.cyfko.inputspec.model;// ─── CrossConstraintDescriptor ───────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.protocol.CrossConstraintType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CrossConstraintDescriptor(
    String name,
    CrossConstraintType type,
    List<String> fields,
    @JsonProperty("params") JsonNode params,
    @JsonProperty("errorMessage") JsonNode errorMessage,
    @JsonProperty("description") JsonNode description
) {
    public CrossConstraintDescriptor {
        fields = fields != null ? List.copyOf(fields) : List.of();
        if (type == null) type = CrossConstraintType.UNKNOWN;
    }
}