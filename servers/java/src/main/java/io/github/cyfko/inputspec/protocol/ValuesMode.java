package io.github.cyfko.inputspec.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Determines whether a {@link io.github.cyfko.inputspec.model.ValuesEndpoint}
 * defines a closed domain or free-form suggestions (§2.2).
 *
 * <p>This controls how the UI client enforces user input:</p>
 * <ul>
 *   <li>{@link #CLOSED} — the user <em>must</em> select one (or more) of the provided values.
 *       Free-text input is not allowed. The validator rejects values not in the list.</li>
 *   <li>{@link #SUGGESTIONS} — the provided values are hints only. The user may type
 *       a free-form value that is not in the list. The validator does not enforce membership.</li>
 * </ul>
 *
 * @see io.github.cyfko.inputspec.model.ValuesEndpoint
 */
public enum ValuesMode {

    /** User must pick from the provided values — membership is enforced. */
    CLOSED,

    /** Values are suggestions — free-text input is permitted. */
    SUGGESTIONS;

    /**
     * Deserializes a JSON string to the corresponding {@code ValuesMode}.
     *
     * <p>Case-insensitive. Returns {@link #CLOSED} as the default
     * for {@code null} or unrecognized values.</p>
     *
     * @param v the JSON string value (may be {@code null})
     * @return the matching enum constant, or {@link #CLOSED} as default
     */
    @JsonCreator
    public static ValuesMode fromJson(String v) {
        if (v == null) return CLOSED;
        return switch (v.toUpperCase()) {
            case "SUGGESTIONS" -> SUGGESTIONS;
            default            -> CLOSED;
        };
    }
}