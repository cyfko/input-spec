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

First, define your custom rule using standard Jakarta Bean Validation annotation patterns.

```java
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueUsernameValidator.class) // Standard Jakarta validator
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueUsername {
    String message() default "This username is already taken";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

And define the form:

```java
public class RegistrationForm {
    @UniqueUsername
    private String username;
}
```

## Bridging to InputSpec JSON

When the InputSpec processor encounters `@UniqueUsername`, it doesn't know what it is natively. It defaults to exporting a `CUSTOM` constraint to the JSON file:

```json
{
  "type": "CUSTOM",
  "namespace": "com.example.validators.UniqueUsername",
  "message": "This username is already taken"
}
```

However, the frontend has no idea how to validate `com.example.validators.UniqueUsername`.

## Registering a Backend Handler

To truly secure this, when the JSON is submitted to the backend, the `FormSpecValidator` must know how to execute the custom logic. 

You do this by registering a callback:

```java
import io.github.cyfko.inputspec.validation.FormSpecValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InputSpecConfig {

    private final UserRepository userRepository;

    public InputSpecConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public FormSpecValidator formSpecValidator() {
        FormSpecValidator validator = new FormSpecValidator();
        
        // Register the custom logic for our specific namespace
        validator.registerCustomHandler("com.example.validators.UniqueUsername", (value, context) -> {
            if (value == null) return; // Let @NotNull handle this
            
            String username = (String) value;
            if (userRepository.existsByUsername(username)) {
                context.addError("username", "This username is already taken.");
            }
        });

        return validator;
    }
}
```

## The Workflow

1.  **Frontend**: Reads the JSON. Sees `type: "CUSTOM"`. The frontend knows it cannot validate this immediately. It waits until the user clicks "Submit".
2.  **Submission**: The JSON hits the backend via `/api/forms/registration`.
3.  **Core Validation**: `FormSpecValidator` runs standard rules (length, email, etc.).
4.  **Custom Validation**: `FormSpecValidator` hits the `CUSTOM` rule. It looks up the associated lambda in its registry using the namespace string. It executes the database query.
5.  **Rejection**: If the lambda adds an error to the context, the request is immediately aborted, returning a `400 Bad Request` with the precise `username` error message. Your `@FormHandler` method *is never called*.

### Why is this better than normal Spring Validation?

In a standard Spring Boot app, you'd write a standard `@ConstraintValidator`. That works fine.

However, explicitly registering the custom handler with InputSpec ensures that the validation pipeline remains centralized within the `FormSpecValidator`. This guarantees that your form submissions, whether coming from a web browser or an **AI MCP Agent**, go through the exact same rigorous, predictable rule pipeline before instantiating your POJOs.
