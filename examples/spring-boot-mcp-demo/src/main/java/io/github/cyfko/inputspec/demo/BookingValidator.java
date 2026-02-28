package io.github.cyfko.inputspec.demo;

import io.github.cyfko.inputspec.validation.FormValidator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Demonstrates the Unified @FormValidator API with Phase 2 and Phase 3 validation.
 */
@Component
public class BookingValidator {

    /**
     * Phase 2: Custom Constraint Validation
     * This corresponds to the CUSTOM cross-constraint "validateSuiteGuestLimit".
     *
     * @param form The populated BookingForm POJO
     * @return Optional containing an error message if invalid, or empty if valid.
     */
    @FormValidator("validateSuiteGuestLimit")
    public Optional<String> validateSuiteGuestLimit(BookingForm form) {
        if (form.getRoomType() == BookingForm.RoomType.SUITE && form.getGuests() > 4) {
            return Optional.of("Suites accommodate a maximum of 4 guests. Please choose a different room type or reduce the number of guests.");
        }
        return Optional.empty();
    }

    /**
     * Phase 3: Global Form Validation
     * This is executed only if Phase 1 (Standard) and Phase 2 (Custom Constraints) are spotless.
     * Applies to the entire "hotel-booking" form.
     *
     * @param form The populated BookingForm POJO
     * @return A map of fieldName -> errorMessage (empty if valid).
     */
    @FormValidator("hotel-booking")
    public Map<String, String> validateGlobalBookingRules(BookingForm form) {
        Map<String, String> errors = new HashMap<>();

        // Example: If a guest requests "Early Check-in", they must book at least the Deluxe room
        if (form.getSpecialRequests() != null 
            && form.getSpecialRequests().toLowerCase().contains("early check-in")
            && form.getRoomType() == BookingForm.RoomType.STANDARD) {
                
            errors.put("roomType", "Early check-in is only available for DELUXE and SUITE rooms.");
            errors.put("specialRequests", "Remove early check-in request to proceed with STANDARD room.");
        }

        return errors;
    }
}
