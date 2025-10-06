package io.github.cyfko.inputspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Domain membership strictness abstraction mirroring {@link ValuesEndpoint.Mode} but usable
 * independently of a full {@link ValuesEndpoint} instance.
 * <p>Backward compatibility legacy tokens are still accepted on input:
 * STRICT -> CLOSED, LENIENT -> SUGGESTIONS.</p>
 * <p>Unknown tokens default to {@link #CLOSED} as a conservative choice.</p>
 * @since 2.0.0
 */
public enum ClosedDomainMode {
    CLOSED,          // replaces legacy STRICT
    SUGGESTIONS;     // replaces legacy LENIENT

    @JsonCreator
    public static ClosedDomainMode fromString(String value) {
        if (value == null) return null;
        String upper = value.toUpperCase();
        // Backward compatibility aliases
        if ("STRICT".equals(upper)) return CLOSED;
        if ("LENIENT".equals(upper)) return SUGGESTIONS;
        try {
            return ClosedDomainMode.valueOf(upper);
        } catch (IllegalArgumentException ex) {
            // Unknown future value -> default CLOSED (strict interpretation) or could return null
            return CLOSED;
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
