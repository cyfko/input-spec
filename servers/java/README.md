<div align="center">
  <img src="https://raw.githubusercontent.com/cyfko/input-spec/main/website/static/img/logo.png" alt="InputSpec Logo" width="120" />
</div>

# InputSpec (Java Core)

The reference implementation of the Dynamic Input Field Specification Protocol (DIFSP v2.1+) for the Java ecosystem.

This module provides the core validation engine (`FormSpecValidator`), the declarative annotations (`@FormSpec`, `@FieldMeta`), and the underlying protocol domain objects.

## 🚀 Features

*   **Deep Jakarta Bean Validation Integration**: Maps `@NotNull`, `@Size`, `@Email`, etc., directly to DIFSP JSON constraints.
*   **Compile-Time Form Generation**: When paired with the `input-spec-processor`, JSON schemas and `i18n` property skeletons are generated at build time. No runtime reflection overhead for schema building.
*   **Cross-Constraints**: Declaratively validate complex rules like `checkOut > checkIn` or `paymentMethod = CREDIT_CARD requires CVV`.
*   **AI / MCP Native**: Forms described via InputSpec can be intuitively consumed and completed by Large Language Models via the Model Context Protocol.

## 📦 Modules

The Java ecosystem is broken down into three specialized modules:

1. **`input-spec`** (This module): Core library, annotations, validator, and JSON schemas.
2. **[`input-spec-processor`](../java-processor/README.md)**: The Java Annotation Processor (`javac`) that reads `@FormSpec` models and generates DIFSP protocol artifacts during the compilation phase.
3. **[`input-spec-spring-boot-starter`](../java-spring-boot-starter/README.md)**: Spring Boot auto-configuration, `@FormHandler` reflection, and native Spring AI MCP Tool registration.

## 📖 Quick Example

```java
import io.github.cyfko.inputspec.FormSpec;
import io.github.cyfko.inputspec.FieldMeta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@FormSpec(
    id = "user-registration",
    displayName = "User Registration",
    submitUri = "/api/forms/register"
)
public class RegistrationForm {

    @NotNull
    @Size(min = 3, max = 20)
    @FieldMeta(displayName = "Username", description = "Enter a unique username")
    private String username;
    
    // ...
}
```

The `input-spec-processor` will generate a highly-constrained JSON representation of this POJO. Your frontend (or AI Agent) reads the JSON, and forces the user to provide a valid string between 3 and 20 characters before hitting the `submitUri`. 

When the payload reaches the server, `FormSpecValidator` guarantees the backend enforces the exact same rules.

## 🔗 Documentation

For comprehensive guides, installation instructions, and advanced examples, please visit the [official documentation](https://cyfko.github.io/input-spec/).