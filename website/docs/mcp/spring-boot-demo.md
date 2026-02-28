---
sidebar_position: 2
id: spring-boot-demo
title: Spring Boot Demo
---

# MCP Spring Boot Demo

Want to see InputSpec power an AI Agent? We have built a complete, ready-to-run demo in the `/examples/spring-boot-mcp-demo` directory.

The demo simulates a **Hotel Booking System**. It leverages InputSpec to define the booking form, and Spring AI's MCP Server to expose it to external agents.

## 1. The Form

The `BookingForm` is a simple POJO enriched with standard Jakarta validation (for basic length checking), Java Enums (for inline select values), and an InputSpec `@CrossConstraint` ensuring the dates are logical.

```java
@FormSpec(
    id = "hotel-booking",
    displayName = "Hotel Booking",
    submitUri = "/api/forms/hotel-booking/submit"
)
@CrossConstraint(
    name = "checkOutAfterCheckIn",
    type = CrossConstraintType.FIELD_COMPARISON,
    operator = ComparisonOperator.GT,
    fields = {"checkOut", "checkIn"},
    errorMessage = "Check-out must be after check-in."
)
public class BookingForm {
    // fields...
}
```

## 2. The Handler

The business logic simply accepts the strongly-typed `BookingForm`.

```java
@Service
public class BookingHandler {

    @FormHandler("hotel-booking")
    public SubmitResponse reserve(BookingForm form) {
        String bookingId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return SubmitResponse.ok(Map.of("status", "CONFIRMED", "bookingId", bookingId));
    }
}
```

## 3. Running the Demo

1. Clone the repository.
2. Navigate to the demo directory: `cd examples/spring-boot-mcp-demo`
3. Start the application: `mvn spring-boot:run`

The Spring Boot application will start on `http://localhost:8080`.

Because `spring-ai-starter-mcp-server-webmvc` is included, the server automatically exposes the standard MCP SSE (Server-Sent Events) endpoint at `/mcp/message`.

## 4. Connect Claude Desktop

You can now configure your AI agent to consume this application! For example, if you use the Claude Desktop app, you can add this to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "hotel-booking-service": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/inspector",
        "http://localhost:8080/mcp/message"
      ]
    }
  }
}
```

Restart Claude, and simply type:

> "I want to book a Deluxe room for Alice Smith starting April 1st, 2026 for 4 nights."

Claude will:
1. Discover the `inputspec_submit_hotel_booking` tool.
2. Read the JSON requirements (understanding `DELUXE` is an enum value, assessing constraints).
3. Call the tool automatically.
4. Respond back to you with the `bookingId`!
