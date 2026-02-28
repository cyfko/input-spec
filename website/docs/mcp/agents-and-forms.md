---
sidebar_position: 1
id: agents-and-forms
title: AI Agents & Forms
---

# Why MCP needs InputSpec

The **Model Context Protocol (MCP)** is an open standard introduced by Anthropic that allows AI models to connect securely to local or remote data sources and tools.

While exposing read-only data (like a database query) to an AI is relatively straightforward, allowing an AI agent to **mutably interact with an application**—like booking a hotel room, ordering a pizza, or creating a JIRA ticket—is notoriously complex.

## The Problem with AI "Tools"

When you expose a function or a "Tool" to an LLM, you are asking the LLM to generate a JSON payload that matches your function's schema.

However, business logic is rarely as simple as a flat schema.
- "Check-out date must be after check-in date."
- "If paying by credit card, the CVV is required."
- "Room type must be STANDARD, DELUXE, or SUITE."

If you only give the LLM a generic OpenAPI schema, it will repeatedly guess the payload, submit it, get a `400 Bad Request` regarding a validation error it doesn't understand, and try again until it hallucinates or loops infinitely.

## The InputSpec Solution

InputSpec was designed from the ground up to solve this exact problem. By separating the structural representation of a form (the JSON `DIFSP` payload) from the backend logic, we created the perfect language for LLMs.

Because InputSpec JSON explicitly codifies business constraints into standardized, machine-readable rules:

1. **Self-Correction**: The LLM reads the constraints (`"min": 18`, `"FUTURE"`) *before* it generates the payload.
2. **Deterministic Validation**: The LLM knows exactly what the application expects. It doesn't need to guess if the phone number needs international formatting; the `"regex"` constraint tells it.
3. **Values Sources**: If a field is a dropdown (`"REST"` values source), the AI agent knows to first fetch the available options before attempting to fill out the form, completely eliminating "Invalid Option" errors.

## The Spring Boot Starter

To bridge the gap between Java and MCP, we built `input-spec-spring-boot-starter`.

When this library is on your classpath, it:
1. Scans your application for `@FormHandler` methods.
2. Registers those input forms natively with the `spring-ai-mcp-server`.
3. Auto-generates the MCP Tool descriptions using your `@FieldMeta` descriptions.

Your backend forms are suddenly exposed as perfectly documented, highly constrained MCP Tools that Claude, ChatGPT, or any other agent can execute natively.

Check out the [Spring Boot Demo](./spring-boot-demo) to see this in action.
