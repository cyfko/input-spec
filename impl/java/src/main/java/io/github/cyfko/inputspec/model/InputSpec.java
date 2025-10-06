package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Top-level specification container.
 * <p>
 * Holds the protocol version and the list of defined input field specifications.
 * The {@link #protocolVersion} defaults to {@link #CURRENT_PROTOCOL_VERSION} when built
 * unless explicitly overridden.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class InputSpec {

    /** Current supported protocol version. */
    public static final String CURRENT_PROTOCOL_VERSION = "2.0"; // keep in sync with PROTOCOL_SPECIFICATION.md

    private final String protocolVersion;
    private final List<InputFieldSpec> fields;

    @JsonCreator
    public InputSpec(
            @JsonProperty(value = "protocolVersion", required = true) String protocolVersion,
            @JsonProperty(value = "fields", required = true) List<InputFieldSpec> fields) {
        this.protocolVersion = protocolVersion == null ? CURRENT_PROTOCOL_VERSION : protocolVersion;
        this.fields = fields == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(fields));
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public List<InputFieldSpec> getFields() {
        return fields;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String protocolVersion = CURRENT_PROTOCOL_VERSION;
        private final List<InputFieldSpec> fields = new ArrayList<>();

        private Builder() {}

        public Builder protocolVersion(String protocolVersion) {
            if (protocolVersion != null && !protocolVersion.isBlank()) {
                this.protocolVersion = protocolVersion;
            }
            return this;
        }

        public Builder addField(InputFieldSpec field) {
            if (field != null) {
                this.fields.add(field);
            }
            return this;
        }

        public Builder fields(List<InputFieldSpec> fields) {
            this.fields.clear();
            if (fields != null) {
                this.fields.addAll(fields);
            }
            return this;
        }

        public InputSpec build() {
            return new InputSpec(protocolVersion, fields);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputSpec inputSpec = (InputSpec) o;
        return Objects.equals(protocolVersion, inputSpec.protocolVersion) && Objects.equals(fields, inputSpec.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocolVersion, fields);
    }

    @Override
    public String toString() {
        return "InputSpec{" +
                "protocolVersion='" + protocolVersion + '\'' +
                ", fields=" + fields +
                '}';
    }
}
