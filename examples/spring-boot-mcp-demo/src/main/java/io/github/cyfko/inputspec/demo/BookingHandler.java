package io.github.cyfko.inputspec.demo;

import io.github.cyfko.inputspec.spring.FormHandler;
import io.github.cyfko.inputspec.spring.SubmitResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Domain handler for the "hotel-booking" form.
 *
 * <p>This is where the developer's business logic lives — completely
 * under their control. InputSpec guarantees that the data reaching
 * this method has already been validated against the form spec.</p>
 *
 * <p>The method:</p>
 * <ol>
 *   <li>Receives pre-validated form data as a {@code Map<String, Object>}</li>
 *   <li>Applies domain rules (e.g. room availability check)</li>
 *   <li>Returns {@code SubmitResponse.ok(body)} or {@code SubmitResponse.rejected(message)}</li>
 * </ol>
 */
@Component
public class BookingHandler {

    /** In-memory booking store — replace with a real database in production. */
    private final Map<String, Map<String, Object>> bookings = new ConcurrentHashMap<>();

    /**
     * Processes a hotel booking submission.
     *
     * <p>InputSpec guarantees at this point:</p>
     * <ul>
     *   <li>The framework automatically instantiated the {@code BookingForm} and mapped all fields</li>
     *   <li>All required fields are present and non-empty</li>
     *   <li>Email is valid, dates are in the future</li>
     *   <li>Check-out is after check-in (cross-constraint)</li>
     *   <li>Room type is one of STANDARD, DELUXE, SUITE</li>
     *   <li>Guest count is between 1 and 10</li>
     * </ul>
     *
     * @param form the fully mapped and validated form object
     * @return accepted with booking confirmation, or rejected if room unavailable
     */
    @FormHandler("hotel-booking")
    public SubmitResponse handleBooking(BookingForm form) {
        // Create the booking
        String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Map<String, Object> confirmation = Map.of(
            "bookingId", bookingId,
            "guestName", form.getGuestName(),
            "roomType", form.getRoomType(),
            "checkIn", form.getCheckIn(),
            "checkOut", form.getCheckOut(),
            "guests", form.getGuests(),
            "status", "CONFIRMED",
            "createdAt", Instant.now().toString()
        );

        bookings.put(bookingId, confirmation);

        System.out.println("✅ Booking " + bookingId + " confirmed for " + form.getGuestName()
            + " (" + form.getRoomType() + ", " + form.getCheckIn() + " → " + form.getCheckOut() + ", " + form.getGuests() + " guests)");

        return SubmitResponse.ok(confirmation);
    }
}
