package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * v2 atomic constraint descriptor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ConstraintDescriptor {
    private final String name; // stable identifier within a field
    private final ConstraintType type; // strongly typed
    private final Object params; // shape depends on type
    private final String errorMessage; // optional
    private final String description; // optional

    private ConstraintDescriptor(Builder b) {
        this.name = b.name;
        this.type = b.type;
        this.params = b.params;
        this.errorMessage = b.errorMessage;
        this.description = b.description;
    }

    @JsonCreator
    public ConstraintDescriptor(
            @JsonProperty(value = "name", required = true) String name,
        @JsonProperty(value = "type", required = true) ConstraintType type,
            @JsonProperty(value = "params", required = true) Object params,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("description") String description) {
        this.name = name;
        this.type = type;
        this.params = params;
        this.errorMessage = errorMessage;
        this.description = description;
    }

    public String getName() { return name; }
    @JsonProperty("type")
    public ConstraintType getType() { return type; }
    public Object getParams() { return params; }
    public String getErrorMessage() { return errorMessage; }
    public String getDescription() { return description; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String name;
        private ConstraintType type;
        private Object params;
        private String errorMessage;
        private String description;

        public Builder name(String name) { this.name = name; return this; }
        public Builder type(ConstraintType type) { this.type = type; return this; }
        public Builder params(Object params) { this.params = params; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public ConstraintDescriptor build() { return new ConstraintDescriptor(this); }
    }
}
