---
sidebar_position: 1
id: forms
title: Defining Forms
---

# Defining Forms

The core philosophy of InputSpec is that your Java domain model is the single source of truth for your UI.

To expose a pure Java object as an InputSpec form, you use two main annotations: `@FormSpec` at the class level, and `@FieldMeta` at the field level.

## The `@FormSpec` Annotation

This annotation tells the InputSpec Processor to parse the class and generate a JSON file conforming to the Dynamic Input Field Specification Protocol (DIFSP).

```java
import io.github.cyfko.inputspec.FormSpec;

@FormSpec(
    id = "user-profile",
    displayName = "User Profile",
    description = "Update your account details.",
    submitUri = "/api/forms/user-profile/submit",
    method = "PUT"
)
public class UserProfileForm {
    // fields...
}
```

### Key properties

*   **`id`** (Required): A unique string identifying this form system-wide. This becomes the filename (`user-profile.json`) and the ID used by handlers.
*   **`displayName`**: The human-readable title of the form, typically rendered as an `<h1>` or card title in the UI.
*   **`description`**: A subtitle or instructional text guiding the user.
*   **`submitUri`**: The endpoint where the frontend or AI agent should send the validated JSON payload.
*   **`method`** (Optional, default `POST`): The HTTP method to use for submission (`POST`, `PUT`, `PATCH`).

## The `@FieldMeta` Annotation

While standard validation annotations (`@NotNull`, `@Min`) define *how* a field behaves, `@FieldMeta` defines *what* the field is and how it should be presented to the user.

```java
import io.github.cyfko.inputspec.FieldMeta;
import jakarta.validation.constraints.NotBlank;

public class UserProfileForm {

    @NotBlank
    @FieldMeta(
        displayName = "First Name",
        description = "Your given name as it appears on your ID.",
        formatHint = "text"
    )
    private String firstName;
    
    @FieldMeta(
        displayName = "Date of Birth",
        formatHint = "date"
    )
    private java.time.LocalDate dateOfBirth;
}
```

### Key properties

*   **`displayName`**: The label shown next to the input field in the UI.
*   **`description`**: Help text, a tooltip, or placeholder guidance.
*   **`formatHint`**: A purely presentational hint for the frontend (e.g., `text`, `date`, `password`, `email`). Note that actual data type inference is driven by the Java variable type (e.g., `String`, `Integer`, `LocalDate`).
*   **`valuesSource`**: (Advanced) Used to attach pick-lists, dropdowns, or autocomplete endpoints to the field (see [Values Sources](./values-sources)).

### Type Inference

InputSpec automatically infers the underlying primitive data type from your Java field declarations:

*   `String` -> `STRING`
*   `int`, `Integer`, `long`, `Long` -> `INTEGER`
*   `double`, `Double`, `float`, `Float`, `BigDecimal` -> `DECIMAL`
*   `boolean`, `Boolean` -> `BOOLEAN`
*   `LocalDate`, `OffsetDateTime` -> `DATE` or `DATETIME`

You never have to manually specify `type="STRING"` in the annotations; the compiler handles the reflection for you.
