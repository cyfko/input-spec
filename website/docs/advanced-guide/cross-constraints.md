---
sidebar_position: 1
id: cross-constraints
title: Cross-Constraints
---

# Cross-Constraints

While Jakarta validation is excellent for single-field rules (`@Min`, `@NotNull`), many business rules depend on the relationship *between* fields. 

*   "Check-out date must be after check-in date."
*   "If 'Payment Method' is Credit Card, then 'Card Number' is required."
*   "You can specify a 'Phone Number' OR an 'Email', but you must provide at least one."

InputSpec handles these scenarios natively using **Cross-Constraints**.

## The `@CrossConstraint` Annotation

Cross-constraints are defined at the **Class-level** (not the field level), using the `@CrossConstraint` annotation inside the `@FormSpec`. 

InputSpec supports four types of cross-constraints:

### 1. Field Comparison (`FIELD_COMPARISON`)

Compares the values of two fields using standard operators (`EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`, etc.).

```java
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.CrossConstraint;

@FormSpec(
    id = "booking-form",
    crossConstraints = {
        @CrossConstraint(
            type = CrossConstraint.Type.FIELD_COMPARISON,
            fields = {"checkOut", "checkIn"}, // Output field is first, Reference is second
            operator = CrossConstraint.Operator.GREATER_THAN,
            errorMessage = "Check-out date must be strictly after Check-in date."
        )
    }
)
public class BookingForm {
    private LocalDate checkIn;
    private LocalDate checkOut;
}
```

### 2. Depends On (`DEPENDS_ON`)

A field's requirement or visibility is toggled based on the value of another field.

```java
@FormSpec(
    id = "payment-form",
    crossConstraints = {
        @CrossConstraint(
            type = CrossConstraint.Type.DEPENDS_ON,
            fields = {"creditCardNumber", "paymentMethod"}, // Output field, Dependency field
            expectedValue = "CREDIT_CARD",
            errorMessage = "Credit Card Number is required when paying by card."
        )
    }
)
public class PaymentForm {
    private String paymentMethod; // e.g., "PAYPAL" or "CREDIT_CARD"
    private String creditCardNumber;
}
```

### 3. Mutually Exclusive (`MUTUALLY_EXCLUSIVE`)

Ensures that only *one* of the specified fields can be populated.

```java
@FormSpec(
    id = "contact-preferences",
    crossConstraints = {
        @CrossConstraint(
            type = CrossConstraint.Type.MUTUALLY_EXCLUSIVE,
            fields = {"homePhone", "mobilePhone"},
            errorMessage = "Please provide EITHER a home phone OR a mobile phone, not both."
        )
    }
)
public class ContactPreferences {
    private String homePhone;
    private String mobilePhone;
}
```

### 4. At Least One (`AT_LEAST_ONE`)

Ensures that *minimum one* of the specified fields is populated.

```java
@FormSpec(
    id = "contact-methods",
    crossConstraints = {
        @CrossConstraint(
            type = CrossConstraint.Type.AT_LEAST_ONE,
            fields = {"email", "phoneNumber"},
            errorMessage = "You must provide either an email address or a phone number so we can reach you."
        )
    }
)
public class ContactMethods {
    private String email;
    private String phoneNumber;
}
```

## How it's executed

When you declare a Cross-Constraint:
1. It is exported into the `[form-id].json` file under the `"crossConstraints"` array.
2. A smart frontend library can use this array to dynamically show/hide fields (e.g., hiding `creditCardNumber` if `paymentMethod` != "CREDIT_CARD") or render red text below fields if the dates are inverse.
3. The Spring Boot backend (`input-spec-spring-boot-starter`) **automatically executes these rules** alongside your Jakarta annotations before ever hitting your logic. If the user bypassed the UI and sent an invalid JSON payload, a `400 Bad Request` is still thrown, guaranteeing backend integrity.
