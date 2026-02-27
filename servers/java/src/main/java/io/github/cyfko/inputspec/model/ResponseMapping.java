package io.github.cyfko.inputspec.model;// ─── ResponseMapping ─────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseMapping(
    String dataField,
    String totalField,
    String hasNextField
) {}