package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable descriptor of a single ("atomic") constraint attached to an {@link InputFieldSpec}.
 * <p>
 * A constraint couples:
 * <ul>
 *   <li>a stable logical {@code name} unique within the owning field,</li>
 *   <li>a {@link ConstraintType type} that defines the semantic category,</li>
 *   <li>an arbitrary, type-specific {@code params} object whose JSON shape depends on the type,</li>
 *   <li>an optional human friendly {@code errorMessage} for display,</li>
 *   <li>and an optional {@code description} (documentation / tooltip / analytics).</li>
 * </ul>
 * The library treats {@code params} as an opaque JSON subtree; validation logic interprets it based
 * on {@link ConstraintType}. For custom constraint types ({@link ConstraintType#CUSTOM}) integrators
 * can define their own params contract.
 * <p>
 * Instances are created either via Jackson deserialization or the fluent {@link Builder}.
 * All fields are nullable except {@code name}, {@code type} and {@code params} which are required by the
 * protocol. Unknown future constraint kinds are represented as {@link ConstraintType#UNKNOWN} to allow forward compatibility.
 *
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ConstraintDescriptor {
    /** Stable identifier within a field; must be unique in its field scope. */
    private final String name;
    /** Strongly typed category of the constraint. */
    private final ConstraintType type;
    /** Type‑specific parameters (deserialized as raw JSON / {@code Object}). */
    private final Object params;
    /** Optional user friendly override for the error message. */
    private final String errorMessage;
    /** Optional longer form description / tooltip text. */
    private final String description;

    private ConstraintDescriptor(Builder b) {
        this.name = b.name;
        this.type = b.type;
        this.params = b.params;
        this.errorMessage = b.errorMessage;
        this.description = b.description;
    }

    /**
     * Jackson / programmatic constructor.
     *
     * @param name stable logical identifier (unique within its field)
     * @param type concrete constraint type
     * @param params raw parameters object whose JSON shape depends on {@code type}
     * @param errorMessage optional human readable message override
     * @param description optional doc / tooltip text
     * @since 2.0.0
     */
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

    /** @return stable logical identifier unique within the owning field. */
    public String getName() { return name; }
    /** @return the semantic category of this constraint. */
    @JsonProperty("type")
    public ConstraintType getType() { return type; }
    /** @return raw parameters object interpreted according to {@link #getType()}. */
    public Object getParams() { return params; }
    /** @return optional human friendly error message override or {@code null}. */
    public String getErrorMessage() { return errorMessage; }
    /** @return optional descriptive text or {@code null}. */
    public String getDescription() { return description; }

    /**
     * Create a new fluent builder.
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Fluent builder for {@link ConstraintDescriptor}.
     * <p>Only {@code name}, {@code type} and {@code params} are mandatory.</p>
     * @since 2.0.0
     */
    public static final class Builder {
        private String name;
        private ConstraintType type;
        private Object params;
        private String errorMessage;
        private String description;

        /**
         * Set constraint logical name.
         * @since 2.0.0
         */
        public Builder name(String name) { this.name = name; return this; }
        /**
         * Set constraint type.
         * @since 2.0.0
         */
        public Builder type(ConstraintType type) { this.type = type; return this; }
        /**
         * Set raw params object.
         * @since 2.0.0
         */
        public Builder params(Object params) { this.params = params; return this; }
        /**
         * Set optional error message override.
         * @since 2.0.0
         */
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        /**
         * Set optional descriptive text.
         * @since 2.0.0
         */
        public Builder description(String description) { this.description = description; return this; }
        /**
         * Build immutable descriptor instance.
         * @since 2.0.0
         */
        public ConstraintDescriptor build() { return new ConstraintDescriptor(this); }
    }
}
