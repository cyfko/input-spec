package io.github.cyfko.inputspec.spring;

import java.lang.annotation.*;

/**
 * Marks a method as the submission handler for an InputSpec form.
 *
 * The annotated method is invoked by the InputSpec starter after successful
 * stateless validation. It receives the submitted form object, deserialized
 * by Jackson into the type declared as the method parameter.
 *
 * <h3>Startup guarantees</h3>
 * <ul>
 *   <li>Every form present in the FormSpecCache must have exactly one handler.</li>
 *   <li>Two handlers for the same formId → startup failure with a clear message.</li>
 *   <li>A handler referencing an unknown formId → startup failure.</li>
 * </ul>
 *
 * <h3>Method contract</h3>
 * <ul>
 *   <li>Exactly one parameter — the form object (annotated with {@code @FormSpec}).</li>
 *   <li>Return {@link SubmitResponse}.</li>
 *   <li>Must be on a Spring-managed bean ({@code @Service} or {@code @Component}).</li>
 * </ul>
 *
 * Example:
 * <pre>
 *   {@literal @}Service
 *   public class BookingService {
 *
 *       {@literal @}FormHandler("booking-form")
 *       public SubmitResponse handle(BookingForm form) {
 *           Booking saved = repository.save(toEntity(form));
 *           return SubmitResponse.ok(saved);
 *       }
 *   }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FormHandler {

    /**
     * The id of the form this method handles.
     * Must match the {@code id} declared in {@code @FormSpec(id = "...")}
     * on the corresponding form class.
     */
    String value();
}
