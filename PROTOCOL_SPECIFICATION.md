# Dynamic Input Field Specification Protocol v2.0

## 1. Introduction

This protocol defines a **technology-agnostic method** to specify input field constraints, value sources, and validation rules dynamically.

**Goals:**
- Define input field specifications at runtime
- Understand value constraints and sources without hardcoding
- Enable smart form fields with auto-completion and validation
- Support searchable, paginated value selection
- Maintain cross-language interoperability

---

## 2. Core Entities

### 2.1 InputFieldSpec

Represents a smart input field with constraints and value sources.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `description` | string | | Detailed explanation of field purpose |
| `inputType` | string | ✓ | Input type: `TEXT`, `NUMBER`, `DATE`, `BOOLEAN`, `SELECT` |
| `dataType` | string | ✓ | Data type: `STRING`, `NUMBER`, `DATE`, `BOOLEAN` |
| `expectMultipleValues` | boolean | ✓ | Whether field accepts array of values |
| `constraints` | map<string, ConstraintDescriptor> | ✓ | Named constraints describing expected values |

**Note on types:** 
- `dataType` describes the **singleton element type** only
- If `expectMultipleValues` is `true`, the field works with arrays of this type
- `inputType` hints at the UI component to use (`SELECT` for dropdowns, `TEXT` for text inputs, etc.)

**Example:**
```json
{
  "description": "Select user(s) to assign task to",
  "inputType": "SELECT",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "constraints": {
    "value": {
      "required": true,
      "description": "User identifier",
      "errorMessage": "Please select a user",
      "valuesEndpoint": {
        "protocol": "HTTP",
        "uri": "/api/users",
        "searchField": "name",
        "paginationStrategy": "PAGE_NUMBER",
        "responseMapping": {
          "dataField": "data"
        },
        "requestParams": {
          "pageParam": "page",
          "limitParam": "limit",
          "searchParam": "search",
          "defaultLimit": 50
        }
      }
    }
  }
}
```

### 2.2 ConstraintDescriptor

Describes a single constraint on parameter values.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `required` | boolean | ✓ | Whether constraint must be satisfied |
| `description` | string | | Human-readable explanation |
| `errorMessage` | string | | Error message if constraint not satisfied |
| `defaultValue` | any | | Default value if not provided |
| `min` | number | | Context-dependent minimum (see below) |
| `max` | number | | Context-dependent maximum (see below) |
| `pattern` | string | | Regex pattern (STRING only, applies per element) |
| `format` | string | | Format hint (e.g., `email`, `url`, `uuid`, `iso8601`) - applies per element |
| `enumValues` | ValueAlias[] | | Fixed set of allowed values |
| `valuesEndpoint` | ValuesEndpoint | | Configuration for fetching values dynamically |

**Context-dependent `min` and `max`:**

The semantics of `min` and `max` depend on the field's `dataType` and `expectMultipleValues`:

| dataType | expectMultipleValues | `min` / `max` meaning |
|----------|----------------------|----------------------|
| `STRING` | false | Minimum/maximum **character count** of the string |
| `STRING` | true | Minimum/maximum **number of elements** in the array |
| `NUMBER` | false | Minimum/maximum **numeric value** |
| `NUMBER` | true | Minimum/maximum **number of elements** in the array |
| `DATE` | false | Minimum/maximum **date value** (ISO 8601) |
| `DATE` | true | Minimum/maximum **number of elements** in the array |
| `BOOLEAN` | true | Minimum/maximum **number of elements** in the array |

**Important notes:**
- `pattern` and `format` apply **per element** (whether singleton or in array)
- When `expectMultipleValues` is `true`, `min`/`max` constrain the **array length**
- All constraints present must be satisfied (logical AND)

**Validation order:**
1. Check `required` (if absent and `required=true` → error)
2. Type validation (implicit from `applicableToType`)
3. `pattern` (if present)
4. `min` and `max` (interpret based on context)
5. `format` (semantic hint, optional strict validation)
6. `enumValues` or `valuesEndpoint` (if present)

**Examples:**

**Text field with length constraint:**
```json
{
  "dataType": "STRING",
  "expectMultipleValues": false,
  "constraints": {
    "value": {
      "required": true,
      "min": 3,
      "max": 20,
      "pattern": "^[a-zA-Z0-9_]+$",
      "description": "Username (3-20 alphanumeric characters)",
      "errorMessage": "Username must be 3-20 characters, alphanumeric with underscores"
    }
  }
}
```

**Numeric input with range:**
```json
{
  "dataType": "NUMBER",
  "expectMultipleValues": false,
  "constraints": {
    "value": {
      "required": true,
      "min": 0,
      "max": 150,
      "description": "Age in years",
      "errorMessage": "Age must be between 0 and 150"
    }
  }
}
```

**Multi-select with length constraint:**
```json
{
  "dataType": "STRING",
  "expectMultipleValues": true,
  "constraints": {
    "value": {
      "required": true,
      "min": 1,
      "max": 10,
      "description": "Select 1 to 10 tags",
      "errorMessage": "You must select between 1 and 10 tags"
    }
  }
}
```

**Date range:**
```json
{
  "applicableToType": "DATE",
  "expectMultipleValues": false,
  "constraints": {
    "startDate": {
      "required": true,
      "format": "iso8601",
      "description": "Start date",
      "errorMessage": "Start date is required"
    }
  }
}
```

**Static enum:**
```json
{
  "applicableToType": "STRING",
  "expectMultipleValues": false,
  "constraints": {
    "status": {
      "required": true,
      "errorMessage": "Please select a status",
      "enumValues": [
        { "value": "ACTIVE", "label": "Active" },
        { "value": "INACTIVE", "label": "Inactive" }
      ]
    }
  }
}
```

**Remote values:**
```json
{
  "applicableToType": "STRING",
  "expectMultipleValues": false,
  "constraints": {
    "assignee": {
      "required": false,
      "description": "Assigned user",
      "errorMessage": "Invalid user selected",
      "valuesEndpoint": {
        "protocol": "HTTP",
        "uri": "/api/users",
        "paginationStrategy": "PAGE_NUMBER",
        "responseMapping": {
          "dataField": "data"
        },
        "requestParams": {
          "pageParam": "page",
          "limitParam": "limit",
          "defaultLimit": 50
        }
      }
    }
  }
}
```

**Pattern with format hint:**
```json
{
  "applicableToType": "STRING",
  "expectMultipleValues": false,
  "constraints": {
    "email": {
      "required": true,
      "pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      "format": "email",
      "description": "Valid email address",
      "errorMessage": "Please provide a valid email address"
    }
  }
}
```

### 2.3 ValuesEndpoint

Configuration for fetching values dynamically from a remote source with search capabilities.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `protocol` | string | | Protocol to use: `HTTP`, `HTTPS`, `GRPC` (default: `HTTP`) |
| `uri` | string | ✓ | Endpoint path or full URL |
| `method` | string | | HTTP method: `GET`, `POST` (default: `GET`) |
| `searchField` | string | | Server-side field to search/filter on |
| `paginationStrategy` | string | | `PAGE_NUMBER`, `NONE` |
| `responseMapping` | ResponseMapping | ✓ | Where to find data in the response |
| `requestParams` | RequestParams | | How to send pagination and search parameters |
| `cacheStrategy` | string | | `NONE`, `SESSION`, `SHORT_TERM` (5min), `LONG_TERM` (1h) |
| `debounceMs` | integer | | Milliseconds to wait before sending search request (default: 300) |
| `minSearchLength` | integer | | Minimum characters required before triggering search (default: 0) |

**Pagination Strategies:**

| Strategy | Description | Use Case |
|----------|-------------|----------|
| `NONE` | No pagination, returns all values | Small, static datasets (< 100 items) |
| `PAGE_NUMBER` | Page-based (page 1, 2, 3...) | Traditional pagination |

### 2.4 ResponseMapping

Describes where to find information in the endpoint response.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `dataField` | string | ✓ | Field containing the array of `ValueAlias` |
| `pageField` | string | | Field containing current page number |
| `pageSizeField` | string | | Field containing number of items in this page |
| `totalField` | string | | Field containing total count across all pages |
| `hasNextField` | string | | Field indicating if there's a next page (boolean) |

**Note:** If `dataField` is absent, the root response is assumed to be the array of `ValueAlias`.

### 2.5 RequestParams

Describes how to send pagination and search parameters in the request.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pageParam` | string | conditional | Parameter name for page number (required for `PAGE_NUMBER`) |
| `limitParam` | string | | Parameter name for page size |
| `searchParam` | string | | Parameter name for search query (e.g., "search", "q", "filter") |
| `defaultLimit` | integer | | Default page size if not specified |

### 2.6 ValueAlias

Represents a single value option.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `value` | any | ✓ | Actual value to send back to server (used as-is) |
| `label` | string | ✓ | Display text (shown to user without transformation) |

**Important:** 
- `value` is returned to server **exactly as received** (no transformation)
- `label` is displayed to user **without any transformation**

---

## 3. Complete Examples

### Example 1: Simple Text Input
```json
{
  "description": "User's unique identifier",
  "inputType": "TEXT",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "constraints": {
    "value": {
      "required": true,
      "min": 3,
      "max": 20,
      "pattern": "^[a-zA-Z0-9_]+$",
      "description": "Username (3-20 alphanumeric characters)",
      "errorMessage": "Username must be 3-20 characters, alphanumeric with underscores"
    }
  }
}
```

### Example 2: Numeric Range Input
```json
{
  "description": "Price filter range",
  "inputType": "NUMBER",
  "dataType": "NUMBER",
  "expectMultipleValues": false,
  "constraints": {
    "value": {
      "required": true,
      "min": 0,
      "description": "Price value",
      "errorMessage": "Price must be greater than 0",
      "defaultValue": 0
    }
  }
}
```

### Example 3: Email Input with Pattern
```json
{
  "description": "Contact email address",
  "inputType": "TEXT",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "constraints": {
    "value": {
      "required": true,
      "pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      "format": "email",
      "description": "Valid email address",
      "errorMessage": "Please provide a valid email address"
    }
  }
}
```

### Example 4: Static Select Field
```json
{
  "description": "Filter by status",
  "inputType": "SELECT",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "constraints": {
    "value": {
      "required": true,
      "description": "Item status",
      "errorMessage": "Please select a status",
      "enumValues": [
        { "value": "active", "label": "Active" },
        { "value": "inactive", "label": "Inactive" },
        { "value": "pending", "label": "Pending" }
      ]
    }
  }
}
```

### Example 5: Searchable User Select with Pagination
```json
{
  "description": "Assign task to user",
  "inputType": "SELECT",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "constraints": {
    "value": {
      "required": true,
      "description": "User to assign task to",
      "errorMessage": "Please select a user",
      "valuesEndpoint": {
        "protocol": "HTTP",
        "uri": "/api/users",
        "method": "GET",
        "searchField": "name",
        "paginationStrategy": "PAGE_NUMBER",
        "cacheStrategy": "SHORT_TERM",
        "debounceMs": 300,
        "minSearchLength": 2,
        "responseMapping": {
          "dataField": "data",
          "pageField": "page",
          "pageSizeField": "pageSize",
          "totalField": "total",
          "hasNextField": "hasNext"
        },
        "requestParams": {
          "pageParam": "page",
          "limitParam": "limit",
          "searchParam": "search",
          "defaultLimit": 50
        }
      }
    }
  }
}
```

### Example 6: Multi-Select Tags with Search
```json
{
  "description": "Select relevant tags for content",
  "inputType": "SELECT",
  "dataType": "STRING",
  "expectMultipleValues": true,
  "constraints": {
    "value": {
      "required": true,
      "min": 1,
      "max": 5,
      "description": "Select 1 to 5 relevant tags",
      "errorMessage": "You must select between 1 and 5 tags",
      "valuesEndpoint": {
        "protocol": "HTTP",
        "uri": "/api/tags",
        "searchField": "name",
        "paginationStrategy": "NONE",
        "cacheStrategy": "LONG_TERM",
        "debounceMs": 200,
        "minSearchLength": 1,
        "responseMapping": {
          "dataField": "tags"
        },
        "requestParams": {
          "searchParam": "q"
        }
      }
    }
  }
}
```

### Example 7: Date Range Input
```json
{
  "description": "Filter by creation date",
  "inputType": "DATE",
  "dataType": "DATE",
  "expectMultipleValues": false,
  "constraints": {
    "value": {
      "required": false,
      "format": "iso8601",
      "description": "Creation date",
      "errorMessage": "Please provide a valid date"
    }
  }
}
```

---

## 4. API Endpoints

### 4.1 Get All Input Field Specifications

**GET** `/api/fields`

**Query Parameters:**
- `dataType` (optional): Filter by data type (`STRING`, `NUMBER`, `DATE`, `BOOLEAN`)
- `inputType` (optional): Filter by input type (`TEXT`, `SELECT`, `NUMBER`, `DATE`)

**Response:**
```json
{
  "fields": [InputFieldSpec],
  "version": "2.0"
}
```

### 4.2 Get Input Field Specification

**GET** `/api/fields/{fieldName}`

**Response:**
```json
{
  "field": InputFieldSpec
}
```

### 4.3 Fetch Values from Endpoint

**Request:** As configured in `ValuesEndpoint`

**Examples:**

**Page-based with search:**
```
GET /api/users?page=1&limit=50&search=john
```

**Response:**
```json
{
  "data": [
    { "value": "usr_123", "label": "John Doe" },
    { "value": "usr_456", "label": "John Smith" }
  ],
  "page": 1,
  "pageSize": 50,
  "total": 2,
  "hasNext": false
}
```

**No pagination with search:**
```
GET /api/tags?q=java
```

**Response (direct array):**
```json
[
  { "value": "javascript", "label": "JavaScript" },
  { "value": "java", "label": "Java" }
]
```

---

## 5. Implementation Guidelines

### 5.1 Client-Side Behavior

**Rendering UI Controls:**

1. **Check for value sources:**
   - If `enumValues` present → render dropdown/select
   - If `valuesEndpoint` present → fetch and render dropdown/select with pagination
   - Otherwise → render based on type and constraints

2. **Determine input type based on `applicableToType`:**
   - `STRING` → text input (validate with `pattern`, `min`/`max` for length)
   - `NUMBER` → number input (validate with `min`/`max` for value)
   - `BOOLEAN` → checkbox/toggle
   - `DATE` → date picker (validate with `min`/`max` for date range)

3. **Handle `expectMultipleValues`:**
   - If `true` and has value source → multi-select dropdown
   - If `true` and free input → add/remove input fields
   - Validate array length with `min`/`max`

4. **Apply validation:**
   - `required`: Show error if empty and required
   - `pattern`: Validate each element against regex
   - `min`/`max`: Context-dependent validation
   - `format`: Use as hint for input type and optional validation

5. **Display `errorMessage` when validation fails**

**Validation Logic Example:**
```javascript
function validateConstraint(value, constraint, applicableToType, expectMultiple) {
  // Check required
  if (constraint.required && !value) {
    return constraint.errorMessage || "This field is required";
  }
  
  if (!value) return null; // Not required and empty = valid
  
  // For arrays
  if (expectMultiple) {
    if (constraint.min && value.length < constraint.min) {
      return constraint.errorMessage || `Minimum ${constraint.min} items required`;
    }
    if (constraint.max && value.length > constraint.max) {
      return constraint.errorMessage || `Maximum ${constraint.max} items allowed`;
    }
    // Validate each element
    for (const item of value) {
      const error = validateSingleValue(item, constraint, applicableToType);
      if (error) return error;
    }
    return null;
  }
  
  // For single values
  return validateSingleValue(value, constraint, applicableToType);
}

function validateSingleValue(value, constraint, applicableToType) {
  if (applicableToType === "STRING") {
    if (constraint.pattern && !new RegExp(constraint.pattern).test(value)) {
      return constraint.errorMessage || "Invalid format";
    }
    if (constraint.min && value.length < constraint.min) {
      return constraint.errorMessage || `Minimum ${constraint.min} characters`;
    }
    if (constraint.max && value.length > constraint.max) {
      return constraint.errorMessage || `Maximum ${constraint.max} characters`;
    }
  }
  
  if (applicableToType === "NUMBER") {
    if (constraint.min !== undefined && value < constraint.min) {
      return constraint.errorMessage || `Minimum value is ${constraint.min}`;
    }
    if (constraint.max !== undefined && value > constraint.max) {
      return constraint.errorMessage || `Maximum value is ${constraint.max}`;
    }
  }
  
  if (applicableToType === "DATE") {
    const date = new Date(value);
    if (constraint.min && date < new Date(constraint.min)) {
      return constraint.errorMessage || `Date must be after ${constraint.min}`;
    }
    if (constraint.max && date > new Date(constraint.max)) {
      return constraint.errorMessage || `Date must be before ${constraint.max}`;
    }
  }
  
  return null; // Valid
}
```

**Fetching Paginated Values:**
```javascript
async function fetchValues(valuesEndpoint, page = 1) {
  const { uri, method, paginationStrategy, requestParams, responseMapping } = valuesEndpoint;
  
  let url = uri;
  const params = new URLSearchParams();
  
  if (paginationStrategy === "PAGE_NUMBER") {
    params.set(requestParams.pageParam, page);
    params.set(requestParams.limitParam, requestParams.defaultLimit);
  }
  
  url += `?${params.toString()}`;
  
  const response = await fetch(url, { method });
  const data = await response.json();
  
  // Extract values based on mapping
  const values = responseMapping.dataField 
    ? data[responseMapping.dataField]
    : data;
  
  return {
    values,
    hasNext: responseMapping.hasNextField ? data[responseMapping.hasNextField] : false,
    total: responseMapping.totalField ? data[responseMapping.totalField] : null
  };
}
```

### 5.2 Server-Side Behavior

**Operator Registration:**
1. Define operators with complete metadata
2. Ensure `id` is unique and stable
3. Provide clear `description` and `errorMessage` for each constraint
4. Set appropriate `applicableToType`
5. Define all constraints with proper validation rules

**Constraint Validation:**
1. Check all `required` constraints are satisfied
2. Validate types match declared `applicableToType`
3. Apply context-dependent `min`/`max` validation
4. Validate `pattern` for strings
5. Return appropriate `errorMessage` when validation fails

**Values Endpoint Implementation:**
1. Implement endpoints referenced in `valuesEndpoint.uri`
2. Support declared `paginationStrategy`
3. Return response matching `responseMapping` structure
4. Handle pagination parameters from `requestParams`
5. Keep responses performant (caching, indexing)

### 5.3 Best Practices

**Operator Design:**
- Use meaningful, stable `id` strings
- Never assume client knowledge of specific operators
- Provide complete, self-contained metadata
- Keep constraints simple and focused
- Use `applicableToType` precisely (one type per operator)

**Constraint Design:**
- Use clear, descriptive constraint keys
- Set realistic `defaultValue` when applicable
- Provide user-friendly `errorMessage` for each constraint
- Use `description` to explain expected input
- Apply appropriate validation rules

**Error Messages:**
- Be specific about what went wrong
- Suggest corrective action when possible
- Keep messages user-friendly, not technical
- Example: ❌ "Constraint violation" → ✅ "Username must be 3-20 characters"

**Values Management:**
- Use `enumValues` for small, static lists (< 20 items)
- Use `valuesEndpoint` with `NONE` pagination for medium lists (< 100 items)
- Use paginated `valuesEndpoint` for large or dynamic datasets
- Keep `label` user-friendly and localized
- Return `value` in the expected backend format

**Pagination Strategy Selection:**
- `NONE`: Static or small datasets
- `PAGE_NUMBER`: User-facing pagination (e.g., "Page 1 of 10")

---

## 6. Error Handling

**Validation Error Response:**
```json
{
  "error": {
    "code": "CONSTRAINT_VIOLATION",
    "message": "One or more constraints failed validation",
    "violations": [
      {
        "constraint": "max",
        "message": "Maximum value must be greater than minimum"
      }
    ]
  }
}
```

**Error Codes:**
- `INVALID_PROPERTY`: Unknown property name
- `UNSUPPORTED_OPERATOR`: Operator not applicable to property type
- `CONSTRAINT_VIOLATION`: Constraint validation failed
- `VALUES_FETCH_ERROR`: Failed to fetch values from endpoint
- `MISSING_REQUIRED_CONSTRAINT`: Required constraint not provided
- `INVALID_CONSTRAINT_VALUE`: Constraint value has wrong type or format

---

## 7. Protocol Versioning

Protocol version: **2.0.0**

**Key Principles:**
- Clients discover operators dynamically
- No assumptions about operator IDs or availability
- Metadata is self-contained and complete
- Backward compatibility through additive changes

**Breaking changes:**
- Removing required fields
- Changing field semantics fundamentally
- Incompatible response structure changes

**Non-breaking changes:**
- Adding optional fields
- Adding new operators
- Extending validation rules
- Adding new pagination strategies

---

## 8. Type System Reference

### Applicable Types

| Type | Description | `min`/`max` when single | `min`/`max` when multiple |
|------|-------------|-------------------------|---------------------------|
| `STRING` | Text value | Character count | Array length |
| `NUMBER` | Numeric value | Numeric value | Array length |
| `DATE` | ISO 8601 date/datetime | Date value | Array length |
| `BOOLEAN` | True/false | N/A | Array length |

---

## 9. Complete Usage Flow

### Scenario: Dynamic filter with paginated user selection

**Step 1:** Client requests operators
```
GET /api/operators/property/assignee
```

**Step 2:** Server responds
```json
{
  "property": "assignee",
  "propertyType": "STRING",
  "operators": [
    {
      "id": "assigned_to",
      "displayName": "Assigned to",
      "applicableToType": "STRING",
      "expectMultipleValues": false,
      "constraints": {
        "userId": {
          "required": true,
          "errorMessage": "Please select a user",
          "valuesEndpoint": {
            "protocol": "HTTP",
            "uri": "/api/users",
            "paginationStrategy": "PAGE_NUMBER",
            "responseMapping": {
              "dataField": "data",
              "totalField": "total",
              "hasNextField": "hasNext"
            },
            "requestParams": {
              "pageParam": "page",
              "limitParam": "limit",
              "defaultLimit": 50
            }
          }
        }
      }
    }
  ]
}
```

**Step 3:** Client fetches first page
```
GET /api/users?page=1&limit=50
```

**Step 4:** Server returns
```json
{
  "data": [
    { "value": "usr_123", "label": "John Doe" },
    { "value": "usr_456", "label": "Jane Smith" }
  ],
  "total": 150,
  "hasNext": true
}
```

**Step 5:** Client renders dropdown, user selects, loads more pages as needed

**Step 6:** User submits:
```json
{
  "property": "assignee",
  "operator": "assigned_to",
  "constraints": {
    "userId": "usr_123"
  }
}
```

**Step 7:** Server validates and applies filter

---

## Summary

This protocol provides:
- ✅ **Smart input field specifications** - complete metadata for form fields
- ✅ **Context-dependent constraints** - `min`/`max` adapt to type and multiplicity  
- ✅ **Searchable value sources** - auto-completion with server-side filtering
- ✅ **Comprehensive pagination** - multiple strategies with search support
- ✅ **Performance optimization** - caching strategies and debouncing
- ✅ **Clear validation** - all rules in constraint descriptor
- ✅ **User-friendly errors** - specific messages per constraint
- ✅ **Flexible value sources** - static enums, searchable remote endpoints
- ✅ **Self-contained metadata** - everything needed in one response
- ✅ **Technology agnostic** - works with any frontend/backend combination