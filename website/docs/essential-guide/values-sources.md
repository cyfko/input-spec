---
sidebar_position: 3
id: values-sources
title: Values Sources (Selects)
---

# Values Sources

Not all inputs are free-form text. Frequently, you want the user to select from a predefined list of options, such as choosing a Country, a Room Type, or a tagging category.

In InputSpec, we call these **Values Sources**. The protocol provides two ways to supply these options to the frontend: `INLINE` and `REST`.

## 1. INLINE (Static Lists)

If the list of options is small and rarely changes, you should embed them directly into the form definition using `INLINE`.

### The Auto-Magic Enum Approach (Recommended)

InputSpec v3 introduced automatic Enum mapping. If your field is entirely defined by a Java `Enum`, you just declare it! The compiler does the rest.

```java
public enum RoomType {
    STANDARD,
    DELUXE,
    SUITE
}

public class BookingForm {

    @NotNull
    @FieldMeta(
        displayName = "Room Type",
        description = "Select your preferred room category"
    )
    private RoomType roomType; // <--- The magic happens here
}
```

InputSpec automatically detects the Enum and embeds an `INLINE CLOSED` list into the JSON. It will even automatically format the labels (e.g., `DELUXE` becomes "Deluxe"). 

Because it's a closed list, the backend validator will stringently reject any submission attempting to pass "PENTHOUSE".

### The Explicit `@ValuesSource` Approach

If you don't want to use an Enum, or if you need to override the labels explicitly, you can configure it via the `@FieldMeta` annotation:

```java
import io.github.cyfko.inputspec.ValuesSource;
import io.github.cyfko.inputspec.Inline;

public class TaskForm {

    @FieldMeta(
        displayName = "Priority",
        valuesSource = @ValuesSource(
            protocol = "INLINE",
            mode = ValuesSource.ValuesMode.CLOSED,
            items = {
                @Inline(value = "P1", label = "Critical - Fix Immediately"),
                @Inline(value = "P2", label = "High - Next Sprint"),
                @Inline(value = "P3", label = "Low - Backlog")
            }
        )
    )
    private String priority;
}
```

## 2. REST (Dynamic Lists)

If your list of options is huge (e.g., all cities in the world), or if it changes constantly based on database state (e.g., Active Projects assigned to the user), embedding them inline is impossible.

You must fetch them dynamically using the `REST` protocol.

```java
public class AssignmentForm {

    @FieldMeta(
        displayName = "Assign to User",
        valuesSource = @ValuesSource(
            protocol = "REST",
            mode = ValuesSource.ValuesMode.CLOSED,
            uri = "/api/v1/users?active=true"
        )
    )
    private String assignedUserId;
}
```

### How REST Values Sources work:
1. When the frontend renders the form, it sees the `REST` protocol and the `uri`.
2. The frontend makes an HTTP `GET` request to `/api/v1/users?active=true`.
3. The frontend expects the response to be an array of objects containing `value` and `label` (e.g., `[{"value": "u123", "label": "Alice"}, ...]`).
4. The frontend populates the searchable dropdown with those results.

## Modes: CLOSED vs. SUGGESTIONS

Both `INLINE` and `REST` protocols require you to declare a `mode`:

*   **`CLOSED`**: The user *must* select exactly one of the provided options. The backend validator will actively enforce this by cross-checking the submitted value against the list of exact allowed values. (Like a strict `<select>` dropdown).
*   **`SUGGESTIONS`**: The list acts only as a helpful autocomplete. The user can select an option, *or* they can type their own free-form text. The backend validator will allow any text that matches the other standard constraints (like `@Size`). (Like an `<input datalist>`).
