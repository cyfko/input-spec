package io.github.cyfko.inputspec.spring;

import io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationError;

import java.util.List;

/**
 * Result DTO for the {@code inputspec_submit_form} MCP tool.
 *
 * <p>Wraps the outcome of a form submission into a flat, AI-friendly structure
 * with a single {@code status} field for easy branching by the AI agent.</p>
 *
 * <p><b>Possible statuses:</b></p>
 * <ul>
 *   <li>{@code "accepted"} — the form was valid and successfully processed by the {@code @FormHandler}.
 *       The {@code body} contains the handler's response (may be {@code null}).</li>
 *   <li>{@code "rejected"} — the form data was valid structurally, but the domain handler
 *       rejected it (e.g. duplicate booking). The {@code errors} list contains the reasons.</li>
 *   <li>{@code "validation_failed"} — the form data failed stateless validation.
 *       The {@code errors} list contains the constraint violations.</li>
 * </ul>
 *
 * @param status the outcome: {@code "accepted"}, {@code "rejected"}, or {@code "validation_failed"}
 * @param body   the response body from the {@code @FormHandler} (only when {@code status = "accepted"})
 * @param errors the validation or rejection errors (only when {@code status != "accepted"})
 *
 * @see InputSpecMcpTools
 * @see SubmitResponse
 */
public record McpSubmitResult(
    String status,
    Object body,
    List<ValidationError> errors
) {
    /** Creates an accepted result with a response body. */
    public static McpSubmitResult accepted(Object body) {
        return new McpSubmitResult("accepted", body, List.of());
    }

    /** Creates an accepted result without a response body. */
    public static McpSubmitResult accepted() {
        return new McpSubmitResult("accepted", null, List.of());
    }

    /** Creates a rejected result from domain handler errors. */
    public static McpSubmitResult rejected(List<ValidationError> errors) {
        return new McpSubmitResult("rejected", null, errors);
    }

    /** Creates a validation_failed result from constraint violations. */
    public static McpSubmitResult validationFailed(List<ValidationError> errors) {
        return new McpSubmitResult("validation_failed", null, errors);
    }
}
