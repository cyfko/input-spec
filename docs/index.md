---
title: "Dynamic Input Field Specification Protocol"
description: "Stop hardcoding forms! Let your backend define validation rules and form fields dynamically."
---

# Stop Hardcoding Forms! 🚀

## The Problem Every Developer Faces

You're building a user registration form. The backend team says:
- "Email is required, max 100 characters"
- "Phone number format depends on the country"
- "Available departments come from our API"
- "Validation rules change per client configuration"

**Your current solution?** Hardcode everything in the frontend. Again. 😤

```javascript
// 😩 Every time requirements change...
const emailValidation = {
  required: true,
  maxLength: 100,
  pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/
};

// 😩 More hardcoded rules...
const departmentOptions = [
  "Engineering", "Marketing", "Sales" // Hardcoded list
];
```

## The Game-Changing Solution 🎯

**What if your backend could send the form definition AND validation rules?**

```json
{
  "displayName": "Email Address",
  "dataType": "STRING",
  "required": true,
  "constraints": [
    { "name": "email", "type": "email", "message": "Please enter a valid email" },
    { "name": "maxLength", "type": "maxLength", "value": 100 }
  ]
}
```

**What if dropdown options came from live API calls with search?**

```json
{
  "displayName": "Department",
  "dataType": "STRING",
  "valuesEndpoint": {
    "url": "/api/departments/search",
    "searchParam": "query"
  }
}
```

## This Changes Everything! ⚡

### ✅ For Frontend Developers
- **No more hardcoded validation rules** - everything comes from backend
- **Dynamic form generation** - forms adapt to business logic changes
- **Smart autocomplete** - searchable dropdowns with real-time API calls
- **Zero frontend updates** when validation rules change

### ✅ For Backend Developers  
- **Control validation from one place** - your API defines the rules
- **Dynamic business logic** - different rules per tenant/config
- **Type-safe contracts** - clear interface between frontend and backend

### ✅ For Product Teams
- **Faster feature delivery** - no coordination between frontend/backend for form changes
- **A/B testing forms** - change validation rules without deploys
- **Multi-tenant flexibility** - different validation per client

## Real-World Magic ✨

### Scenario 1: Multi-Country Phone Validation
```typescript
// Backend sends different rules based on user's country
const phoneField = await fetch('/api/form-fields/phone?country=FR');
// Returns French phone validation automatically!
```

### Scenario 2: Smart Product Search
```typescript
// User types "iPhone" -> instant API search -> filtered results
const productField: InputFieldSpec = {
  displayName: "Product",
  valuesEndpoint: {
    url: "/api/products/search",
    searchParam: "q",
    minSearchLength: 2
  }
};
```

### Scenario 3: Dynamic Business Rules
```typescript
// VIP customers get different validation rules
const creditLimitField = await fetch('/api/form-fields/credit-limit?userTier=VIP');
// Returns higher limits automatically!
```

## Get Started in 2 Minutes ⚡

### Install
```bash
npm install input-field-spec-ts
```

### Use in React/Vue/Angular
```typescript
import { InputFieldSpec, FieldValidator } from 'input-field-spec-ts';

// 1. Fetch field definition from YOUR backend
const fieldSpec = await fetch('/api/form-fields/email').then(r => r.json());

// 2. Validate user input
const validator = new FieldValidator();
const result = validator.validate(userInput, fieldSpec);

// 3. That's it! Backend controls everything 🎉
```

## Who's This For? 🎯

### ✅ Perfect If You Have:
- **Dynamic forms** that change based on business logic
- **Multi-tenant apps** with different validation per client  
- **Complex validation rules** that change frequently
- **Autocomplete fields** with live data
- **Backend-driven UI** requirements

### ❌ Probably Overkill If:
- Simple static forms that never change
- Single-tenant app with fixed validation
- No backend integration needed

## Framework Support 🌐

**Ready-to-use adapters for:**
- **Angular** - HttpClient integration with dependency injection
- **React** - Axios adapter preserving your interceptors
- **Vue.js** - Composables with reactive validation
- **Vanilla JS** - Standard fetch-based implementation

## Ready to Transform Your Forms? 🚀

### � Deep Dive Documentation
- [📋 **Protocol Specification**](https://github.com/cyfko/input-spec/blob/main/PROTOCOL_SPECIFICATION.md) - Complete technical specification
- [🚀 **TypeScript Implementation**](https://cyfko.github.io/input-spec/typescript/) - Getting started guide 🚀
- [⚙️ **Framework Integration**](https://cyfko.github.io/input-spec/typescript/FRAMEWORK_INTEGRATION) - Angular, React, Vue examples
- [📊 **Performance Guide**](https://cyfko.github.io/input-spec/typescript/PERFORMANCE) - Optimization techniques

### 🔗 Quick Links
- [**npm Package**](https://www.npmjs.com/package/input-field-spec-ts) - `input-field-spec-ts@1.0.0`
- [**GitHub Repository**](https://github.com/cyfko/input-spec) - Source code and examples
- [**Report Issues**](https://github.com/cyfko/input-spec/issues) - Bug reports and feature requests

---

**Ready to stop hardcoding forms?** [Get started now!](https://github.com/cyfko/input-spec/blob/main/impl/typescript/README.md) 🎯
