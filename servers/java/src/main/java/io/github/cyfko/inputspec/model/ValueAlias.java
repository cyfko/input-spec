package io.github.cyfko.inputspec.model;// ─── ValueAlias ───────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ValueAlias(
    @JsonProperty("value") JsonNode value,
    @JsonProperty("label") JsonNode label
) {}
