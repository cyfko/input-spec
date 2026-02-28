# InputSpec MCP Demo — Hotel Booking

A complete demo showing how an **AI agent** can autonomously discover, read, validate, and submit forms using InputSpec + MCP (Model Context Protocol).

## Quick Start

### 1. Install InputSpec modules (from the project root)

```bash
# From: input-spec/servers/java
mvn clean install -DskipTests

# From: input-spec/servers/java-processor
mvn clean install -DskipTests

# From: input-spec/servers/java-spring-boot-starter
mvn clean install -DskipTests
```

### 2. Run the demo

```bash
cd examples/spring-boot-mcp-demo
mvn spring-boot:run
```

### 3. Test the REST API

```bash
# List available forms
curl http://localhost:8080/api/forms

# Get the booking form spec
curl http://localhost:8080/api/forms/hotel-booking

# Validate form data
curl -X POST http://localhost:8080/api/forms/hotel-booking/validate \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "John Doe",
    "email": "john@example.com",
    "checkIn": "2026-04-01",
    "checkOut": "2026-04-05",
    "roomType": "DELUXE",
    "guests": 2
  }'

# Submit a booking
curl -X POST http://localhost:8080/api/forms/hotel-booking/submit \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "John Doe",
    "email": "john@example.com",
    "checkIn": "2026-04-01",
    "checkOut": "2026-04-05",
    "roomType": "DELUXE",
    "guests": 2
  }'
```

### 4. Test with an AI agent (MCP)

Connect any MCP-compatible AI client (Claude Desktop, etc.) to `http://localhost:8080/mcp`.

The agent can then use 4 tools:

| Tool | Description |
|------|-------------|
| `inputspec_list_forms` | Lists available forms |
| `inputspec_get_form` | Gets the full form spec (fields, constraints, values) |
| `inputspec_validate_form` | Pre-validates data |
| `inputspec_submit_form` | Validates + submits to the `@FormHandler` |

**Example AI conversation:**
> "Book me a deluxe room at the hotel from April 1 to April 5 for 2 guests. My name is John Doe and my email is john@example.com."

The AI agent will:
1. Call `inputspec_list_forms` → discover "hotel-booking"
2. Call `inputspec_get_form("hotel-booking")` → understand the fields and constraints
3. Call `inputspec_validate_form("hotel-booking", {...})` → check the data
4. Call `inputspec_submit_form("hotel-booking", {...})` → submit and receive confirmation

## What's in the demo

| File | Purpose |
|------|---------|
| `BookingForm.java` | `@FormSpec` with 7 fields, Jakarta constraints, INLINE values, cross-constraint |
| `BookingHandler.java` | `@FormHandler` with domain logic (rejects SUITE > 4 guests) |
| `application.yml` | MCP enabled (`inputspec.mcp.enabled: true`) |

## Architecture

```
   @FormSpec("hotel-booking")         Annotation Processor        META-INF/difsp/hotel-booking.json
   BookingForm.java           ──→     (compile-time)        ──→   + i18n skeleton

   @FormHandler("hotel-booking")      FormHandlerRegistry         REST + MCP endpoints
   BookingHandler.java        ──→     (startup scan)        ──→   /api/forms/** + MCP tools
```
