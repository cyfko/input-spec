---------

title: "Dynamic Input Field Specification Protocol"

description: "Stop hardcoding forms! Let your backend define validation rules and form fields dynamically."title: "Dynamic Input Field Specification Protocol"title: "Dynamic Input Field Specification Protocol"

---

description: "Stop hardcoding forms! Let your backend define validation rules and form fields dynamically."description: "Stop hardcoding forms! Let your backend define validation rules and form fields ### 📚 Deep Dive Documentation

# Stop Hardcoding Forms! 🚀

---- [📋 **Protocol Specification**](https://github.com/cyfko/input-spec/blob/main/PROTOCOL_SPECIFICATION.md) - Complete technical specification

## The Problem Every Developer Faces

- [🚀 **TypeScript Implementation**](https://cyfko.github.io/input-spec/typescript/) - Getting started guide

You're building a user registration form. The backend team says:

- "Email is required, max 100 characters"# Stop Hardcoding Forms! 🚀- [⚙️ **Framework Integration**](https://cyfko.github.io/input-spec/typescript/FRAMEWORK_INTEGRATION) - Angular, React, Vue examples

- "Phone number format depends on the country"

- "Available departments come from our API"- [📊 **Performance Guide**](https://cyfko.github.io/input-spec/typescript/PERFORMANCE) - Optimization techniquesically."

- "Validation rules change per client configuration"

## The Problem Every Developer Faces---

**Your current solution?** Hardcode everything in the frontend. Again. 😤



```javascript

// 😩 Every time requirements change...You're building a user registration form. The backend team says:# Stop Hardcoding Forms! 🚀

const emailValidation = {

  required: true,- "Email is required, max 100 characters"

  maxLength: 100,

  pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/- "Phone number format depends on the country"## The Problem Every Developer Faces

};

- "Available departments come from our API"

// 😩 More hardcoded rules...

const departmentOptions = [- "Validation rules change per client configuration"You're building a user registration form. The backend team says:

  "Engineering", "Marketing", "Sales" // Hardcoded list

];- "Email is required, max 100 characters"

```

**Your current solution?** Hardcode everything in the frontend. Again. 😤- "Phone number format depends on the country"

## The Game-Changing Solution 🎯

- "Available departments come from our API"

**What if your backend could send the form definition AND validation rules?**

```javascript- "Validation rules change per client configuration"

```json

{// 😩 Every time requirements change...

  "displayName": "Email Address",

  "dataType": "STRING",const emailValidation = {**Your current solution?** Hardcode everything in the frontend. Again. 😤

  "required": true,

  "constraints": [  required: true,

    { "name": "email", "pattern": "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "errorMessage": "Please enter a valid email" },

    { "name": "maxLength", "max": 100, "errorMessage": "Email too long" }  maxLength: 100,```javascript

  ]

}  pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/// 😩 Every time requirements change...

```

};const emailValidation = {

**What if dropdown options came from live API calls with search?**

  required: true,

```json

{// 😩 More hardcoded rules...  maxLength: 100,

  "displayName": "Department",

  "dataType": "STRING",const departmentOptions = [  pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  "valuesEndpoint": {

    "url": "/api/departments/search",  "Engineering", "Marketing", "Sales" // Hardcoded list};

    "searchParam": "query"

  }];

}

``````// 😩 More hardcoded rules...



## This Changes Everything! ⚡const departmentOptions = [



### ✅ For Frontend Developers## The Game-Changing Solution 🎯  "Engineering", "Marketing", "Sales" // Hardcoded list

- **No more hardcoded validation rules** - everything comes from backend

- **Dynamic form generation** - forms adapt to business logic changes];

- **Smart autocomplete** - searchable dropdowns with real-time API calls

- **Zero frontend updates** when validation rules change**What if your backend could send the form definition AND validation rules?**```



### ✅ For Backend Developers

- **Control validation from one place** - your API defines the rules

- **Dynamic business logic** - different rules per tenant/config```json## The Game-Changing Solution 🎯

- **Type-safe contracts** - clear interface between frontend and backend

{

### ✅ For Product Teams

- **Faster feature delivery** - no coordination between frontend/backend for form changes  "displayName": "Email Address",**What if your backend could send the form definition AND validation rules?**

- **A/B testing forms** - change validation rules without deploys

- **Multi-tenant flexibility** - different validation per client  "dataType": "STRING",



## Real-World Magic ✨  "required": true,```json



### Scenario 1: Multi-Country Phone Validation  "constraints": [{

```typescript

// Backend sends different rules based on user's country    { "name": "email", "pattern": "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "errorMessage": "Please enter a valid email" },  "displayName": "Email Address",

const phoneField = await fetch('/api/form-fields/phone?country=FR');

// Returns French phone validation automatically!    { "name": "maxLength", "max": 100, "errorMessage": "Email too long" }  "dataType": "STRING",

```

  ]  "required": true,

### Scenario 2: Smart Product Search

```typescript}  "constraints": [

// User types "iPhone" -> instant API search -> filtered results

const productField: InputFieldSpec = {```    { "name": "email", "type": "email", "message": "Please enter a valid email" },

  displayName: "Product",

  valuesEndpoint: {    { "name": "maxLength", "type": "maxLength", "value": 100 }

    url: "/api/products/search",

    searchParam: "q",**What if dropdown options came from live API calls with search?**  ]

    minSearchLength: 2

  }}

};

``````json```



### Scenario 3: Dynamic Business Rules{

```typescript

// VIP customers get different validation rules  "displayName": "Department",**What if dropdown options came from live API calls with search?**

const creditLimitField = await fetch('/api/form-fields/credit-limit?userTier=VIP');

// Returns higher limits automatically!  "dataType": "STRING",

```

  "valuesEndpoint": {```json

## Get Started in 2 Minutes ⚡

    "url": "/api/departments/search",{

### Install

```bash    "searchParam": "query"  "displayName": "Department",

npm install input-field-spec-ts

```  }  "dataType": "STRING",



### Use in React/Vue/Angular}  "valuesEndpoint": {

```typescript

import { InputFieldSpec, FieldValidator } from 'input-field-spec-ts';```    "url": "/api/departments/search",



// 1. Fetch field definition from YOUR backend    "searchParam": "query"

const fieldSpec = await fetch('/api/form-fields/email').then(r => r.json());

## This Changes Everything! ⚡  }

// 2. Validate user input

const validator = new FieldValidator();}

const result = validator.validate(userInput, fieldSpec);

### ✅ For Frontend Developers```

// 3. That's it! Backend controls everything 🎉

```- **No more hardcoded validation rules** - everything comes from backend



## Who's This For? 🎯- **Dynamic form generation** - forms adapt to business logic changes## This Changes Everything! ⚡



### ✅ Perfect If You Have:- **Smart autocomplete** - searchable dropdowns with real-time API calls

- **Dynamic forms** that change based on business logic

- **Multi-tenant apps** with different validation per client- **Zero frontend updates** when validation rules change### ✅ For Frontend Developers

- **Complex validation rules** that change frequently

- **Autocomplete fields** with live data- **No more hardcoded validation rules** - everything comes from backend

- **Backend-driven UI** requirements

### ✅ For Backend Developers- **Dynamic form generation** - forms adapt to business logic changes

### ❌ Probably Overkill If:

- Simple static forms that never change- **Control validation from one place** - your API defines the rules- **Smart autocomplete** - searchable dropdowns with real-time API calls

- Single-tenant app with fixed validation

- No backend integration needed- **Dynamic business logic** - different rules per tenant/config- **Zero frontend updates** when validation rules change



## Framework Support 🌐- **Type-safe contracts** - clear interface between frontend and backend



**Ready-to-use adapters for:**### ✅ For Backend Developers  

- **Angular** - HttpClient integration with dependency injection

- **React** - Axios adapter preserving your interceptors### ✅ For Product Teams- **Control validation from one place** - your API defines the rules

- **Vue.js** - Composables with reactive validation

- **Vanilla JS** - Standard fetch-based implementation- **Faster feature delivery** - no coordination between frontend/backend for form changes- **Dynamic business logic** - different rules per tenant/config



## Ready to Transform Your Forms? 🚀- **A/B testing forms** - change validation rules without deploys- **Type-safe contracts** - clear interface between frontend and backend



### 📚 Deep Dive Documentation- **Multi-tenant flexibility** - different validation per client

- [📋 **Protocol Specification**](https://github.com/cyfko/input-spec/blob/main/PROTOCOL_SPECIFICATION.md) - Complete technical specification

- [🚀 **TypeScript Implementation**](https://cyfko.github.io/input-spec/typescript/) - Getting started guide### ✅ For Product Teams

- [⚙️ **Framework Integration**](https://cyfko.github.io/input-spec/typescript/FRAMEWORK_INTEGRATION) - Angular, React, Vue examples

- [📊 **Performance Guide**](https://cyfko.github.io/input-spec/typescript/PERFORMANCE) - Optimization techniques## Real-World Magic ✨- **Faster feature delivery** - no coordination between frontend/backend for form changes



### 🔗 Quick Links- **A/B testing forms** - change validation rules without deploys

- [**npm Package**](https://www.npmjs.com/package/input-field-spec-ts) - `input-field-spec-ts@1.0.0`

- [**GitHub Repository**](https://github.com/cyfko/input-spec) - Source code and examples### Scenario 1: Multi-Country Phone Validation- **Multi-tenant flexibility** - different validation per client

- [**Report Issues**](https://github.com/cyfko/input-spec/issues) - Bug reports and feature requests

```typescript

---

// Backend sends different rules based on user's country## Real-World Magic ✨

**Ready to stop hardcoding forms?** [Get started now!](https://cyfko.github.io/input-spec/typescript/) 🎯
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