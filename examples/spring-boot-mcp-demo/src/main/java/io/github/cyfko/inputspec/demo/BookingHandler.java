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
     *   <li>All required fields are present and non-empty</li>
     *   <li>Email is valid, dates are in the future</li>
     *   <li>Check-out is after check-in (cross-constraint)</li>
     *   <li>Room type is one of STANDARD, DELUXE, SUITE</li>
     *   <li>Guest count is between 1 and 10</li>
     * </ul>
     *
     * @param data the validated form data
     * @return accepted with booking confirmation, or rejected if room unavailable
     */
    @FormHandler("hotel-booking")
    public SubmitResponse handleBooking(Map<String, Object> data) {
        String guestName = (String) data.get("guestName");
        String roomType  = ((BookingForm.RoomType) data.get("roomType")).name();
        String checkIn   = (String) data.get("checkIn");
        String checkOut  = (String) data.get("checkOut");
        int guests       = ((Number) data.get("guests")).intValue();

        // Simulate domain logic: reject SUITE for > 4 guests
        if ("SUITE".equals(roomType) && guests > 4) {
            return SubmitResponse.rejected(
                "Suites accommodate a maximum of 4 guests. "
                + "Please choose a different room type or reduce the number of guests."
            );
        }

        // Create the booking
        String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Map<String, Object> confirmation = Map.of(
            "bookingId", bookingId,
            "guestName", guestName,
            "roomType", roomType,
            "checkIn", checkIn,
            "checkOut", checkOut,
            "guests", guests,
            "status", "CONFIRMED",
            "createdAt", Instant.now().toString()
        );

        bookings.put(bookingId, confirmation);

        System.out.println("✅ Booking " + bookingId + " confirmed for " + guestName
            + " (" + roomType + ", " + checkIn + " → " + checkOut + ", " + guests + " guests)");

        return SubmitResponse.ok(confirmation);
    }
}
