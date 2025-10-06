package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Top-level immutable specification container for a collection of input field definitions.
 * <p>Encapsulates the protocol version (defaulting to {@link #CURRENT_PROTOCOL_VERSION}) and
 * an ordered list of {@link InputFieldSpec} entries. The version enables future evolution while
 * allowing clients to validate compatibility early.</p>
 * <p>Instances are thread-safe and may be freely cached or shared. The {@code fields} list is
 * defensively copied and wrapped unmodifiable on construction.</p>
 *
 * @since 2.0.0
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

    /**
     * @return the declared protocol version string (never null)
     * @since 2.0.0
     */
    public String getProtocolVersion() { return protocolVersion; }

    /**
     * @return unmodifiable ordered list of field specifications (never null)
     * @since 2.0.0
     */
    public List<InputFieldSpec> getFields() { return fields; }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link InputSpec}. Not thread-safe.
     * @since 2.0.0
     */
    public static final class Builder {
        private String protocolVersion = CURRENT_PROTOCOL_VERSION;
        private final List<InputFieldSpec> fields = new ArrayList<>();

        private Builder() {}

    /**
     * Override protocol version (ignored if blank).
     * @since 2.0.0
     */
    public Builder protocolVersion(String protocolVersion) {
            if (protocolVersion != null && !protocolVersion.isBlank()) {
                this.protocolVersion = protocolVersion;
            }
            return this;
        }

    /**
     * Append a single field specification (ignored if null).
     * @since 2.0.0
     */
    public Builder addField(InputFieldSpec field) {
            if (field != null) {
                this.fields.add(field);
            }
            return this;
        }

    /**
     * Replace entire field list with provided collection.
     * @since 2.0.0
     */
    public Builder fields(List<InputFieldSpec> fields) {
            this.fields.clear();
            if (fields != null) {
                this.fields.addAll(fields);
            }
            return this;
        }

        /**
         * Build immutable {@link InputSpec} instance.
         * @since 2.0.0
         */
        public InputSpec build() { return new InputSpec(protocolVersion, fields); }
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
