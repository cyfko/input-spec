package io.github.cyfko.inputspec.spring;

import java.lang.annotation.*;

/**
 * Marks a method as the submission handler for a DIFSP form.
 *
 * <h2>How it works</h2>
 *
 * The DIFSP starter infers the target form id by reading the {@code @FormSpec}
 * annotation on the method's parameter type. No explicit id needs to be declared —
 * the binding is derived from the parameter class and verified at application startup.
 *
 * <pre>
 *   // BookingForm is annotated @FormSpec(id = "booking-form")
 *   // → the starter automatically resolves this method as the handler for "booking-form"
 *
 *   {@literal @}Service
 *   public class BookingService {
 *
 *       {@literal @}FormHandler
 *       public SubmitResponse handle(BookingForm form) {
 *           return SubmitResponse.ok(repository.save(toEntity(form)));
 *       }
 *   }
 * </pre>
 *
 * <h2>Method contract</h2>
 *
 * The annotated method MUST:
 * <ul>
 *   <li>Declare exactly <strong>one parameter</strong> — the form object.</li>
 *   <li>Have that parameter type annotated with {@code @FormSpec}.</li>
 *   <li>Return {@link SubmitResponse}.</li>
 *   <li>Be declared on a Spring-managed bean ({@code @Service}, {@code @Component}, etc.).</li>
 * </ul>
 *
 * The method is invoked <strong>only after stateless validation has passed</strong>.
 * It receives the form object deserialized by Jackson from the submitted values.
 * Any stateful business logic (availability checks, uniqueness, authorization, etc.)
 * belongs here and MUST be expressed as a {@link SubmitResponse}.
 *
 * <h2>Startup invariants</h2>
 *
 * The application refuses to start if any of the following conditions is violated:
 * <ul>
 *   <li>A form present in the cache has no registered handler.</li>
 *   <li>Two handlers are registered for the same form.</li>
 *   <li>The method parameter type is not annotated with {@code @FormSpec}.</li>
 *   <li>The method does not return {@link SubmitResponse}.</li>
 *   <li>The method declares more than one parameter.</li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * Accepted submission returning a created resource:
 * <pre>
 *   {@literal @}FormHandler
 *   public SubmitResponse handle(BookingForm form) {
 *       Booking saved = repository.save(toEntity(form));
 *       return SubmitResponse.ok(saved);
 *   }
 * </pre>
 *
 * Business rejection (stateful server-side logic):
 * <pre>
 *   {@literal @}FormHandler
 *   public SubmitResponse handle(BookingForm form) {
 *       if (!availabilityService.isAvailable(form.getRoomType(), form.getStartDate())) {
 *           return SubmitResponse.rejected("Room not available for the selected dates");
 *       }
 *       return SubmitResponse.ok(repository.save(toEntity(form)));
 *   }
 * </pre>
 *
 * @see SubmitResponse
 * @see io.github.cyfko.inputspec.FormSpec
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FormHandler {}
