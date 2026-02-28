---
sidebar_position: 2
id: custom-handlers
title: Custom Handlers
---

# Custom Validation Handlers

InputSpec handles standard format rules (email, length, min/max) and cross-field comparisons automatically. But what if your validation requires checking a database, calling an external API, or applying a proprietary cryptographic rule?

For example:
*   "Username must be unique in the database."
*   "Coupon code must be active in Stripe."
*   "IBAN must match the specified country code."

These rules cannot be resolved statically by a JSON parser. They require server-side context. For this, InputSpec allows you to register **Custom Handlers**.

## Defining Custom Constraints in Java

Because InputSpec generates protocol schemas at *compile time*, it cannot automatically execute generic Jakarta `@ConstraintValidator` logic. Instead, you declare custom validation rules using a **Custom Cross-Constraint**, which can apply to one or more fields.

```java
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.CrossConstraint;
import io.github.cyfko.inputspec.protocol.CrossConstraintType;

@FormSpec(
    id = "registration"
)
@CrossConstraint(
    name = "uniqueUser",
    type = CrossConstraintType.CUSTOM,
    customKey = "checkUniqueUsername", // The routing key for the backend
    fields = {"username"},
    errorMessage = "This username is already taken"
)
public class RegistrationForm {
    private String username;
}
```

## The Generated JSON

When the InputSpec processor encounters this annotation, it exports a `CUSTOM` cross-constraint to the JSON file:

```json
{
  "name": "uniqueUser",
  "type": "custom",
  "fields": ["username"],
  "params": {
    "key": "checkUniqueUsername"
  },
  "errorMessage": "This username is already taken"
}
```

The frontend knows it cannot validate a `custom` rule locally. It will simply wait until the user clicks "Submit" to see if the server rejects it.

## Registering a Backend Handler (Spring Boot)

To execute this validation logic on the backend, you simply define a Spring Component and annotate a method with `@FormValidator`, passing the `customKey` as its value.

> [!TIP]
> The method must accept your `@FormSpec` annotated POJO. Return `Optional<String>` to denote an error message. If the optional is empty, validation is considered successful.

```java
import io.github.cyfko.inputspec.validation.FormValidator;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserValidationService {

    private final UserRepository userRepository;

    public UserValidationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Maps to the customKey defined in @CrossConstraint
    @FormValidator("checkUniqueUsername")
    public Optional<String> validateConstraint(RegistrationForm form) {
        if (form.getUsername() == null) {
            return Optional.empty(); // Let the standard @NotNull deal with missing values
        }
        
        if (userRepository.existsByUsername(form.getUsername())) {
            return Optional.of("This username is already taken."); // Error message
        }
        
        return Optional.empty(); // Valid
    }
}
```

## Global Form Validation

Sometimes, validation rules are so complex or cross-cutting that they don't apply to specific fields, but to the entire form payload as a whole. You can execute global business logic by registering a form-level validator.

To do this, use `@FormValidator` but provide the **Form ID** as the value, and return a `Map<String, String>` where the keys are the specific field paths causing errors, and the values are their messages.

```java
@Service
public class BookingValidationService {

    // Maps to the form's defined ID: @FormSpec(id = "booking-form")
    @FormValidator("booking-form")
    public Map<String, String> validateEntireForm(BookingForm form) {
        Map<String, String> errors = new HashMap<>();

        // Perform complex API lookups or calculations spanning multiple fields
        if (form.getDiscountCode() != null && !isEligible(form.getUserId(), form.getDiscountCode())) {
            errors.put("discountCode", "You are not eligible for this discount.");
        }

        return errors; // Return an empty map if valid
    }
}
```

## The 3-Phase Execution Pipeline

InputSpec enforces a strict execution pipeline to prevent your complex API calls from executing on obviously invalid payloads. Your methods are only invoked when it is safe to do so.

1.  **Phase 1 (Standard Validation)**: Runs fundamental checks (length, required, static cross-comparisons). It fails immediately if any of these basic rules are broken.
2.  **Phase 2 (Custom Constraint Validation)**: If Phase 1 succeeds, it evaluates your `CUSTOM` cross-constraints (methods returning `Optional<String>`). It collects all field errors and aborts if any are found.
3.  **Phase 3 (Global Validation)**: Only if Phase 1 and Phase 2 are flawless does the form-level `@FormValidator` (method returning `Map<String, String>`) execute to perform cross-cutting business logic.

If all phases succeed, the validated payload is officially delegated to your `@FormHandler` endpoint connection.
