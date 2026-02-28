---
sidebar_position: 1
id: intro
title: Why InputSpec?
---

# Why InputSpec?

Welcome to **InputSpec**, the declarative, strongly-typed, and protocol-first approach to building, validating, and sharing forms between your backend, your frontend, and Artificial Intelligence agents.

## The Problem

In modern web development, forms are a notorious source of duplication and tight coupling. Consider a standard User Registration form. You typically have to:

1. **Write the UI:** Build the HTML/React/Vue form, manually wiring up labels, placeholders, and error messages.
2. **Write the Frontend Validation:** Write JavaScript logic ensuring the email is valid and the password is long enough.
3. **Write the Backend POJO/DTO:** Create a Java class representing the payload.
4. **Write the Backend Validation:** Add Jakarta annotations (`@NotNull`, `@Email`) to recreate the exact same rules you just wrote in JS.
5. **Write the API Documentation:** Document the endpoints and expected payloads so the frontend team (or external APIs) knows what to send.

If a product manager asks to change the minimum password length from 8 to 12 characters, you have to find and update this rule in up to **five different places**. This inevitably leads to validation drift, where the frontend accepts something the backend rejects, creating frustrating user experiences.

## The InputSpec Solution

**Code Once, Generate Everywhere.**

InputSpec flips this paradigm. It treats your strongly-typed backend domain model as the **single source of truth**. 

By simply annotating your Java POJOs (Plain Old Java Objects) with standard Jakarta validation constraints and a few InputSpec decorators, an annotation processor automatically generates a rich, technology-agnostic JSON specification at compile time.

### How it works

1. **Declare:** You write your Java class and annotate fields with standard `@NotNull`, `@Min`, `@Email`.
2. **Compile:** The InputSpec Annotation Processor reads your classes and generates a `[form-id].json` file complying with the Dynamic Input Field Specification Protocol (DIFSP).
3. **Consume:** Your frontend (React, Vue, Flutter) dramatically dynamically renders the form reading the JSON. No more hardcoding inputs.
4. **Submit:** The Spring Boot starter automatically binds the submission back to your POJO and validates it before it ever hits your business logic.

## AI-Ready (Model Context Protocol)

Forms are the primary way users interact with software. But what about AI agents?

InputSpec natively integrates with the **Model Context Protocol (MCP)**. Because your forms are fully described in a structured JSON protocol, AI agents (like Claude or Custom GPTs) can **autonomously discover, understand, and fill out your application's forms** on behalf of the user, without any extra prompt engineering.

Ready to see it in action? Head over to the [Hello World](./getting-started/hello-world) guide.
