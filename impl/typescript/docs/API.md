# API Reference: Two Sides of Dynamic Forms

**Frontend developers**: Use these APIs to consume field specs from your backend  
**Backend developers**: Use these types to generate field specs in your APIs

## Frontend Usage: Consuming Field Specs

### Quick Start for Frontend Teams

```typescript
import { FieldValidator, InputFieldSpec } from 'input-field-spec-ts';

// 1. Get field definition from YOUR backend API
const emailFieldSpec: InputFieldSpec = await fetch('/api/form-fields/email')
  .then(response => response.json());

// 2. Validate user input using backend-defined rules
const validator = new FieldValidator();
const result = validator.validate(userEmail, emailFieldSpec);

// 3. Handle validation results
if (!result.isValid) {
  // Show errors defined by your backend
  console.log(result.errors.map(e => e.message));
}
```

## Backend Usage: Generating Field Specs

### Quick Start for Backend Teams

```typescript
import { InputFieldSpec, ConstraintDescriptor } from 'input-field-spec-ts';

// Generate field specs in your API endpoints
app.get('/api/form-fields/email', (req, res) => {
  const emailFieldSpec: InputFieldSpec = {
    displayName: "Email Address",
    dataType: "STRING",
    expectMultipleValues: false,
    required: true,
    constraints: [
      { 
        name: "email", 
        pattern: "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$",
        errorMessage: "Please enter a valid email address" 
      },
      { 
        name: "maxLength", 
        max: 100,
        errorMessage: "Email too long (max 100 characters)"
      }
    ]
  };
  
  res.json(emailFieldSpec);
});
```

## Core Types

### InputFieldSpec

The heart of the protocol - represents a complete field specification that your backend sends to your frontend.

```typescript
interface InputFieldSpec {
  displayName: string;                  // What users see as field label
  description?: string;                 // Optional help text
  dataType: DataType;                   // 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN'
  expectMultipleValues: boolean;        // Array input or single value?
  required: boolean;                    // Is this field required?
  constraints: ConstraintDescriptor[];  // Validation rules (ordered execution)
  valuesEndpoint?: ValuesEndpoint;      // For autocomplete/dropdown data
}
```

**Real-world examples:**

```typescript
// Backend generates different email rules based on user type
function createEmailField(isPremiumUser: boolean): InputFieldSpec {
  const constraints: ConstraintDescriptor[] = [
    { name: "email", pattern: "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", errorMessage: "Invalid email format" }
  ];
  
  if (isPremiumUser) {
    constraints.push({ 
      name: "maxLength", 
      max: 200,
      errorMessage: "Email too long (premium: max 200 chars)" 
    });
  } else {
    constraints.push({ 
      name: "maxLength", 
      max: 50,
      errorMessage: "Email too long (basic: max 50 chars)" 
    });
  }

  return {
    displayName: "Email Address",
    dataType: "STRING",
    expectMultipleValues: false,
    required: true,
    constraints
  };
}
```
};
```

### ConstraintDescriptor

Describes a single validation constraint with execution order.

```typescript
interface ConstraintDescriptor {
  name: string;                    // ✨ v2.0: Required identifier for constraint
  description?: string;
  errorMessage?: string;
  defaultValue?: any;
  min?: number;
  max?: number;
  pattern?: string;
  format?: string;
  enumValues?: ValueAlias[];
  valuesEndpoint?: ValuesEndpoint;
}
```

**Properties:**
- `name` - Unique identifier for this constraint (v2.0: required)
- `description` - Human-readable explanation
- `errorMessage` - Custom error message
- `defaultValue` - Default value if not provided
- `min`/`max` - Context-dependent limits (length, value, array size)
- `pattern` - Regex pattern for STRING validation
- `format` - Format hint (email, url, uuid, iso8601)
- `enumValues` - Fixed set of allowed values
- `valuesEndpoint` - Dynamic values configuration

**Example:**
```typescript
const constraints: ConstraintDescriptor[] = [
  {
    name: 'length',
    min: 8,
    max: 50,
    errorMessage: 'Must be 8-50 characters'
  },
  {
    name: 'strength',
    pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)',
    errorMessage: 'Must contain lowercase, uppercase, and digit'
  }
];
```

### ValidationResult

Result of field validation with detailed error information.

```typescript
interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
}

interface ValidationError {
  constraintName: string;
  message: string;
  value?: any;
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
  protocol?: 'HTTP' | 'HTTPS' | 'GRPC';
  uri: string;
  method?: 'GET' | 'POST';
  searchField?: string;
  paginationStrategy?: PaginationStrategy;
  responseMapping: ResponseMapping;
  requestParams?: RequestParams;
  cacheStrategy?: CacheStrategy;
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
  constructor()
  
  async validate(
    fieldSpec: InputFieldSpec,
    value: any,
    constraintName?: string
  ): Promise<ValidationResult>
}
```

**Methods:**

#### `validate(fieldSpec, value, constraintName?)`

Validates a value against field specification constraints.

**Parameters:**
- `fieldSpec` - The field specification to validate against
- `value` - The value to validate
- `constraintName` - Optional specific constraint name to validate

**Returns:** `Promise<ValidationResult>`

**Behavior:**
- If `constraintName` provided: validates only that constraint
- If no `constraintName`: validates all constraints in array order
- Checks field-level `required` before constraint validation
- Returns first error encountered or success

**Examples:**
```typescript
const validator = new FieldValidator();

// Validate specific constraint
const result1 = await validator.validate(fieldSpec, 'test@example.com', 'email');

// Validate all constraints in order
const result2 = await validator.validate(fieldSpec, 'test@example.com');

// Check results
if (result1.isValid) {
  console.log('Email format is valid');
} else {
  console.log('Errors:', result1.errors.map(e => e.message));
}
```

### ValuesResolver

Orchestrates dynamic value resolution with caching and HTTP requests.

```typescript
class ValuesResolver {
  constructor(
    private httpClient: HttpClient,
    private cacheProvider: CacheProvider
  )
  
  async resolveValues(
    endpoint: ValuesEndpoint,
    params?: ResolveParams
  ): Promise<ValuesResult>
}
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
const resolver = new ValuesResolver(
  new FetchHttpClient(),
  new MemoryCacheProvider()
);

const endpoint = createDefaultValuesEndpoint('https://api.example.com/countries');
const result = await resolver.resolveValues(endpoint, { 
  search: 'france',
  page: 1,
  limit: 10 
});
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
    {
      name: 'length',
      min: 8,
      max: 128,
      errorMessage: 'Password must be 8-128 characters'
    },
    {
      name: 'strength',
      pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)',
      errorMessage: 'Must contain lowercase, uppercase, and digit'
    }
  ]
};

const validator = new FieldValidator();

// Validate step by step
const lengthResult = await validator.validate(passwordSpec, 'weak', 'length');
const strengthResult = await validator.validate(passwordSpec, 'StrongPass123', 'strength');

// Validate all constraints
const allResult = await validator.validate(passwordSpec, 'StrongPass123!');
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
    {
      name: 'arraySize',
      min: 1,
      max: 5,
      errorMessage: 'Select 1-5 skills'
    },
    {
      name: 'skillFormat',
      pattern: '^[a-zA-Z\\s]+$',
      errorMessage: 'Skills must contain only letters and spaces'
    }
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
    {
      name: 'format',
      pattern: '^[a-zA-Z0-9_]+$',
      errorMessage: 'Username can only contain letters, numbers, and underscores'
    },
    {
      name: 'length',
      min: 3,
      max: 20,
      errorMessage: 'Username must be between 3 and 20 characters'
    }
  ]
};
```

### Enum Values

```typescript
const statusSpec: InputFieldSpec = {
  displayName: 'Status',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,
  constraints: [
    {
      name: 'validStatus',
      enumValues: [
        { value: 'active', label: 'Active' },
        { value: 'inactive', label: 'Inactive' },
        { value: 'pending', label: 'Pending' }
      ],
      errorMessage: 'Please select a valid status'
    }
  ]
};
```

## Version 2.0 Migration

### Key Changes

1. **Required field moved to top-level**
2. **Constraints as ordered array**
3. **Named constraints with execution order**

### Migration Example

**Before (v1.x):**
```typescript
const oldSpec = {
  constraints: {
    email: { 
      required: true, 
      pattern: '...' 
    }
  }
};
```

**After (v2.0):**
```typescript
const newSpec = {
  required: true,  // Moved to top-level
  constraints: [   // Now an array
    { 
      name: 'email', 
      pattern: '...' 
    }
  ]
};
```

See [MIGRATION.md](./MIGRATION.md) for complete migration guide.

## TypeScript Support

All types are fully typed with TypeScript strict mode:

```typescript
// Full type inference
const spec: InputFieldSpec = { /* ... */ };
const validator = new FieldValidator();
const result = await validator.validate(spec, value);

// result.isValid is boolean
// result.errors is ValidationError[]
// Full IDE autocomplete and type checking
```

## Error Handling

```typescript
try {
  const result = await validator.validate(fieldSpec, value);
  
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
- **Lazy validation**: Only validates when called
- **Efficient caching**: Built-in memory cache with TTL
- **Minimal memory**: Uses native data structures (Map, Array)
- **Fast execution**: Optimized validation algorithms