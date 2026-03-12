package io.github.cyfko.inputspec.spring;

import io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationError;

import java.util.List;

/**
 * The result of a {@code @FormHandler} method.
 *
 * Two outcomes are possible:
 *
 *   <b>Accepted</b> — submission accepted by domain logic.
 *               HTTP: 201 Created (with body) or 204 No Content (without).
 *
 *   <b>Rejected</b> — submission rejected by domain logic (stateful validation).
 *               Errors use the same format as stateless ValidationErrors:
 *               path=null, constraintName="server" for global rejections.
 *               HTTP: 200 OK with { isValid: false, errors: [...] }
 *
 * Using the same error format for both stateless and stateful rejections is
 * a deliberate protocol decision — the client has a single rendering path
 * regardless of where the rejection originated.
 */
public sealed interface SubmitResponse permits SubmitResponse.Accepted, SubmitResponse.Rejected {

    // ─── Factory methods ─────────────────────────────────────────────────────

    /** Submission accepted, no body. */
    static SubmitResponse ok() {
        return new Accepted(null);
    }

    /**
     * Submission accepted with a response body.
     * Typically the persisted resource — serialized by Jackson.
     */
    static SubmitResponse ok(Object body) {
        return new Accepted(body);
    }

    /**
     * Submission rejected by domain logic — single global message.
     * Should be human-readable and include remediation hints when applicable.
     */
    static SubmitResponse rejected(String message) {
        return new Rejected(List.of(
            ValidationError.field(null, "server", message, null)
        ));
    }

    /**
     * Submission rejected with multiple domain errors.
     * Each error may target a specific field via {@code path},
     * or be global with {@code path: null}.
     */
    static SubmitResponse rejected(List<ValidationError> errors) {
        return new Rejected(List.copyOf(errors));
    }

    // ─── Variants ────────────────────────────────────────────────────────────

    record Accepted(Object body) implements SubmitResponse {}

    record Rejected(List<ValidationError> errors) implements SubmitResponse {}
}
