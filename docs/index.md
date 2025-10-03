------

title: "Dynamic Input Field Specification Protocol"title: "Dynamic Input Field Specification Protocol"

description: "Stop hardcoding forms! Let your backend define validation rules and form fields dynamically."description: "Stop hardcoding forms! Let your backend define validation rules and form fields ### 📚 Deep Dive Documentation

---- [📋 **Protocol Specification**](https://github.com/cyfko/input-spec/blob/main/PROTOCOL_SPECIFICATION.md) - Complete technical specification

- [🚀 **TypeScript Implementation**](https://cyfko.github.io/input-spec/typescript/) - Getting started guide

# Stop Hardcoding Forms! 🚀- [⚙️ **Framework Integration**](https://cyfko.github.io/input-spec/typescript/FRAMEWORK_INTEGRATION) - Angular, React, Vue examples

- [📊 **Performance Guide**](https://cyfko.github.io/input-spec/typescript/PERFORMANCE) - Optimization techniquesically."

## The Problem Every Developer Faces---



You're building a user registration form. The backend team says:# Stop Hardcoding Forms! 🚀

- "Email is required, max 100 characters"

- "Phone number format depends on the country"## The Problem Every Developer Faces

- "Available departments come from our API"

- "Validation rules change per client configuration"You're building a user registration form. The backend team says:

- "Email is required, max 100 characters"

**Your current solution?** Hardcode everything in the frontend. Again. 😤- "Phone number format depends on the country"

- "Available departments come from our API"

```javascript- "Validation rules change per client configuration"

// 😩 Every time requirements change...

const emailValidation = {**Your current solution?** Hardcode everything in the frontend. Again. 😤

  required: true,

  maxLength: 100,```javascript

  pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/// 😩 Every time requirements change...

};const emailValidation = {

  required: true,

// 😩 More hardcoded rules...  maxLength: 100,

const departmentOptions = [  pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  "Engineering", "Marketing", "Sales" // Hardcoded list};

];

```// 😩 More hardcoded rules...

const departmentOptions = [

## The Game-Changing Solution 🎯  "Engineering", "Marketing", "Sales" // Hardcoded list

];

**What if your backend could send the form definition AND validation rules?**```



```json## The Game-Changing Solution 🎯

{

  "displayName": "Email Address",**What if your backend could send the form definition AND validation rules?**

  "dataType": "STRING",

  "required": true,```json

  "constraints": [{

    { "name": "email", "pattern": "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "errorMessage": "Please enter a valid email" },  "displayName": "Email Address",

    { "name": "maxLength", "max": 100, "errorMessage": "Email too long" }  "dataType": "STRING",

  ]  "required": true,

}  "constraints": [

```    { "name": "email", "type": "email", "message": "Please enter a valid email" },

    { "name": "maxLength", "type": "maxLength", "value": 100 }

**What if dropdown options came from live API calls with search?**  ]

}

```json```

{

  "displayName": "Department",**What if dropdown options came from live API calls with search?**

  "dataType": "STRING",

  "valuesEndpoint": {```json

    "url": "/api/departments/search",{

    "searchParam": "query"  "displayName": "Department",

  }  "dataType": "STRING",

}  "valuesEndpoint": {

```    "url": "/api/departments/search",

    "searchParam": "query"

## This Changes Everything! ⚡  }

}

### ✅ For Frontend Developers```

- **No more hardcoded validation rules** - everything comes from backend

- **Dynamic form generation** - forms adapt to business logic changes## This Changes Everything! ⚡

- **Smart autocomplete** - searchable dropdowns with real-time API calls

- **Zero frontend updates** when validation rules change### ✅ For Frontend Developers

- **No more hardcoded validation rules** - everything comes from backend

### ✅ For Backend Developers- **Dynamic form generation** - forms adapt to business logic changes

- **Control validation from one place** - your API defines the rules- **Smart autocomplete** - searchable dropdowns with real-time API calls

- **Dynamic business logic** - different rules per tenant/config- **Zero frontend updates** when validation rules change

- **Type-safe contracts** - clear interface between frontend and backend

### ✅ For Backend Developers  

### ✅ For Product Teams- **Control validation from one place** - your API defines the rules

- **Faster feature delivery** - no coordination between frontend/backend for form changes- **Dynamic business logic** - different rules per tenant/config

- **A/B testing forms** - change validation rules without deploys- **Type-safe contracts** - clear interface between frontend and backend

- **Multi-tenant flexibility** - different validation per client

### ✅ For Product Teams

## Real-World Magic ✨- **Faster feature delivery** - no coordination between frontend/backend for form changes

- **A/B testing forms** - change validation rules without deploys

### Scenario 1: Multi-Country Phone Validation- **Multi-tenant flexibility** - different validation per client

```typescript

// Backend sends different rules based on user's country## Real-World Magic ✨

const phoneField = await fetch('/api/form-fields/phone?country=FR');

// Returns French phone validation automatically!### Scenario 1: Multi-Country Phone Validation

``````typescript

// Backend sends different rules based on user's country

### Scenario 2: Smart Product Searchconst phoneField = await fetch('/api/form-fields/phone?country=FR');

```typescript// Returns French phone validation automatically!

// User types "iPhone" -> instant API search -> filtered results```

const productField: InputFieldSpec = {

  displayName: "Product",### Scenario 2: Smart Product Search

  valuesEndpoint: {```typescript

    url: "/api/products/search",// User types "iPhone" -> instant API search -> filtered results

    searchParam: "q",const productField: InputFieldSpec = {

    minSearchLength: 2  displayName: "Product",

  }  valuesEndpoint: {

};    url: "/api/products/search",

```    searchParam: "q",

    minSearchLength: 2

### Scenario 3: Dynamic Business Rules  }

```typescript};

// VIP customers get different validation rules```

const creditLimitField = await fetch('/api/form-fields/credit-limit?userTier=VIP');

// Returns higher limits automatically!### Scenario 3: Dynamic Business Rules

``````typescript

// VIP customers get different validation rules

## Get Started in 2 Minutes ⚡const creditLimitField = await fetch('/api/form-fields/credit-limit?userTier=VIP');

// Returns higher limits automatically!

### Install```

```bash

npm install input-field-spec-ts## Get Started in 2 Minutes ⚡

```

### Install

### Use in React/Vue/Angular```bash

```typescriptnpm install input-field-spec-ts

import { InputFieldSpec, FieldValidator } from 'input-field-spec-ts';```



// 1. Fetch field definition from YOUR backend### Use in React/Vue/Angular

const fieldSpec = await fetch('/api/form-fields/email').then(r => r.json());```typescript

import { InputFieldSpec, FieldValidator } from 'input-field-spec-ts';

// 2. Validate user input

const validator = new FieldValidator();// 1. Fetch field definition from YOUR backend

const result = validator.validate(userInput, fieldSpec);const fieldSpec = await fetch('/api/form-fields/email').then(r => r.json());



// 3. That's it! Backend controls everything 🎉// 2. Validate user input

```const validator = new FieldValidator();

const result = validator.validate(userInput, fieldSpec);

## Who's This For? 🎯

// 3. That's it! Backend controls everything 🎉

### ✅ Perfect If You Have:```

- **Dynamic forms** that change based on business logic

- **Multi-tenant apps** with different validation per client## Who's This For? 🎯

- **Complex validation rules** that change frequently

- **Autocomplete fields** with live data### ✅ Perfect If You Have:

- **Backend-driven UI** requirements- **Dynamic forms** that change based on business logic

- **Multi-tenant apps** with different validation per client  

### ❌ Probably Overkill If:- **Complex validation rules** that change frequently

- Simple static forms that never change- **Autocomplete fields** with live data

- Single-tenant app with fixed validation- **Backend-driven UI** requirements

- No backend integration needed

### ❌ Probably Overkill If:

## Framework Support 🌐- Simple static forms that never change

- Single-tenant app with fixed validation

**Ready-to-use adapters for:**- No backend integration needed

- **Angular** - HttpClient integration with dependency injection

- **React** - Axios adapter preserving your interceptors## Framework Support 🌐

- **Vue.js** - Composables with reactive validation

- **Vanilla JS** - Standard fetch-based implementation**Ready-to-use adapters for:**

- **Angular** - HttpClient integration with dependency injection

## Ready to Transform Your Forms? 🚀- **React** - Axios adapter preserving your interceptors

- **Vue.js** - Composables with reactive validation

### 📚 Deep Dive Documentation- **Vanilla JS** - Standard fetch-based implementation

- [📋 **Protocol Specification**](https://github.com/cyfko/input-spec/blob/main/PROTOCOL_SPECIFICATION.md) - Complete technical specification

- [🚀 **TypeScript Implementation**](https://cyfko.github.io/input-spec/typescript/) - Getting started guide## Ready to Transform Your Forms? 🚀

- [⚙️ **Framework Integration**](https://cyfko.github.io/input-spec/typescript/FRAMEWORK_INTEGRATION) - Angular, React, Vue examples

- [📊 **Performance Guide**](https://cyfko.github.io/input-spec/typescript/PERFORMANCE) - Optimization techniques### � Deep Dive Documentation

- [📋 **Protocol Specification**](https://github.com/cyfko/input-spec/blob/main/PROTOCOL_SPECIFICATION.md) - Complete technical specification

### 🔗 Quick Links- [🚀 **TypeScript Implementation**](https://github.com/cyfko/input-spec/blob/main/impl/typescript/README.md) - Getting started guide

- [**npm Package**](https://www.npmjs.com/package/input-field-spec-ts) - `input-field-spec-ts@1.0.0`- [⚙️ **Framework Integration**](https://github.com/cyfko/input-spec/blob/main/impl/typescript/docs/FRAMEWORK_INTEGRATION.md) - Angular, React, Vue examples

- [**GitHub Repository**](https://github.com/cyfko/input-spec) - Source code and examples- [📊 **Performance Guide**](https://github.com/cyfko/input-spec/blob/main/impl/typescript/docs/PERFORMANCE.md) - Optimization techniques

- [**Report Issues**](https://github.com/cyfko/input-spec/issues) - Bug reports and feature requests

### 🔗 Quick Links

---- [**npm Package**](https://www.npmjs.com/package/input-field-spec-ts) - `input-field-spec-ts@1.0.0`

- [**GitHub Repository**](https://github.com/cyfko/input-spec) - Source code and examples

**Ready to stop hardcoding forms?** [Get started now!](https://cyfko.github.io/input-spec/typescript/) 🎯- [**Report Issues**](https://github.com/cyfko/input-spec/issues) - Bug reports and feature requests

---

**Ready to stop hardcoding forms?** [Get started now!](https://cyfko.github.io/input-spec/typescript/) 🎯