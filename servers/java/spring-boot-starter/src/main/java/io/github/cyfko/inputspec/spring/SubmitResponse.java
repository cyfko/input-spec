package io.github.cyfko.inputspec.spring;

import io.github.cyfko.inputspec.validation.FormSpecValidator.ValidationError;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Represents the outcome of a submission attempt.
 * <p>
 * A {@code SubmitResponse} is either:
 * </p>
 * <ul>
 *   <li>{@link Accepted} – the submission was successfully processed (optionally with a body and HTTP status), or</li>
 *   <li>{@link Rejected} – the submission was rejected by domain validation rules, with one or more errors.</li>
 * </ul>
 *
 * <p>
 * Static factory methods provide convenient, intention-revealing ways to construct common responses.
 * </p>
 */
public sealed interface SubmitResponse permits SubmitResponse.Accepted, SubmitResponse.Rejected {

    // ─── Factory methods ─────────────────────────────────────────────────────

    /**
     * Creates a successful submission response without a response body.
     * <p>
     * The resulting {@link Accepted} instance will ultimately be normalized to use
     * {@link HttpStatus#NO_CONTENT} as HTTP status, since there is no payload to return.
     * </p>
     *
     * @return an accepted response with no body
     */
    static SubmitResponse ok() {
        return new Accepted(null, HttpStatus.OK);
    }

    /**
     * Creates a successful submission response with a response body.
     * <p>
     * This is typically used to return the persisted or updated resource, which will be
     * serialized (for example by Jackson) in the HTTP response. If the status is not
     * overridden, it defaults to {@link HttpStatus#OK}.
     * </p>
     *
     * @param body the response payload to send back to the client; may be {@code null}
     * @return an accepted response carrying the given body
     */
    static SubmitResponse ok(Object body) {
        return new Accepted(body, HttpStatus.OK);
    }

    /**
     * Creates a successful submission response with a response body and a custom HTTP status code.
     * <p>
     * Use this variant when you need to signal specific HTTP semantics:
     * </p>
     * <ul>
     *   <li>{@link HttpStatus#CREATED} when a new resource has been created,</li>
     *   <li>{@link HttpStatus#ACCEPTED} for asynchronous processing,</li>
     *   <li>or any other appropriate 2xx status.</li>
     * </ul>
     * <p>
     * If both {@code body} and {@code status} are {@code null}, the compact constructor
     * will normalize the status to {@link HttpStatus#NO_CONTENT}. If only {@code status}
     * is {@code null}, it will default to {@link HttpStatus#OK}.
     * </p>
     *
     * @param body   the response payload to send back to the client; may be {@code null}
     * @param status the HTTP status to use; may be {@code null} to let the record normalize it
     * @return an accepted response carrying the given body and status
     */
    static SubmitResponse ok(Object body, HttpStatus status) {
        return new Accepted(body, status);
    }

    /**
     * Creates a rejected submission with a single, global domain message.
     * <p>
     * This is a convenience for errors that are not tied to a specific field. The resulting
     * {@link Rejected} response will contain a single {@link ValidationError} with:
     * </p>
     * <ul>
     *   <li>{@code path == null} (global error),</li>
     *   <li>{@code code == "server"},</li>
     *   <li>{@code message} equal to the provided {@code message} parameter.</li>
     * </ul>
     * <p>
     * The HTTP status associated with this rejection can be customized via the {@code status}
     * parameter. When {@code status} is {@code null}, the {@link Rejected} record will default
     * it to {@link HttpStatus#BAD_REQUEST}.
     * </p>
     *
     * @param message human-readable error message describing why the submission was rejected
     * @param status  HTTP status to expose for this rejection; may be {@code null} to let
     *                the {@link Rejected} type normalize it
     * @return a rejected response containing one global validation error and the given status
     */
    static SubmitResponse rejected(String message, HttpStatus status) {
        return new Rejected(List.of(ValidationError.field(null, "server", message, null)), status);
    }


    /**
     * Creates a rejected submission with multiple domain validation errors.
     * <p>
     * Each {@link ValidationError} may optionally target a specific field via its {@code path}
     * (for example {@code "email"} or {@code "address.city"}), or be global when {@code path == null}.
     * The list is defensively copied to preserve immutability of the response.
     * </p>
     *
     * @param errors list of validation errors describing why the submission was rejected
     * @return a rejected response carrying the provided validation errors
     * @throws IllegalArgumentException if {@code errors} is {@code null} or empty
     */
    static SubmitResponse rejected(List<ValidationError> errors) {
        return new Rejected(List.copyOf(errors), HttpStatus.BAD_REQUEST);
    }

    // ─── Variants ────────────────────────────────────────────────────────────

    /**
     * Successful submission result.
     * <p>
     * Represents an accepted submission, optionally carrying a response body and an HTTP status.
     * The compact constructor normalizes the HTTP status according to the following rules:
     * </p>
     * <ul>
     *   <li>If {@code body == null} and {@code status == null}, the status becomes {@link HttpStatus#NO_CONTENT}.</li>
     *   <li>If {@code body != null} but {@code status == null}, the status defaults to {@link HttpStatus#OK}.</li>
     *   <li>If {@code status} is non-null, it is used as-is.</li>
     * </ul>
     *
     * @param body   the response payload; may be {@code null} for no content
     * @param status the HTTP status associated with this response; may be {@code null} and will be normalized
     */
    record Accepted(Object body, HttpStatus status) implements SubmitResponse {
        public Accepted {
            if (body == null && status == null) status = HttpStatus.NO_CONTENT;
            if (status == null) status = HttpStatus.OK;
        }
    }

    /**
     * Failed submission result containing domain validation errors and an HTTP status.
     * <p>
     * This variant is used when a submission is rejected due to business or validation rules.
     * It carries both a list of {@link ValidationError} instances and the HTTP status that
     * should be returned to the client (for example {@code 400 BAD_REQUEST}).
     * </p>
     *
     * <p><b>Status normalization:</b></p>
     * <ul>
     *   <li>If {@code status} is {@code null}, it is automatically normalized to
     *       {@link HttpStatus#BAD_REQUEST}.</li>
     *   <li>If {@code status} is non-null, it is used as-is.</li>
     * </ul>
     *
     * @param errors immutable list of validation errors explaining the rejection
     * @param status HTTP status to expose for this rejection; {@code null} is allowed and will default to 400
     */
    record Rejected(List<ValidationError> errors, HttpStatus status) implements SubmitResponse {
        public Rejected {
            if (status == null) status = HttpStatus.BAD_REQUEST;
        }
    }

}
