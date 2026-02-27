package io.github.cyfko.inputspec.model;// ─── RequestParams ────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestParams(
    String  pageParam,
    String  limitParam,
    Integer defaultLimit
) {}