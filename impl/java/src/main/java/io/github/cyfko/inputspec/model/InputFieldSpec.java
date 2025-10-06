package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public DataType getDataType() { return dataType; }
    public boolean isExpectMultipleValues() { return expectMultipleValues; }
    public boolean isRequired() { return required; }
    public ValuesEndpoint getValuesEndpoint() { return valuesEndpoint; }
    public List<ConstraintDescriptor> getConstraints() { return constraints; }
    public String getFormatHint() { return formatHint; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String displayName;
        private String description;
        private DataType dataType;
        private boolean expectMultipleValues;
        private boolean required;
        private ValuesEndpoint valuesEndpoint;
        private List<ConstraintDescriptor> constraints = List.of();
        private String formatHint;

        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder dataType(DataType dataType) { this.dataType = dataType; return this; }
        public Builder expectMultipleValues(boolean expectMultipleValues) { this.expectMultipleValues = expectMultipleValues; return this; }
        public Builder required(boolean required) { this.required = required; return this; }
        public Builder valuesEndpoint(ValuesEndpoint valuesEndpoint) { this.valuesEndpoint = valuesEndpoint; return this; }
        public Builder constraints(List<ConstraintDescriptor> constraints) { this.constraints = constraints; return this; }
        public Builder formatHint(String formatHint) { this.formatHint = formatHint; return this; }
        public InputFieldSpec build() { return new InputFieldSpec(this); }
    }
}
