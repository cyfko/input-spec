# API Reference (v2 Protocol)

This document covers the TypeScript API surface of `input-spec` version 2.0.0 matching the v2 protocol (atomic constraints, field‑level `valuesEndpoint`, `formatHint`, removed `enumValues`).

Audience:
- Frontend: consume & validate field specs
- Backend: generate field specs & serve dynamic value domains

## Frontend Usage: Consuming Field Specs

### Quick Start for Frontend Teams

```typescript
import { FieldValidator, InputFieldSpec, validateField } from 'input-spec';

// 1. Get field definition from YOUR backend API
const emailFieldSpec: InputFieldSpec = await fetch('/api/form-fields/email')
  .then(response => response.json());

// 2. Validate user input using backend-defined rules
const validator = new FieldValidator();
const result = validator.validate(emailFieldSpec, userEmail);

// 3. Handle validation results
if (!result.isValid) {
  // Show errors defined by your backend
  console.log(result.errors.map(e => e.message));
}
```

## Backend Usage: Generating Field Specs

### Quick Start for Backend Teams

```typescript
import { InputFieldSpec, AtomicConstraintDescriptor } from 'input-spec';

// Generate field specs in your API endpoints
app.get('/api/form-fields/email', (req, res) => {
  const emailFieldSpec: InputFieldSpec = {
    displayName: "Email Address",
    dataType: "STRING",
    expectMultipleValues: false,
    required: true,
    constraints: [
      { name: "emailPattern", type: "pattern", params: { regex: "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$" }, errorMessage: "Invalid email" },
      { name: "emailMax", type: "maxLength", params: { value: 100 }, errorMessage: "≤ 100 chars" }
    ]
  };
  
  res.json(emailFieldSpec);
});
```

## Core Types

### InputFieldSpec (v2)

```typescript
interface InputFieldSpec {
  displayName: string;
  description?: string;
  dataType: 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN';
  expectMultipleValues: boolean;
  required: boolean;
  formatHint?: string; // passive formatting / display hint
  valuesEndpoint?: ValuesEndpoint; // unified domain (INLINE or remote)
  constraints: ConstraintDescriptor[]; // ordered atomic constraints
}
```

**Real-world examples:**

```typescript
// Backend generates different email rules based on user type
function createEmailField(isPremiumUser: boolean): InputFieldSpec {
  const max = isPremiumUser ? 200 : 50;
  return {
    displayName: 'Email Address',
    dataType: 'STRING',
    expectMultipleValues: false,
    required: true,
    formatHint: 'email',
    constraints: [
      { name: 'emailPattern', type: 'pattern', params: { regex: '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$' }, errorMessage: 'Invalid email format' },
      { name: 'maxLen', type: 'maxLength', params: { value: max }, errorMessage: `Max ${max} chars` }
    ]
  };
}
};
```

### ConstraintDescriptor (Atomic)

Two forms are accepted internally during migration, but new specs MUST use the atomic form:

```typescript
interface AtomicConstraintDescriptor {
  name: string;              // stable ID
  type: ConstraintType;      // 'pattern' | 'minLength' | 'maxLength' | 'minValue' | 'maxValue' | 'minDate' | 'maxDate' | 'range' | 'custom'
  params: any;               // shape depends on type
  errorMessage?: string;
  description?: string;
}

type ConstraintDescriptor = AtomicConstraintDescriptor; // legacy union removed in public surface
```

Examples:
```typescript
{ name: 'minL', type: 'minLength', params: { value: 3 }, errorMessage: '≥ 3 chars' }
{ name: 'syntax', type: 'pattern', params: { regex: '^[a-z0-9_]+$', flags: 'i' } }
{ name: 'range', type: 'range', params: { min: 0, max: 100 } }
```

### ValidationResult

Result of field validation with detailed error information.

```typescript
interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
}

interface ValidationError {
  constraintName: string; // 'membership' for domain errors or atomic name
  message: string;
  value?: any;
  index?: number;         // present for multi-value element errors
}
```

**Properties:**
- `isValid` - Whether validation passed
- `errors` - Array of validation errors
- `constraintName` - Name of the failed constraint
- `message` - Error message (custom or default)
- `value` - The invalid value (optional)

### ValuesEndpoint

Configuration for dynamic value resolution with search and pagination.

```typescript
interface ValuesEndpoint {
  protocol: 'INLINE' | 'HTTP' | 'HTTPS' | 'GRPC';
  mode?: 'CLOSED' | 'SUGGESTIONS';
  items?: ValueAlias[];        // required if INLINE
  uri?: string;                // required if remote
  method?: 'GET' | 'POST';
  searchField?: string;
  paginationStrategy?: 'NONE' | 'PAGE_NUMBER';
  responseMapping?: ResponseMapping;
  requestParams?: RequestParams;
  cacheStrategy?: 'NONE' | 'SESSION' | 'SHORT_TERM' | 'LONG_TERM';
  debounceMs?: number;
  minSearchLength?: number;
}
```

### ValueAlias

Represents a single value option for dropdowns and selections.

```typescript
interface ValueAlias {
  value: any;     // Actual value sent to server
  label: string;  // Display text for user
}
```

**Example:**
```typescript
const countries: ValueAlias[] = [
  { value: 'FR', label: 'France' },
  { value: 'DE', label: 'Germany' },
  { value: 'US', label: 'United States' }
];
```

## Classes

### FieldValidator

Main validation engine for field specifications.

```typescript
class FieldValidator {
  validate(fieldSpec: InputFieldSpec, value: any): ValidationResult;
  // Atomic application (internal) not exposed separately.
}
```

**Methods:**

#### `validate(fieldSpec, value)`

Runs the full pipeline: required → type → membership (if CLOSED) → atomic constraints (ordered). Always evaluates all constraints (no early stop) to collect comprehensive errors unless implementation chooses optimization.

**Examples:**
```typescript
const validator = new FieldValidator();

// Validate specific constraint
const result = validator.validate(fieldSpec, 'test@example.com');
if (!result.isValid) {
  console.log(result.errors);
}
```

## Optional Coercion (Library Only)

Coercion is a convenience layer; it is NOT part of the protocol wire format. Servers MUST NOT rely on clients performing coercion. Disabled by default for strictness.

### Goals
- Accept common string representations for NUMBER / BOOLEAN without changing server specs.
- Optionally interpret epoch timestamps as dates.
- Permit per-field overrides where only some inputs are lenient.

### Activation
Two levers (field override wins over global):
1. Global: `new FieldValidator({ coercion: { coerce: true } })`
2. Per field: add `coercion: { coerce: true }` inside `InputFieldSpec` (library-only property).

### Available Options (ValidationOptions.coercion or fieldSpec.coercion)
| Option | Type | Default | Purpose |
|--------|------|---------|---------|
| `coerce` | boolean | false | Master on/off switch |
| `acceptNumericBoolean` | boolean | false | Accept "1"/"0" (and configurable extra) as booleans |
| `extraTrueValues` | string[] | [] | Additional truthy tokens (lower‑cased) |
| `extraFalseValues` | string[] | [] | Additional falsey tokens (lower‑cased) |
| `numberPattern` | RegExp | `/^-?\\d+(\\.\\d+)?$/` | Override numeric detection |
| `dateEpochSupport` | boolean | false | Treat integer seconds or millis as Date ISO strings |
| `trimStrings` | boolean | true | Trim leading/trailing whitespace before other checks |

### Behavior Summary
| DataType | Accepted When Coercion Enabled | Result After Coercion |
|----------|--------------------------------|-----------------------|
| NUMBER | "42", "3.14" | 42, 3.14 (number) |
| BOOLEAN | "true","false" always (case‑insens.), optionally "1"/"0" | true / false |
| DATE | ISO 8601 passes unchanged; if `dateEpochSupport` then 1700000000 or 1700000000000 | ISO string (from Date.toISOString()) |

Membership checks use a loose compare: if a value is coerced, numeric vs string numeric and boolean vs string boolean differences are ignored when matching INLINE domain values.

### Examples
```typescript
import { FieldValidator } from 'input-spec';

const v = new FieldValidator({ coercion: { coerce: true, acceptNumericBoolean: true } });

const boolSpec = { displayName: 'Flag', dataType: 'BOOLEAN', expectMultipleValues: false, required: true, constraints: [] };
v.validate(boolSpec, 'TRUE');   // valid -> true
v.validate(boolSpec, '1');      // valid if acceptNumericBoolean

const numSpec = { displayName: 'Amount', dataType: 'NUMBER', expectMultipleValues: false, required: true, constraints: [] };
v.validate(numSpec, '12.5');    // valid -> 12.5

const dateSpec = { displayName: 'When', dataType: 'DATE', expectMultipleValues: false, required: true, constraints: [], coercion: { coerce: true, dateEpochSupport: true } };
v.validate(dateSpec, 1700000000); // epoch seconds -> coerced ISO string
```

### Per-Field Override Use Case
Enable leniency only for a problematic legacy field:
```typescript
const strictNumber = { displayName: 'Code', dataType: 'NUMBER', expectMultipleValues: false, required: true, constraints: [] };
const lenientNumber = { ...strictNumber, coercion: { coerce: true } };

new FieldValidator().validate(strictNumber, '5');        // invalid (no coercion)
new FieldValidator().validate(lenientNumber, '5');       // valid
```

### Non-Goals
- No locale-specific parsing (commas, spaces).
- No partial date formats; rely on backend specifying accepted format through `formatHint`.
- Does not mutate original value in the caller; coercion occurs inside validation pipeline only.

### Recommendation
Use coercion sparingly: enable globally only if your UI routinely provides raw form input strings without local pre-parsing.

### ValuesResolver

Orchestrates dynamic value resolution with caching and HTTP requests.

```typescript
// (v2 minimal core does not ship an HTTP values resolver in zero‑dep build.)
// Provide your own remote fetching layer as needed.
```

**Constructor:**
- `httpClient` - HTTP client implementation
- `cacheProvider` - Cache provider implementation

**Methods:**

#### `resolveValues(endpoint, params?)`

Resolves values from dynamic endpoint with caching and pagination.

**Parameters:**
- `endpoint` - ValuesEndpoint configuration
- `params` - Optional search and pagination parameters

**Returns:** `Promise<ValuesResult>`

**Example:**
```typescript
// Example INLINE domain
const countrySpec: InputFieldSpec = {
  displayName: 'Country',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: false,
  valuesEndpoint: {
    protocol: 'INLINE',
    mode: 'CLOSED',
    items: [
      { value: 'FR', label: 'France' },
      { value: 'DE', label: 'Germany' }
    ]
  },
  constraints: []
};
```

### FetchHttpClient

HTTP client implementation using native fetch API.

```typescript
class FetchHttpClient implements HttpClient {
  async request(config: HttpRequestConfig): Promise<HttpResponse>
}
```

**Features:**
- Zero dependencies (uses native fetch)
- Automatic JSON parsing
- Error handling with detailed messages
- Support for GET and POST methods

### MemoryCacheProvider

In-memory cache implementation using native Map.

```typescript
class MemoryCacheProvider implements CacheProvider {
  constructor(private defaultTtl: number = 300000) // 5 minutes
  
  get(key: string): any | null
  set(key: string, value: any, ttl?: number): void
  delete(key: string): void
  clear(): void
}
```

**Features:**
- TTL (Time To Live) support
- Automatic cleanup of expired entries
- Zero dependencies (uses native Map)
- Memory efficient

## Interfaces

### HttpClient

Abstraction for HTTP requests enabling dependency injection.

```typescript
interface HttpClient {
  request(config: HttpRequestConfig): Promise<HttpResponse>;
}
```

### CacheProvider

Abstraction for caching enabling different cache implementations.

```typescript
interface CacheProvider {
  get(key: string): any | null;
  set(key: string, value: any, ttl?: number): void;
  delete(key: string): void;
  clear(): void;
}
```

## Utility Functions

### validateField(fieldSpec, value)

Convenience function for simple field validation.

```typescript
function validateField(
  fieldSpec: InputFieldSpec,
  value: any
): Promise<ValidationResult>
```

**Example:**
```typescript
import { validateField } from './src';

const result = await validateField(emailFieldSpec, 'test@example.com');
```

### createDefaultValuesEndpoint(uri)

Creates a ValuesEndpoint with sensible defaults.

```typescript
function createDefaultValuesEndpoint(uri: string): ValuesEndpoint
```

**Example:**
```typescript
const endpoint = createDefaultValuesEndpoint('https://api.example.com/users');
// Returns endpoint with default pagination, caching, and response mapping
```

### Type Guards

#### isInputFieldSpec(obj)

Type guard to check if object is a valid InputFieldSpec.

```typescript
function isInputFieldSpec(obj: any): obj is InputFieldSpec
```

#### isConstraintDescriptor(obj)

Type guard to check if object is a valid ConstraintDescriptor.

```typescript
function isConstraintDescriptor(obj: any): obj is ConstraintDescriptor
```

#### isValidationResult(obj)

Type guard to check if object is a valid ValidationResult.

```typescript
function isValidationResult(obj: any): obj is ValidationResult
```

## Examples

### Basic Field Validation

```typescript
import { FieldValidator, InputFieldSpec } from './src';

const passwordSpec: InputFieldSpec = {
  displayName: 'Password',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,
  constraints: [
    { name: 'minL', type: 'minLength', params: { value: 8 }, errorMessage: '≥ 8 chars' },
    { name: 'maxL', type: 'maxLength', params: { value: 128 }, errorMessage: '≤ 128 chars' },
    { name: 'strength', type: 'pattern', params: { regex: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)' }, errorMessage: 'Need lower, upper, digit' }
  ]
};

const validator = new FieldValidator();

// Validate step by step
const resultPwd = validator.validate(passwordSpec, 'Weak');
console.log(resultPwd.errors);
```

### Dynamic Values with Search

```typescript
import { 
  ValuesResolver, 
  FetchHttpClient, 
  MemoryCacheProvider,
  createDefaultValuesEndpoint 
} from './src';

const resolver = new ValuesResolver(
  new FetchHttpClient(),
  new MemoryCacheProvider()
);

const countriesEndpoint = createDefaultValuesEndpoint('https://api.example.com/countries');

// Search for countries
const searchResult = await resolver.resolveValues(countriesEndpoint, {
  search: 'united',
  page: 1,
  limit: 10
});

console.log('Found countries:', searchResult.values);
console.log('Has more:', searchResult.hasNext);
```

### Array Field Validation

```typescript
const skillsSpec: InputFieldSpec = {
  displayName: 'Skills',
  dataType: 'STRING',
  expectMultipleValues: true,
  required: false,
  constraints: [
    { name: 'minSkills', type: 'minValue', params: { value: 1 }, errorMessage: 'At least 1 skill' },
    { name: 'maxSkills', type: 'maxValue', params: { value: 5 }, errorMessage: 'At most 5 skills' },
    { name: 'skillFormat', type: 'pattern', params: { regex: '^[a-zA-Z\\s]+$' }, errorMessage: 'Letters/spaces only' }
  ]
};

const skills = ['JavaScript', 'TypeScript', 'React'];
const result = await validator.validate(skillsSpec, skills);
```

### Custom Error Messages

```typescript
const customFieldSpec: InputFieldSpec = {
  displayName: 'Username',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,
  constraints: [
    { name: 'syntax', type: 'pattern', params: { regex: '^[a-zA-Z0-9_]+' }, errorMessage: 'Alnum + underscore' },
    { name: 'minL', type: 'minLength', params: { value: 3 } },
    { name: 'maxL', type: 'maxLength', params: { value: 20 } }
  ]
};
```

### Static Enumeration (v2 INLINE)

```typescript
const statusSpec: InputFieldSpec = {
  displayName: 'Status',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,
  valuesEndpoint: {
    protocol: 'INLINE',
    mode: 'CLOSED',
    items: [
      { value: 'ACTIVE', label: 'Active' },
      { value: 'INACTIVE', label: 'Inactive' },
      { value: 'PENDING', label: 'Pending' }
    ]
  },
  constraints: []
};
```

## Migration From v1
v2 introduces atomic constraints and a unified `valuesEndpoint` replacing `enumValues`. A helper `migrateV1Spec(legacy: LegacyInputFieldSpec): InputFieldSpec` performs a mechanical, best‑effort upgrade.

### What It Transforms
- `enumValues` -> `valuesEndpoint: { protocol: 'INLINE', mode: 'CLOSED', items: [...] }`
- Legacy embedded `valuesEndpoint` (rare) -> lifted untouched if already compatible
- `min` / `max` (numeric) -> separate `minValue` / `maxValue` atomic descriptors
- `min` / `max` (string length) -> `minLength` / `maxLength`
- `pattern` -> `pattern`
- Legacy `format` -> `formatHint`

### Example
Legacy (v1):
```jsonc
{
  "displayName": "Age",
  "dataType": "NUMBER",
  "expectMultipleValues": false,
  "required": true,
  "min": 18,
  "max": 120,
  "enumValues": [
    { "value": 18, "label": "18" },
    { "value": 21, "label": "21" }
  ]
}
```

Upgraded (v2):
```jsonc
{
  "displayName": "Age",
  "dataType": "NUMBER",
  "expectMultipleValues": false,
  "required": true,
  "valuesEndpoint": {
    "protocol": "INLINE",
    "mode": "CLOSED",
    "items": [
      { "value": 18, "label": "18" },
      { "value": 21, "label": "21" }
    ]
  },
  "constraints": [
    { "name": "min", "type": "minValue", "params": { "value": 18 } },
    { "name": "max", "type": "maxValue", "params": { "value": 120 } }
  ]
}
```

### Caveats & Manual Review Checklist
1. Ordering: The migration preserves no explicit ordering information for legacy overlapping semantics beyond `min` before `max`. If your UI depended on original ordering of multiple pattern-like validations, review and adjust.
2. Enum Label Collisions: If different `enumValues` entries share the same `value` with different `label`s, they will be kept as‑is; verify the dataset for duplicates.
3. Mixed Numeric & Length Bounds: If a v1 spec mixed usage (e.g., numeric `min` with a length-based interpretation) you must clarify intent manually.
4. Custom / Unsupported Fields: Unrecognized legacy keys are copied through into the atomic `params` only if they map cleanly; otherwise they remain unused and should be removed.
5. Default Values: Legacy `defaultValue` (if present) is not enforced by the validator; surface it separately in your UI logic.

### Usage
```typescript
import { migrateV1Spec } from 'input-spec';

const upgraded = migrateV1Spec(legacySpec);
// Validate with standard v2 pipeline
const result = new FieldValidator().validate(upgraded, userValue);
```

### Non‑Goals
The migration does NOT attempt semantic inference (e.g., turning a combined range into a single `range` descriptor); it emits distinct atomic constraints for minimal ambiguity.

### Recommendation
Run migration once on the backend at spec generation time and store / serve only v2 forward. Avoid migrating on every request.

## TypeScript Support

All types are fully typed with TypeScript strict mode:

```typescript
// Full type inference
const spec: InputFieldSpec = { /* ... */ };
const validator = new FieldValidator();
const result = validator.validate(spec, value);

// result.isValid is boolean
// result.errors is ValidationError[]
// Full IDE autocomplete and type checking
```

## Error Handling

```typescript
try {
  const result = validator.validate(fieldSpec, value);
  
  if (!result.isValid) {
    result.errors.forEach(error => {
      console.log(`Constraint '${error.constraintName}' failed: ${error.message}`);
    });
  }
} catch (error) {
  console.error('Validation error:', error.message);
}
```

## Performance

- **Zero dependencies**: No runtime overhead from external libraries
- **Deterministic order**: Atomic constraint evaluation sequence is stable
- **Indexed errors**: Multi-value membership / constraint violations include `index`
- **Zero deps**: Small surface optimized for embedding