# Dynamic Input Field Specification - TypeScript Implementation v1.0

A zero-dependency TypeScript implementation of the Dynamic Input Field Specification Protocol v1.0 with ordered constraint execution and enhanced API ergonomics.

## 🚀 Quick Start

### Installation
```bash
npm install
npm run build
```

### 📖 Learning Path

1. **🔍 Basic Concepts** - Start with [docs/USAGE_GUIDE.md](./docs/USAGE_GUIDE.md)
2. **🏗️ Architecture** - Understand the design with [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)  
3. **💻 Live Examples** - Try the interactive examples:
   ```bash
   npm run examples:basic      # Basic validation
   npm run examples:dynamic    # Dynamic values  
   npm run examples:complete   # Complete forms
   npm run examples:demo       # Full demonstration
   ```

### 30-Second Example

```typescript
import { FieldValidator, InputFieldSpec } from './src';

// Define field specification with new v2.0 structure
const emailField: InputFieldSpec = {
  displayName: 'Email',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,  // ✨ Now at top-level for better API design
  constraints: [   // ✨ Now an array for ordered execution
    {
      name: 'email',
      pattern: '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$',
      errorMessage: 'Please enter a valid email address'
    }
  ]
};

// Validate input
const validator = new FieldValidator();
const result = await validator.validate(emailField, 'user@example.com', 'email');
console.log(result.isValid); // true
```

## ✨ What's New in v2.0

### 🎯 **Enhanced API Design**
- **Required field at top-level**: `required: boolean` moved from constraints to `InputFieldSpec`
- **Ordered constraint execution**: Constraints now execute in array order for predictable behavior
- **Named constraints**: Each constraint has a `name` property for better identification

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