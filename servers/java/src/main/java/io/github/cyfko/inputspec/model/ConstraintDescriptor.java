package io.github.cyfko.inputspec.model;// ─── ConstraintDescriptor ─────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cyfko.inputspec.protocol.ConstraintType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConstraintDescriptor(
    String name,
    ConstraintType type,
    @JsonProperty("params") JsonNode params,
    @JsonProperty("errorMessage") JsonNode errorMessage,
    @JsonProperty("description") JsonNode description
) {}