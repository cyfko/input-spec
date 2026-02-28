---
sidebar_position: 2
id: validation
title: Validation & Constraints
---

# Validation & Constraints

One of the biggest pain points in building web applications is duplicating validation logic: writing it once in the HTML/JavaScript frontend, and writing it again in the Java backend.

InputSpec solves this by hijacking the standard **Jakarta Bean Validation** annotations (`@NotNull`, `@Size`, `@Min`, etc.) that you probably already use.

## How it works

When the InputSpec Annotation Processor runs, it inspects your fields. If it finds a Jakarta validation annotation, it translates that semantic rule into the declarative JSON protocol (the `DIFSP`).

### The Flow
1. You add `@Size(min = 5)` to a Java field.
2. InputSpec generates JSON containing `{"type": "MIN_LENGTH", "value": 5}`.
3. Your frontend library reads the JSON and enforces `minLength="5"` on the HTML input, preventing the user from typing invalid data.
4. When submitted, the Spring Boot backend runs the *exact same* `@Size` validation to ensure data integrity before saving.

## Supported Constraints

InputSpec automatically translates the following standard constraints:

### Nullability & Presence
*   `@NotNull`, `@NotEmpty`, `@NotBlank` ➡️ Sets `required: true` in the JSON spec.

### Numbers
*   `@Min(N)` / `@DecimalMin(N)` ➡️ `MIN(N)` constraint.
*   `@Max(N)` / `@DecimalMax(N)` ➡️ `MAX(N)` constraint.

### Strings & Collections
*   `@Size(min=A, max=B)` ➡️ `MIN_LENGTH(A)` and `MAX_LENGTH(B)` constraints.
*   `@Pattern(regexp="...")` ➡️ `PATTERN("...")` constraint. The frontend can use this regex directly for HTML5 pattern validation.
*   `@Email` ➡️ `EMAIL` constraint.

### Dates
*   `@Past` / `@PastOrPresent` ➡️ `PAST` constraint.
*   `@Future` / `@FutureOrPresent` ➡️ `FUTURE` constraint.

---

## Example

```java
public class RegistrationForm {

    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    @FieldMeta(displayName = "Username")
    private String username;

    @Min(18)
    @FieldMeta(displayName = "Age")
    private int age;

    @Future
    @FieldMeta(displayName = "Desired Move-in Date")
    private LocalDate moveInDate;
}
```

This Java code guarantees that the generated frontend form will restrict the `username` length, apply a regex pattern to block special characters, ensure the `age` number input has `min="18"`, and prevent selecting past dates in the `moveInDate` datepicker.

All from a single source of truth.

## Beyond Standard Jakarta

Sometimes, single-field validation isn't enough. What if "Check-out Date" must be strictly after "Check-in Date"? 

For those scenarios, InputSpec provides [Cross-Constraints](../advanced-guide/cross-constraints) and [Custom Validation Handlers](../advanced-guide/custom-handlers).
