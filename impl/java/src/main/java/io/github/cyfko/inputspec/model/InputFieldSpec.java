package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Immutable field specification describing a single logical input in the protocol.
 * <p>Key aspects:
 * <ul>
 *   <li>{@code dataType} – high level value type (STRING, NUMBER, DATE, BOOLEAN).</li>
 *   <li>{@code expectMultipleValues} – when {@code true} the value is a list and length constraints apply to collection size.</li>
 *   <li>{@code constraints} – ordered list of {@link ConstraintDescriptor} applied after REQUIRED/TYPE/MEMBERSHIP.</li>
 *   <li>{@code valuesEndpoint} – optional domain enumeration / suggestion source.</li>
 *   <li>{@code formatHint} – optional UI hint (placeholder / formatting guidance).</li>
 * </ul>
 * Instances are created via {@link #builder()} or the Jackson constructor.
 *
 * @since 2.0.0
 */
public final class InputFieldSpec {
    private final String displayName;
    private final String description;
    private final DataType dataType;
    private final boolean expectMultipleValues;
    private final boolean required;
    private final ValuesEndpoint valuesEndpoint; // nullable
    private final List<ConstraintDescriptor> constraints; // never null (empty allowed)
    private final String formatHint; // optional

    private InputFieldSpec(Builder b) {
        this.displayName = b.displayName;
        this.description = b.description;
        this.dataType = b.dataType;
        this.expectMultipleValues = b.expectMultipleValues;
        this.required = b.required;
        this.valuesEndpoint = b.valuesEndpoint;
        this.constraints = b.constraints;
        this.formatHint = b.formatHint;
    }

    @JsonCreator
    public InputFieldSpec(
            @JsonProperty(value = "displayName", required = true) String displayName,
            @JsonProperty("description") String description,
            @JsonProperty(value = "dataType", required = true) DataType dataType,
            @JsonProperty(value = "expectMultipleValues", required = true) boolean expectMultipleValues,
            @JsonProperty(value = "required", required = true) boolean required,
            @JsonProperty("valuesEndpoint") ValuesEndpoint valuesEndpoint,
            @JsonProperty(value = "constraints", required = true) List<ConstraintDescriptor> constraints,
            @JsonProperty("formatHint") String formatHint) {
        this.displayName = displayName;
        this.description = description;
        this.dataType = dataType;
        this.expectMultipleValues = expectMultipleValues;
        this.required = required;
        this.valuesEndpoint = valuesEndpoint;
        this.constraints = constraints;
        this.formatHint = formatHint;
    }

    /** @return human readable label for UI display (non-null) @since 2.0.0 */
    public String getDisplayName() { return displayName; }
    /** @return optional free-form description (may be null) @since 2.0.0 */
    public String getDescription() { return description; }
    /** @return declared high-level {@link DataType} @since 2.0.0 */
    public DataType getDataType() { return dataType; }
    /** @return whether the field expects a list of values @since 2.0.0 */
    public boolean isExpectMultipleValues() { return expectMultipleValues; }
    /** @return whether a non-empty value is mandatory @since 2.0.0 */
    public boolean isRequired() { return required; }
    /** @return optional value domain endpoint (null if not restricted) @since 2.0.0 */
    public ValuesEndpoint getValuesEndpoint() { return valuesEndpoint; }
    /** @return ordered immutable list of constraints (never null) @since 2.0.0 */
    public List<ConstraintDescriptor> getConstraints() { return constraints; }
    /** @return optional UI formatting hint @since 2.0.0 */
    public String getFormatHint() { return formatHint; }

    public static Builder builder() { return new Builder(); }

    /** Builder for {@link InputFieldSpec}. Not thread-safe. @since 2.0.0 */
    public static final class Builder {
        private String displayName;
        private String description;
        private DataType dataType;
        private boolean expectMultipleValues;
        private boolean required;
        private ValuesEndpoint valuesEndpoint;
        private List<ConstraintDescriptor> constraints = List.of();
        private String formatHint;

        /** Set UI display name (required). @since 2.0.0 */
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        /** Optional human description. @since 2.0.0 */
        public Builder description(String description) { this.description = description; return this; }
        /** Declared data type (required). @since 2.0.0 */
        public Builder dataType(DataType dataType) { this.dataType = dataType; return this; }
        /** Enable multi-value semantics (list). @since 2.0.0 */
        public Builder expectMultipleValues(boolean expectMultipleValues) { this.expectMultipleValues = expectMultipleValues; return this; }
        /** Mark field as required. @since 2.0.0 */
        public Builder required(boolean required) { this.required = required; return this; }
        /** Provide value domain endpoint (null = unrestricted). @since 2.0.0 */
        public Builder valuesEndpoint(ValuesEndpoint valuesEndpoint) { this.valuesEndpoint = valuesEndpoint; return this; }
        /** Ordered list of constraints (replaces existing). @since 2.0.0 */
        public Builder constraints(List<ConstraintDescriptor> constraints) { this.constraints = constraints; return this; }
        /** Optional formatting hint for UI. @since 2.0.0 */
        public Builder formatHint(String formatHint) { this.formatHint = formatHint; return this; }
        /** Build immutable spec. @since 2.0.0 */
        public InputFieldSpec build() { return new InputFieldSpec(this); }
    }
}
