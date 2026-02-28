<div align="center">
  <img src="https://raw.githubusercontent.com/cyfko/input-spec/main/website/static/img/logo.png" alt="InputSpec Logo" width="120" />
</div>

# InputSpec (Dynamic Input Field Specification Protocol)

[![Docs Site](https://img.shields.io/badge/docs-online-blueviolet)](https://cyfko.github.io/input-spec/)
[![License](https://img.shields.io/badge/license-MIT-green)](./LICENSE)

**InputSpec** is an open standard and ecosystem (DIFSP v2.1+) designed to solve the "Validation Drift" problem between frontend UIs, backend services, and AI Agents. 

It allows you to **define your forms, types, and validation rules exactly once** in your backend code, and automatically generate a rich, framework-agnostic JSON protocol that any client can understand and render.

---

## 🛑 The Problem

Modern applications suffer from duplicated form logic:
1. **The Backend** defines strict validation rules, data types, and database constraints.
2. **The Frontend** (React, Vue, iOS) is forced to manually duplicate those rules (Regexes, min/max lengths, required markers) to provide a good user experience.
3. **AI Agents** (via frameworks like MCP) are handed generic OpenAPI schemas, forcing them into a trial-and-error loop of guessing the correct payload formatting and failing backend validations.

When these layers drift out of sync, the UI lies to the user, the AI hallucinates bad requests, and the backend is forced to reject payloads.

## 💡 The Solution

InputSpec treats the backend as the single source of truth.

By using your language's native validation annotations (e.g., Jakarta Validation in Java), InputSpec generates a structured JSON representation of your form called the **Dynamic Input Field Specification Protocol (DIFSP)**.

This JSON file contains everything a client needs:
- `DataType` (String, Number, Date, Boolean)
- Required flags and multiplicity
- Granular constraints (`pattern`, `maxValue`, `crossConstraints`)
- Value sources (Static enums or REST endpoints for dropdowns)

Clients (Web, Native, or AI) consume this JSON to dynamically render the UI, enforce validation locally, and guarantee a 100% success rate when submitting the payload back to the server.

---

## 🤖 AI & MCP Native

Because InputSpec codifies complex business rules into standard machine-readable JSON constraints, it is the **perfect companion for the Model Context Protocol (MCP)**.

When exposed to an AI model (like Claude or ChatGPT), the agent reads the InputSpec JSON _before_ attempting to call your tool. It sees precisely what RegExp is required, which values are accepted from a dropdown menu, and what dates are invalid. This eliminates validation hallucinations and allows Large Language Models to confidently interact with your application's mutable forms.

---

## 📦 Implementations

InputSpec is designed to be language-agnostic at the protocol layer. Current reference implementations include:

### ☕ Java Ecosystem

The Java implementation leans on Jakarta Bean Validation (`@NotNull`, `@Size`, etc.) and an Annotation Processor to completely automate your form schema generation.

*   **[`servers/java`](./servers/java/)**: The core engine and generic `FormSpecValidator`.
*   **[`servers/java-processor`](./servers/java-processor/)**: The compile-time annotation processor that generates DIFSP JSON schemas from `@FormSpec` classes.
*   **[`servers/java-spring-boot-starter`](./servers/java-spring-boot-starter/)**: Auto-configures InputSpec, serves your generated forms, and natively bridges `@FormHandler` methods into Spring AI MCP Tool callbacks.

---

## 📚 Documentation

The complete documentation, including guides, cross-constraint examples, and the full Protocol Specification, is available on our **[Docusaurus Site](https://cyfko.github.io/input-spec/)**.

- [English Documentation](https://cyfko.github.io/input-spec/)
- [Documentation en Français](https://cyfko.github.io/input-spec/fr/)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.