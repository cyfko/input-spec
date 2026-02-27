# Input Spec Spring Boot Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cyfko/input-spec-spring-boot-starter)](https://central.sonatype.com/artifact/io.github.cyfko/input-spec-spring-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Spring Boot starter for automatic REST endpoint generation following the [Dynamic Input Field Specification Protocol v2.1](https://github.com/cyfko/input-spec).

## Features

- **Zero-config**: Auto-generates input-spec from JPA entities
- **Auto-detection**: Extracts metadata from Bean Validation, JPA annotations
- **Customizable**: Enrich with `@InputField` or provide custom `InputSpecProvider`
- **Type-safe**: Uses input-spec v2.1 core models
- **Spring Boot 3.x compatible**

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.github.cyfko</groupId>
    <artifactId>input-spec-spring-boot-starter</artifactId>
    <version>2.1.0</version>
</dependency>
```

### 2. Annotate Your Entity

```java
@Entity
@InputSpecEnabled
public class User {
    @Size(min = 3, max = 20)
    private String username;

    @Email
    @Column(nullable = false)
    private String email;

    @Enumerated
    private AccountStatus status;
}
```

### 3. Access the Endpoint

```bash
GET /users/input-spec
```

**Response:**
```json
{
  "protocolVersion": "2.1",
  "fields": [
    {
      "displayName": "Username",
      "dataType": "string",
      "required": false,
      "constraints": [
        {"name": "minLength", "type": "minLength", "params": 3},
        {"name": "maxLength", "type": "maxLength", "params": 20}
      ]
    },
    {
      "displayName": "Email",
      "dataType": "string",
      "required": true,
      "formatHint": "email",
      "constraints": [
        {"name": "email", "type": "pattern", "params": "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"}
      ]
    },
    {
      "displayName": "Status",
      "dataType": "string",
      "valuesEndpoint": {
        "protocol": "INLINE",
        "mode": "CLOSED",
        "items": [
          {"value": "ACTIVE", "label": "Active"},
          {"value": "INACTIVE", "label": "Inactive"},
          {"value": "SUSPENDED", "label": "Suspended"}
        ]
      }
    }
  ]
}
```

## Advanced Usage

### Enrich with @InputField

```java
@Entity
@InputSpecEnabled
public class Product {
    @InputField(
        displayName = "Product Name",
        description = "Official product title",
        formatHint = "capitalize"
    )
    @Size(min = 5, max = 200)
    private String name;

    @InputField(
        displayName = "Category",
        description = "Select product category",
        valuesEndpoint = @ValuesEndpointConfig(
            uri = "/api/categories",
            searchable = true,
            debounceMs = 300,
            minSearchLength = 2
        )
    )
    private String category;
}
```

### Custom Provider

```java
@Component
public class UserFormProvider implements InputSpecProvider<User> {

    @Override
    public Class<User> getEntityClass() {
        return User.class;
    }

    @Override
    public InputSpec provide() {
        return InputSpec.builder()
            .protocolVersion("2.1")
            .addField(InputFieldSpec.builder()
                .displayName("Advanced Username")
                .dataType(DataType.STRING)
                .required(true)
                .valuesEndpoint(ValuesEndpoint.builder()
                    .protocol(ValuesEndpoint.Protocol.HTTP)
                    .uri("/api/username/suggestions")
                    .searchParams(Map.of("fuzzy", true))
                    .searchParamsSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "fuzzy", Map.of("type", "boolean")
                        )
                    ))
                    .build())
                .build())
            .build();
    }
}
```

## Configuration

```yaml
input-spec:
  enabled: true
  auto-scan: true
  entity-scan-packages:
    - com.example.myapp.domain
  base-path: /api/v1
  endpoint-suffix: /input-spec
  enable-cors: true
  cors-allowed-origins:
    - https://example.com
```

## Auto-Detection Features

| Annotation | Detection |
|------------|-----------|
| `@NotNull`, `@NotBlank` | `required: true` |
| `@Column(nullable=false)` | `required: true` |
| `@Size(min, max)` | Min/Max length constraints |
| `@Min`, `@Max` | Min/Max value constraints |
| `@Pattern` | Regex pattern constraint |
| `@Email` | Email format + pattern |
| `@Enumerated` | INLINE ValuesEndpoint |
| `Collection<T>`, `T[]` | `expectMultipleValues: true` |

## License

MIT © [cyfko](https://github.com/cyfko)
