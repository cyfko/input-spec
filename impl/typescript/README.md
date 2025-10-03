# Stop Hardcoding Forms! TypeScript Implementation

**Let your backend define form fields and validation rules. Your frontend just renders them.**

## The Two Sides of Dynamic Forms

### 🎯 Frontend Developer? You Consume Field Specs
Your backend sends you complete field definitions. No more hardcoded validation!

```typescript
// Instead of hardcoding validation rules...
const emailValidation = { required: true, pattern: /email-regex/ }; // 😤

// Your backend sends you the complete field specification!
const emailFieldSpec = await fetch('/api/form-fields/email').then(r => r.json());
// Returns: { displayName: "Email", required: true, constraints: [...] }
```

### ⚙️ Backend Developer? You Generate Field Specs  
You control all validation logic and form behavior from your API.

```typescript
// In your API endpoint
app.get('/api/form-fields/email', (req, res) => {
  const emailFieldSpec: InputFieldSpec = {
    displayName: "Email Address",
    dataType: "STRING", 
    required: true,
    constraints: [
      { name: "email", type: "email", message: "Please enter a valid email" },
      { name: "maxLength", type: "maxLength", value: 100 }
    ]
  };
  res.json(emailFieldSpec);
});
```

## 🚀 Installation & Quick Start

```bash
npm install input-field-spec-ts
```

### Frontend Usage: Consume Field Specs

```typescript
import { FieldValidator, InputFieldSpec } from 'input-field-spec-ts';

// 1. Get field definition from YOUR backend
const fieldSpec: InputFieldSpec = await fetch('/api/form-fields/email')
  .then(r => r.json());

// 2. Validate user input using backend rules  
const validator = new FieldValidator();
const result = validator.validate(userInput, fieldSpec);

// 3. Display validation errors from backend-defined messages
if (!result.isValid) {
  console.log(result.errors); // Backend-controlled error messages
}
```

### Backend Usage: Generate Field Specs

```typescript
import { InputFieldSpec, ConstraintDescriptor } from 'input-field-spec-ts';

// Generate field specs based on your business logic
function createEmailField(userTier: 'basic' | 'premium'): InputFieldSpec {
  const constraints: ConstraintDescriptor[] = [
    { name: "email", type: "email", message: "Please enter a valid email" }
  ];
  
  // Premium users get longer email addresses
  if (userTier === 'premium') {
    constraints.push({ 
      name: "maxLength", 
      type: "maxLength", 
      value: 200, 
      message: "Email too long (max 200 chars)" 
    });
  } else {
    constraints.push({ 
      name: "maxLength", 
      type: "maxLength", 
      value: 50, 
      message: "Email too long (max 50 chars)" 
    });
  }

  return {
    displayName: "Email Address",
    dataType: "STRING",
    required: true,
    constraints
  };
}

// In your API
app.get('/api/form-fields/email', (req, res) => {
  const userTier = req.user.tier; // From your auth system
  const fieldSpec = createEmailField(userTier);
});
```

## 🌐 Real-World Scenarios

### Scenario 1: Multi-Tenant SaaS Application

**Frontend Team**: Different validation rules per client, all handled automatically!

```typescript
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
      { name: "email", type: "email", message: "Invalid email format" }
    ]
  };

  // Client-specific email domain restrictions
  if (client.restrictEmailDomains) {
    emailField.constraints.push({
      name: "allowedDomains",
      type: "pattern", 
      value: `^[^@]+@(${client.allowedDomains.join('|')})$`,
      message: `Email must be from: ${client.allowedDomains.join(', ')}`
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
    { name: "required", type: "required", message: "Please select a product" }
  ];

  // Admin users can see all products including discontinued
  if (userRole === 'admin') {
    searchEndpoint += '?includeDiscontinued=true';
  }
  
  // Sales users have minimum quantity requirements
  if (userRole === 'sales') {
    constraints.push({
      name: "minQuantity",
      type: "min",
      value: 10,
      message: "Sales orders minimum 10 units"
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
        { name: "email", type: "email", message: "Invalid email format" },
        { name: "maxLength", type: "maxLength", value: 100 }
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
        type: "pattern",
        value: countryConfig.phonePattern,
        message: `Invalid phone format for ${country}`
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
          type: "pattern",
          value: countryConfig.taxIdPattern,
          message: `Invalid tax ID format for ${country}`
        }
      ]
    });
  }

  res.json({ fields });
});
```

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

### �🔄 **Migration from v1.x**
```typescript
// Before (v1.x)
const oldSpec = {
  constraints: {
    myConstraint: { required: true, pattern: "..." }
  }
};

// After (v2.0)  
const newSpec = {
  required: true,  // Moved to top-level
  constraints: [   // Now an array
    { name: 'myConstraint', pattern: "..." }
  ]
};

// Framework Integration (NEW!)
const httpClient = HttpClientFactory.createAngularAdapter(angularHttpClient);
const valuesResolver = new ValuesResolver(httpClient, cache);
```

## 📦 Features

- **Zero Runtime Dependencies**: Pure TypeScript implementation
- **Framework Integration**: Angular, React, Vue, Vanilla JS support
- **HTTP Client Injection**: Preserves existing interceptors and configurations
- **Type Safety**: Complete TypeScript type definitions  
- **Validation Engine**: Comprehensive field validation
- **HTTP Client**: Pluggable HTTP client with caching
- **Dependency Injection**: Clean architecture with IoC
- **Extensive Testing**: 58 tests with 96%+ coverage

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

## 🔧 Usage Examples

### Basic Validation

```typescript
import { FieldValidator, InputFieldSpec } from './src';

const fieldSpec: InputFieldSpec = {
  displayName: 'Email',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,  // ✨ Top-level required field
  constraints: [   // ✨ Array with ordered execution
    {
      name: 'email',
      pattern: '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$',
      errorMessage: 'Please enter a valid email address'
    }
  ]
};

const validator = new FieldValidator();
const result = await validator.validate(fieldSpec, 'test@example.com', 'email');
console.log(result.isValid); // true
```

### Multiple Constraints with Ordered Execution

```typescript
const passwordSpec: InputFieldSpec = {
  displayName: 'Password',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,
  constraints: [  // Executed in order: length → strength → special
    {
      name: 'length',
      min: 8,
      max: 50,
      errorMessage: 'Password must be 8-50 characters'
    },
    {
      name: 'strength',
      pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)',
      errorMessage: 'Must contain lowercase, uppercase, and digit'
    },
    {
      name: 'special',
      pattern: '.*[!@#$%^&*]',
      errorMessage: 'Must contain special character'
    }
  ]
};

// Validate specific constraint
const lengthResult = await validator.validate(passwordSpec, 'weak', 'length');

// Validate all constraints in order
const allResult = await validator.validate(passwordSpec, 'StrongPass123!');
```

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

## 📋 API Reference

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

## 📚 Documentation

- [Usage Guide](./docs/USAGE_GUIDE.md) - Getting started and common patterns
- [API Reference](./docs/API.md) - Complete API documentation with examples
- [Framework Integration](./docs/FRAMEWORK_INTEGRATION.md) - Angular, React, Vue integration examples
- [Performance Guide](./docs/PERFORMANCE.md) - Optimization strategies and benchmarks
- [Architecture Guide](./docs/ARCHITECTURE.md) - Design decisions and patterns
- [Migration Guide](./MIGRATION.md) - Migrating from v1.x to v2.0
- [Release Notes](./RELEASE_NOTES.md) - What's new in v2.0

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Add tests for your changes
4. Ensure all tests pass (`npm test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## 📄 License

MIT License - see LICENSE file for details.

## 🔗 Related

- [Protocol Specification](../../PROTOCOL_SPECIFICATION.md)
- [Project Root](../../README.md)