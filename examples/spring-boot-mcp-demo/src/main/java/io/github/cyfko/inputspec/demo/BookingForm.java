package io.github.cyfko.inputspec.demo;

import io.github.cyfko.inputspec.*;
import io.github.cyfko.inputspec.protocol.CrossConstraintType;
import jakarta.validation.constraints.*;

/**
 * Hotel booking form — a realistic example demonstrating all InputSpec features.
 *
 * <p>This class:</p>
 * <ul>
 *   <li>Uses {@code @FormSpec} to declare a form with i18n support</li>
 *   <li>Uses Jakarta Validation annotations ({@code @NotBlank}, {@code @Email}, {@code @Future}, etc.)
 *       which the annotation processor maps to DIFSP {@code ConstraintDescriptor}s</li>
 *   <li>Uses {@code @FieldMeta} for display metadata and value sources</li>
 *   <li>Uses {@code @CrossConstraint} for inter-field validation rules</li>
 * </ul>
 *
 * <p>At compile time, the annotation processor generates:</p>
 * <ul>
 *   <li>{@code META-INF/difsp/hotel-booking.json} — the full form spec</li>
 *   <li>{@code META-INF/difsp/i18n/hotel-booking.properties} — i18n bundle skeleton</li>
 * </ul>
 */
@FormSpec(
    id = "hotel-booking",
    displayName = "Hotel Booking",
    description = "Book a hotel room with your preferred dates and room type.",
    submitUri = "/api/bookings",
    submitMethod = "POST"
)
@CrossConstraint(
    name = "checkOutAfterCheckIn",
    type = CrossConstraintType.FIELD_COMPARISON,
    fields = {"checkOut", "checkIn"},
    operator = io.github.cyfko.inputspec.protocol.ComparisonOperator.GT,
    errorMessage = "Check-out date must be after check-in date"
)
public class BookingForm {

    /**
     * Full name of the guest making the reservation.
     */
    @NotBlank
    @Size(min = 2, max = 100)
    @FieldMeta(displayName = "Guest Name", description = "Full name as it appears on your ID")
    String guestName;

    /**
     * Contact email for booking confirmation.
     */
    @NotBlank
    @Email
    @FieldMeta(displayName = "Email", description = "We'll send the booking confirmation here")
    String email;

    /**
     * Desired check-in date (must be in the future).
     */
    @NotNull
    @Future
    @FieldMeta(displayName = "Check-in Date", description = "Date of arrival (ISO-8601)")
    String checkIn;

    /**
     * Desired check-out date (must be in the future and after check-in).
     */
    @NotNull
    @Future
    @FieldMeta(displayName = "Check-out Date", description = "Date of departure (ISO-8601)")
    String checkOut;

    public enum RoomType {
        STANDARD, DELUXE, SUITE
    }

    /**
     * Room type — the guest must choose from the available options.
     * The processor automatically infers an INLINE CLOSED values source from this enum.
     */
    @NotNull
    @FieldMeta(
        displayName = "Room Type",
        description = "Select your preferred room category"
    )
    RoomType roomType;

    /**
     * Number of guests (1 to 10).
     */
    @NotNull
    @Min(1)
    @Max(10)
    @FieldMeta(displayName = "Number of Guests", description = "How many people will be staying")
    Integer guests;

    /**
     * Optional special requests or notes.
     */
    @Size(max = 500)
    @FieldMeta(displayName = "Special Requests", description = "Any special requirements (optional)")
    String specialRequests;
}
