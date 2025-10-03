# Dynamic Input Field Specification Protocol# input-spec

A language-agnostic protocol to describe expected inputs and their constraints, enabling dynamic validation and discoverability across systems.

**A technology-agnostic protocol for defining input field constraints, value sources, and validation rules dynamically.**

## 🎯 Overview

The Dynamic Input Field Specification Protocol enables applications to:
- Define input field specifications at runtime
- Understand value constraints and sources without hardcoding
- Enable smart form fields with auto-completion and validation
- Support searchable, paginated value selection
- Maintain cross-language interoperability

## 📋 Protocol Specification

The complete protocol specification is available in [`PROTOCOL_SPECIFICATION.md`](./PROTOCOL_SPECIFICATION.md).

### Key Features
- **Technology-agnostic**: Works with any programming language or framework
- **Dynamic constraints**: Define validation rules at runtime
- **Value sources**: Support for remote data fetching with search and pagination
- **Type safety**: Strong typing for field specifications and constraints
- **Extensible**: Easy to add new constraint types and value sources

## 🚀 Implementations

### TypeScript Implementation (Production Ready)
- **Status**: ✅ **Published** - `input-field-spec-ts@1.0.0`
- **Location**: [`docs/typescript/`](https://cyfko.github.io/input-spec/typescript/)
- **Features**: 
  - Zero dependencies
  - Framework integration (Angular, React, Vue)
  - Comprehensive test suite (58 tests)
  - Complete TypeScript support

```bash
npm install input-field-spec-ts
```

**Quick Start:**
```typescript
import { InputFieldSpec, FieldValidator } from 'input-field-spec-ts';

const emailField: InputFieldSpec = {
  displayName: "Email Address",
  dataType: "STRING",
  expectMultipleValues: false,
  required: true,
  constraints: [
    { name: "email", type: "email", message: "Must be a valid email" },
    { name: "maxLength", type: "maxLength", value: 100 }
  ]
};

const validator = new FieldValidator();
const result = validator.validate("user@example.com", emailField);
```

### Other Implementations
- **Java**: 🔄 *Planned*
- **Python**: 🔄 *Planned*
- **C#**: 🔄 *Planned*
- **Go**: 🔄 *Planned*

## 📖 Documentation

### Protocol Documentation
- [**Protocol Specification**](./PROTOCOL_SPECIFICATION.md) - Complete protocol definition
- [**Examples**](./PROTOCOL_SPECIFICATION.md#examples) - Real-world usage examples

### TypeScript Implementation
- [**README**](https://cyfko.github.io/input-spec/typescript/) - Getting started guide
- [**API Reference**](https://cyfko.github.io/input-spec/typescript/API) - Complete API documentation
- [**Framework Integration**](https://cyfko.github.io/input-spec/typescript/FRAMEWORK_INTEGRATION) - Angular, React, Vue guides
- [**Performance Guide**](https://cyfko.github.io/input-spec/typescript/PERFORMANCE) - Optimization techniques
- [**Architecture**](https://cyfko.github.io/input-spec/typescript/ARCHITECTURE) - Design decisions

## 🏗️ Core Concepts

### InputFieldSpec
Defines a smart input field with constraints and value sources:
```json
{
  "displayName": "Product Category",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    { "name": "minLength", "type": "minLength", "value": 2 }
  ],
  "valuesEndpoint": {
    "url": "/api/categories",
    "searchParam": "query"
  }
}
```

### Constraint Types
- **Validation**: `required`, `minLength`, `maxLength`, `pattern`, `email`
- **Numeric**: `min`, `max`, `step`
- **Date/Time**: `minDate`, `maxDate`
- **Custom**: Extensible constraint system

### Value Sources
- **Static lists**: Predefined values
- **Remote endpoints**: RESTful APIs with search and pagination
- **Dependent fields**: Values based on other field selections

## 🌐 Framework Support

The TypeScript implementation provides native adapters for:
- **Angular**: HttpClient integration with dependency injection
- **React**: Axios adapter preserving interceptors
- **Vue.js**: Configurable HTTP client with composables
- **Vanilla JS**: Standard fetch-based implementation

## 📊 Use Cases

### Dynamic Forms
```typescript
// Form fields defined by server configuration
const formSpec = await fetch('/api/form-config/user-registration');
const fields = formSpec.fields.map(spec => createFormField(spec));
```

### Smart Autocomplete
```typescript
// Searchable product selection
const productField: InputFieldSpec = {
  displayName: "Product",
  dataType: "STRING",
  valuesEndpoint: {
    url: "/api/products/search",
    searchParam: "q",
    pageParam: "page"
  }
};
```

### Conditional Validation
```typescript
// Country-specific phone validation
const phoneField: InputFieldSpec = {
  displayName: "Phone Number",
  dataType: "STRING",
  constraints: [
    { 
      name: "pattern", 
      type: "pattern", 
      value: getPhonePatternForCountry(selectedCountry) 
    }
  ]
};
```

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### Protocol Evolution
- Propose new constraint types
- Suggest API improvements
- Submit usage examples

### New Implementations
- Implement the protocol in other languages
- Follow the specification in `PROTOCOL_SPECIFICATION.md`
- Include comprehensive tests and documentation

### TypeScript Implementation
- Bug fixes and improvements
- Framework adapter enhancements
- Performance optimizations

## 📋 Roadmap

### Short Term
- [ ] Java implementation
- [ ] Python implementation
- [ ] Enhanced constraint types (file upload, geographic)
- [ ] Real-time validation WebSocket support

### Long Term
- [ ] Visual form builder
- [ ] Multi-language constraint messages
- [ ] Advanced caching strategies
- [ ] Integration with popular form libraries

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.

## 🔗 Links

- **npm Package**: [input-field-spec-ts](https://www.npmjs.com/package/input-field-spec-ts)
- **GitHub Repository**: [cyfko/input-spec](https://github.com/cyfko/input-spec)
- **Issues & Discussions**: [GitHub Issues](https://github.com/cyfko/input-spec/issues)

---

**Version**: 1.0.0 | **Updated**: October 2025