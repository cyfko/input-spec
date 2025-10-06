<div align="center">

# input-spec (TypeScript SDK)

Declarative, backend-owned field & validation specifications for frontends.

<strong>FR:</strong> Définissez les champs et règles côté serveur et consommez-les côté client sans duplication.

Zero runtime dependencies • Fully typed • Framework agnostic

[Docs](https://cyfko.github.io/input-spec/typescript/) · [API](https://cyfko.github.io/input-spec/typescript/API) · [Intégration](https://cyfko.github.io/input-spec/typescript/FRAMEWORK_INTEGRATION) · [Architecture](https://cyfko.github.io/input-spec/typescript/ARCHITECTURE)

</div>

---

## 1. Install
```bash
npm install input-spec
```
Node >= 16. No runtime deps.

---

## 2. Frontend Quick Start
```typescript
import { FieldValidator, InputFieldSpec } from 'input-spec';

const spec: InputFieldSpec = await fetch('/api/form-fields/email').then(r => r.json());
const validator = new FieldValidator();
const result = await validator.validate(spec, 'user@example.com', 'email');
if (!result.isValid) console.log(result.errors.map(e => e.message));
```

## 3. Backend Generation
```typescript
import { InputFieldSpec, ConstraintDescriptor } from 'input-spec';

function buildEmail(tier: 'basic' | 'premium'): InputFieldSpec {
  const constraints: ConstraintDescriptor[] = [
    { name: 'email', pattern: '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$', errorMessage: 'Invalid email' }
  ];
  constraints.push({
    name: 'maxLength',
    max: tier === 'premium' ? 200 : 50,
    errorMessage: `Email too long (max ${tier === 'premium' ? 200 : 50} chars)`
  });
  return {
    displayName: 'Email Address',
    dataType: 'STRING',
    expectMultipleValues: false,
    required: true,
    constraints
  };
}
```

---

## 4. Dynamic Values (Autocomplete)
```typescript
import { ValuesResolver, FetchHttpClient, MemoryCacheProvider, createDefaultValuesEndpoint } from 'input-spec';
const resolver = new ValuesResolver(new FetchHttpClient(), new MemoryCacheProvider());
const endpoint = createDefaultValuesEndpoint('https://api.example.com/countries');
const result = await resolver.resolveValues(endpoint, { search: 'fr', page: 1, limit: 10 });
console.log(result.values);
```

---

## 5. Core Concepts
| Name | Description |
|------|-------------|
| InputFieldSpec | Full declarative field specification |
| ConstraintDescriptor | Ordered validation rule descriptor |
| ValuesEndpoint | Dynamic values contract (search + pagination) |
| FieldValidator | Executes validation (single constraint or all) |
| ValuesResolver | Fetch + cache orchestration for dynamic values |
| MemoryCacheProvider | In‑memory TTL cache |

---

## 6. API Snapshot
```typescript
interface InputFieldSpec {
  displayName: string;
  description?: string;
  dataType: 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN';
  expectMultipleValues: boolean;
  required: boolean;
  constraints: ConstraintDescriptor[];
  valuesEndpoint?: ValuesEndpoint;
}
```
Full reference: `./docs/API.md`.

Migration: A helper `migrateV1Spec` converts legacy (enumValues + composite min/max/pattern) to v2 atomic form; review output (see Migration section in API docs).

Coercion (optional library-only): Disabled by default; enable via `new FieldValidator({ coercion: { coerce: true } })` or per-field `coercion` block to accept numeric / boolean strings or epoch dates. Protocol wire format remains unchanged.

---

## 7. Example Patterns
Validate specific constraint:
```typescript
await validator.validate(spec, 'bad@', 'email');
```
Validate all:
```typescript
await validator.validate(spec, 'good@example.com');
```
Array field:
```typescript
await validator.validate(arraySpec, ['a','b']);
```

---

## 8. Design Principles
1. Backend is source of truth
2. Ordered constraint execution
3. Zero runtime dependencies
4. Extensible via injected HTTP/cache
5. Serializable specs for testing

---

## 9. Project Layout
```
src/
  types/        # Interfaces & type guards
  validation/   # Validation engine
  client/       # HTTP + cache + resolver
  __tests__/    # Jest tests
```

---

## 10. Scripts
| Task | Command |
|------|---------|
| Build | `npm run build` |
| Test  | `npm test` |
| Lint  | `npm run lint` |
| Types | `npm run type-check` |

---

## 11. Publishing (Maintainers)
```bash
npm run build && npm test
npm publish --dry-run
npm publish --access public
```

---

## 12. Contributing
1. Fork & branch
2. Add tests
3. Ensure green build
4. Open PR

---

## 13. License
MIT (see `LICENSE`).

---

## 14. Integrity
| Item | Value |
|------|-------|
| Package | input-spec |
| Protocol constant | PROTOCOL_VERSION |
| Library constant | LIBRARY_VERSION |
| Runtime deps | 0 |

---

## 15. Resources
- Protocol Specification: `../../PROTOCOL_SPECIFICATION.md`
- Root README: `../../README.md`
- Docs: `./docs/`

---


## 🌐 Real-World Scenarios

### Scenario 1: Multi-Tenant SaaS Application

**Frontend Team**: Different validation rules per client, all handled automatically!

```typescript
---

// Your component automatically adapts to each client's rules
const loadUserForm = async (clientId: string) => {
  const fields = await fetch(`/api/clients/${clientId}/form-fields/user`)
    .then(r => r.json());
  
  // Client A might require 2FA, Client B might not
  // Client A might have different email domains allowed
  // Your frontend doesn't care - it just renders what backend sends!
  return fields;
};
```

**Backend Team**: Complete control over client-specific business rules

```typescript
// Your API dynamically generates rules based on client configuration
app.get('/api/clients/:clientId/form-fields/user', async (req, res) => {
  const client = await getClientConfig(req.params.clientId);
  
  const emailField: InputFieldSpec = {
    displayName: "Email Address",
    dataType: "STRING",
    required: true,
    constraints: [
      { name: "email", pattern: "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", errorMessage: "Invalid email format" }
    ]
  };

  // Client-specific email domain restrictions
  if (client.restrictEmailDomains) {
    emailField.constraints.push({
      name: "allowedDomains",
      pattern: `^[^@]+@(${client.allowedDomains.join('|')})$`,
      errorMessage: `Email must be from: ${client.allowedDomains.join(', ')}`
    });
  }

  res.json({ fields: [emailField] });
});
```

### Scenario 2: Smart Product Search with Live Data

**Frontend Team**: Rich autocomplete without hardcoding product lists

```typescript
// Product search that adapts to inventory and user permissions
const productField = await fetch(`/api/form-fields/product?userRole=${userRole}`)
  .then(r => r.json());

// Backend controls:
// - Which products user can see
// - Search parameters  
// - Pagination settings
// - Validation rules

const searchProducts = async (query: string) => {
  if (productField.valuesEndpoint) {
    const response = await fetch(
      `${productField.valuesEndpoint.url}?${productField.valuesEndpoint.searchParam}=${query}`
    );
    return response.json();
  }
};
```

**Backend Team**: Dynamic product visibility and search logic

```typescript
app.get('/api/form-fields/product', async (req, res) => {
  const userRole = req.query.userRole;
  
  let searchEndpoint = '/api/products/search';
  let constraints: ConstraintDescriptor[] = [
    { name: "required", errorMessage: "Please select a product" }
  ];

  // Admin users can see all products including discontinued
  if (userRole === 'admin') {
    searchEndpoint += '?includeDiscontinued=true';
  }
  
  // Sales users have minimum quantity requirements
  if (userRole === 'sales') {
    constraints.push({
      name: "minQuantity",
      min: 10,
      errorMessage: "Sales orders minimum 10 units"
    });
  }

  const productFieldSpec: InputFieldSpec = {
    displayName: "Product",
    dataType: "STRING",
    required: true,
    constraints,
    valuesEndpoint: {
      url: searchEndpoint,
      searchParam: "q",
      pageParam: "page",
      minSearchLength: 2
    }
  };

  res.json(productFieldSpec);
});
```

### Scenario 3: Dynamic Form Generation

**Frontend Team**: Build entire forms from backend configuration

```typescript
// Generate complete registration form from backend
const buildRegistrationForm = async (country: string, userType: string) => {
  const formConfig = await fetch(`/api/forms/registration?country=${country}&userType=${userType}`)
    .then(r => r.json());
  
  // Backend controls:
  // - Which fields are required per country
  // - Validation rules (phone formats, postal codes)
  // - Field order and grouping
  // - Conditional field visibility
  
  const formElements = formConfig.fields.map(fieldSpec => 
    createFormField(fieldSpec) // Your UI component factory
  );
  
  return formElements;
};
```

**Backend Team**: Country and role-specific form logic

```typescript
app.get('/api/forms/registration', async (req, res) => {
  const { country, userType } = req.query;
  const countryConfig = await getCountryConfig(country);
  
  const fields: InputFieldSpec[] = [
    // Email - universal
    {
      displayName: "Email",
      dataType: "STRING",
      required: true,
      constraints: [
        { name: "email", pattern: "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", errorMessage: "Invalid email format" },
        { name: "maxLength", max: 100, errorMessage: "Email too long (max 100 chars)" }
      ]
    }
  ];

  // Phone field with country-specific validation
  fields.push({
    displayName: "Phone Number",
    dataType: "STRING", 
    required: countryConfig.phoneRequired,
    constraints: [
      {
        name: "phoneFormat",
        pattern: countryConfig.phonePattern,
        errorMessage: `Invalid phone format for ${country}`
      }
    ]
  });

  // Business users get additional fields
  if (userType === 'business') {
    fields.push({
      displayName: "Tax ID",
      dataType: "STRING",
      required: true,
      constraints: [
        {
          name: "taxIdFormat", 
          pattern: countryConfig.taxIdPattern,
          errorMessage: `Invalid tax ID format for ${country}`
        }
      ]
    });
  }

  res.json({ fields });
});
```

---

## 🎯 Key Features

### ✨ **Zero Dependencies**
- Pure TypeScript implementation
- No external runtime dependencies
- Works in browser and Node.js
- Small bundle size (34.7 KB)

### 🔧 **Enhanced API Design**
- **Required field at top-level**: `required: boolean` moved from constraints for better ergonomics
- **Ordered constraint execution**: Constraints execute in array order for predictable behavior
- **Named constraints**: Each constraint has a `name` property for better identification and debugging

### � **Framework Integration**
- **Angular HttpClient**: Seamless integration with interceptors and dependency injection
- **Axios Support**: Custom Axios instances with existing configurations preserved
- **Custom HTTP Clients**: Configurable fetch-based client with interceptors and error handling
- **Zero Breaking Changes**: Existing HTTP infrastructure remains intact


---

## 📦 Feature Summary

- **Zero Runtime Dependencies**: Pure TypeScript implementation
- **Framework Integration**: Angular, React, Vue, Vanilla JS support
- **HTTP Client Injection**: Preserves existing interceptors and configurations
- **Type Safety**: Complete TypeScript type definitions  
- **Validation Engine**: Comprehensive field validation
- **HTTP Client**: Pluggable HTTP client with caching
- **Dependency Injection**: Clean architecture with IoC
- **Extensive Testing**: 58 tests with 96%+ coverage

---

## 🏗️ Architecture

### Separation of Concerns

```
src/
├── types/          # Pure TypeScript interfaces (zero dependencies)
├── validation/     # Business logic validation engine  
├── client/         # Infrastructure (HTTP, caching, resolution)
└── __tests__/      # Comprehensive test suite
```

### Design Patterns

- **Dependency Injection**: Constructor injection with interfaces
- **Strategy Pattern**: Pluggable HTTP clients and cache providers
- **Factory Pattern**: Simplified object creation
- **Template Method**: Validation algorithms

---



### Dynamic Values Resolution

```typescript
import { 
  ValuesResolver, 
  FetchHttpClient, 
  MemoryCacheProvider,
  createDefaultValuesEndpoint 
} from './src';

// Setup with dependency injection
const resolver = new ValuesResolver(
  new FetchHttpClient(),
  new MemoryCacheProvider()
);

// Configure endpoint
const endpoint = createDefaultValuesEndpoint('https://api.example.com/countries');

// Resolve values with caching and pagination
const result = await resolver.resolveValues(endpoint, { 
  search: 'france',
  page: 1,
  limit: 10 
});
```

### Zero-Dependency Architecture

```typescript
// No external dependencies at runtime!
import { MemoryCacheProvider, FetchHttpClient } from './src';

const cache = new MemoryCacheProvider();    // Uses native Map
const client = new FetchHttpClient();       // Uses native fetch
```

---

## 🧪 Testing

### Run Tests

```bash
# All tests
npm test

# Watch mode
npm run test:watch

# Coverage report
npm run test:coverage
```

### Test Results

- **58 tests** pass with **100% success rate**
- **Types Module**: 100% coverage
- **Validation Module**: 96% coverage  
- **Client Module**: 86% coverage
- **Integration Tests**: End-to-end scenarios with new v2.0 structure

---

## 🔨 Build & Development

### Available Scripts

```bash
npm run build         # Build distribution files (CJS, ESM, types)
npm run dev           # Build in watch mode
npm run test          # Run test suite
npm run lint          # ESLint checking
npm run format        # Prettier formatting
npm run type-check    # TypeScript type checking
```

### Build Output

```
dist/
├── index.js          # CommonJS build
├── index.mjs         # ES Module build
├── index.d.ts        # TypeScript declarations (CJS)
└── index.d.mts       # TypeScript declarations (ESM)
```

---

## 📋 API Reference (Snapshot)

### Core Types

- `InputFieldSpec` - Complete field specification
- `ConstraintDescriptor` - Validation rules
- `ValidationResult` - Validation outcome
- `ValuesEndpoint` - Dynamic values configuration

### Classes

- `FieldValidator` - Main validation engine
- `ValuesResolver` - Value resolution orchestrator
- `FetchHttpClient` - HTTP client implementation
- `MemoryCacheProvider` - In-memory caching

### Interfaces

- `HttpClient` - HTTP client abstraction
- `CacheProvider` - Cache provider abstraction

---

## 🎯 Design Principles

### Zero Dependencies
- **Runtime**: No external dependencies
- **Build**: Only development dependencies (TypeScript, Jest, etc.)
- **Browser**: Uses native fetch, Map, etc.

### Type Safety
- **Compile-time**: Full TypeScript strict mode
- **Runtime**: Type guards for external data
- **API**: Strongly typed interfaces

### Testability
- **Dependency Injection**: Easy mocking and testing
- **Interface Segregation**: Focused, testable interfaces
- **Pure Functions**: Predictable, testable logic

---

## 📚 Documentation

- [Usage Guide](./docs/USAGE_GUIDE.md) - Getting started and common patterns
- [API Reference](./docs/API.md) - Complete API documentation with examples
- [Framework Integration](./docs/FRAMEWORK_INTEGRATION.md) - Angular, React, Vue integration examples
- [Performance Guide](./docs/PERFORMANCE.md) - Optimization strategies and benchmarks
- [Architecture Guide](./docs/ARCHITECTURE.md) - Design decisions and patterns
- [Migration Guide](./MIGRATION.md) - Migrating from v1.x to v2.0
- [Release Notes](./RELEASE_NOTES.md) - What's new in v2.0

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Add tests for your changes
4. Ensure all tests pass (`npm test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

---

## 📄 License

MIT License - see LICENSE file for details.

---

## 🔗 Related

---

## 📦 Publishing (Maintainers)

```bash
# Build & test
npm run build && npm test

# Dry run publish
npm publish --dry-run

# Publish (ensure version not already published)
npm publish --access public
```

---

## ✅ Integrity Notes

- Package name: `input-spec`
- Protocol version constant exported as `PROTOCOL_VERSION`
- Library version exported as `LIBRARY_VERSION`
- No runtime dependencies; only dev tooling (TypeScript, Jest, tsup)

---

If something is unclear or you need a French version complète du README, ouvrez une issue. 🙌

- [Protocol Specification](../../PROTOCOL_SPECIFICATION.md)
- [Project Root](../../README.md)