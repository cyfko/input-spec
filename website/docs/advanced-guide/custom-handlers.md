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

## Registering a Backend Handler

To truly secure this, when the JSON is submitted to the backend, the `FormSpecValidator` must know how to execute the custom logic. 

You do this by registering a callback matching the `customKey`:

```java
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Optional;

@Configuration
public class InputSpecConfig {

    private final UserRepository userRepository;

    public InputSpecConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public FormSpecValidator formSpecValidator() {
        FormSpecValidator validator = new FormSpecValidator();
        
        // Register the custom logic for our specific key
        validator.registerCustomCrossHandler("checkUniqueUsername", (fieldValues, params) -> {
            Object value = fieldValues.get("username");
            if (value == null) return Optional.empty(); // Let @NotNull handle missing values
            
            String username = String.valueOf(value);
            if (userRepository.existsByUsername(username)) {
                return Optional.of("This username is already taken."); // Returns an error message
            }
            return Optional.empty(); // Valid
        });

        return validator;
    }
}
```

## The Workflow

1.  **Frontend**: Reads the JSON. Sees `type: "CUSTOM"`. The frontend knows it cannot validate this immediately. It waits until the user clicks "Submit".
2.  **Submission**: The JSON hits the backend via `/api/forms/registration`.
3.  **Core Validation**: `FormSpecValidator` runs standard rules (length, email, etc.).
4.  **Custom Validation**: `FormSpecValidator` hits the `CUSTOM` rule. It looks up the associated lambda in its registry using the `customKey` string (`checkUniqueUsername`). It executes the database query.
5.  **Rejection**: If the lambda returns an `Optional.of("Error")`, the request is immediately aborted, returning a `400 Bad Request` with the precise error message attached to the `uniqueUser` rule. Your `@FormHandler` method *is never called*.

### Why is this better than normal Spring Validation?

In a standard Spring Boot app, you'd write a standard `@ConstraintValidator`. That works fine.

However, explicitly registering the custom handler with InputSpec ensures that the validation pipeline remains centralized within the `FormSpecValidator`. This guarantees that your form submissions, whether coming from a web browser or an **AI MCP Agent**, go through the exact same rigorous, predictable rule pipeline before instantiating your POJOs.
