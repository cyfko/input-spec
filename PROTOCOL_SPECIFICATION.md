# Dynamic Input Field Specification Protocol v2.0 (Breaking Change)

> THIS IS A MAJOR REVISION introducing a new constraint model, removal of `enumValues`, and relocation of `valuesEndpoint`.

## Quick Navigation

| Section | Objet |
|---------|-------|
| 1. Introduction | Objectifs & périmètre |
| 2. Core Entities | `InputFieldSpec`, `ValuesEndpoint`, `ConstraintDescriptor` |
| 2.5 Registry | Liste des types de contraintes atomiques |
| 2.6 Pipeline | Ordre normatif de validation |
| 3. Membership Semantics | Modes CLOSED vs SUGGESTIONS |
| 4. Error Model | Structure des erreurs de validation |
| 5. Migration Notes | Résumé v1 → v2 |
| 6. Extensibility | Ajout de nouveaux types |
| Appendix A | Legacy v1 (référence uniquement) |


## RFC 2119 Terminology

The key words MUST, MUST NOT, REQUIRED, SHALL, SHALL NOT, SHOULD, SHOULD NOT, RECOMMENDED, MAY, and OPTIONAL in this document are to be interpreted as described in RFC 2119.

---

## 1. Introduction

This protocol defines a **technology‑agnostic** structure to describe smart input fields, their validation semantics, and (optionally) dynamic or static value domains.

### 1.1 Goals

* Provide runtime field metadata (no hard‑coded forms)
* Express validation as an ordered, deterministic pipeline
* Support dynamic (remote / paginated / searchable) value sets
* Unify static enumerations and remote values under one mechanism
* Ensure cross‑language, cross‑framework interoperability
* Allow explicit extensibility without ambiguity

### 1.2 Non‑Goals
* Authentication / authorization strategy
* Transport security enforcement (HTTPS is a hint, not a guarantee)
* UI rendering specification (purely a data contract)
* Cross‑field relational constraints (future extension)

---

## 2. Core Entities (v2 Model)

### 2.1 InputFieldSpec

Represents a single logical input field.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `displayName` | string | ✓ | Human readable label |
| `description` | string |  | Human readable explanation / help text |
| `dataType` | string | ✓ | One of: `STRING`, `NUMBER`, `DATE`, `BOOLEAN` |
| `expectMultipleValues` | boolean | ✓ | Accepts an array (true) or single value (false) |
| `required` | boolean | ✓ | Field level required flag (applied before constraints) |
| `valuesEndpoint` | ValuesEndpoint |  | Defines a (possibly closed) value domain (static or remote) |
| `constraints` | ConstraintDescriptor[] | ✓ | Ordered list of **atomic** constraints |
| `formatHint` | string |  | Non-enforced formatting/display hint (moved from constraint type) |

**Key Semantics**
1. Validation order is strictly: required → type → (closed domain membership, if any) → ordered constraints.
2. If `valuesEndpoint.mode = CLOSED`, membership validation MUST occur and, if any element fails, subsequent scalar constraints MUST still run only if they *do not* redefine membership (i.e. pattern/minLength etc. still apply unless the implementation chooses to short‑circuit for performance). Implementations MAY short‑circuit after the first membership failure for UX performance.
3. If `valuesEndpoint.mode = SUGGESTIONS`, returned values are helpers only; membership MUST NOT cause failures. Scalar constraints proceed as if no closed domain.
4. `enumValues` (v1) is removed. Static enumerations MUST use `valuesEndpoint.protocol = "INLINE"`.
5. Each constraint is atomic: exactly one semantic unit per descriptor.

### 2.2 ValuesEndpoint

Unified representation for value sourcing.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `protocol` | string | ✓ | `INLINE` | `HTTPS` | `HTTP` | `GRPC` (default: `HTTPS` if omitted) |
| `mode` | string |  | `CLOSED` (default) or `SUGGESTIONS` |
| `items` | ValueAlias[] | conditional | Required iff `protocol = INLINE` |
| `uri` | string | conditional | Required if protocol is remote (`HTTPS`/`HTTP`/`GRPC`) |
| `method` | string |  | `GET` (default) or `POST` |
| `searchField` | string |  | Remote search field hint |
| `paginationStrategy` | string |  | `NONE` | `PAGE_NUMBER` (default: `NONE` if absent) |
| `responseMapping` | ResponseMapping |  | Where to extract data (required for non-INLINE if structure not root array) |
| `requestParams` | RequestParams |  | Names for query or body parameters |
| `cacheStrategy` | string |  | `NONE` | `SESSION` | `SHORT_TERM` | `LONG_TERM` |
| `debounceMs` | number |  | Client hint for search debounce |
| `minSearchLength` | number |  | Minimum characters before search (default 0) |

**Membership Semantics**
* `mode = CLOSED`: Value(s) MUST belong to the provided (static or fetched) set.
* `mode = SUGGESTIONS`: Value(s) MAY lie outside; no membership failure generated.

### 2.3 ValueAlias

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `value` | any | ✓ | Canonical value returned to server |
| `label` | string | ✓ | UI label (no automatic transformation) |

### 2.4 ConstraintDescriptor (Atomic)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | ✓ | Unique identifier within the field (stable for telemetry/UI) |
| `type` | string | ✓ | Constraint type discriminator (see registry) |
| `params` | any | ✓ | Type‑specific payload (structure depends on `type`) |
| `errorMessage` | string |  | Message used on failure (MAY be omitted) |
| `description` | string |  | Human description / UX hint |

**No legacy fields** like `min`, `max`, `pattern`, `enumValues` remain—migration required.

### 2.5 Constraint Type Registry (Initial Set)

| Type | params Shape | Applies To | Validation Rule |
|------|--------------|-----------|-----------------|
| `pattern` | `{ regex: string, flags?: string }` | STRING | Value MUST match regex |
| `minLength` | `{ value: number }` | STRING | length ≥ value |
| `maxLength` | `{ value: number }` | STRING | length ≤ value |
| `minValue` | `{ value: number }` | NUMBER | value ≥ number |
| `maxValue` | `{ value: number }` | NUMBER | value ≤ number |
| `minDate` | `{ iso: string }` | DATE | date ≥ iso timestamp |
| `maxDate` | `{ iso: string }` | DATE | date ≤ iso timestamp |
| `range` | `{ min: number|string, max: number|string, step?: number }` | NUMBER or DATE | Combined inclusive bounds (DATE uses ISO strings) |
| `custom` | `{ key: string, [extra: string]: any }` | ANY | Implementation-defined; MUST NOT break default validators |

> Future extensions MUST define param schema clearly. Unknown `type` values MUST be ignored (tolerant) or cause a controlled validation warning; they MUST NOT crash a generic validator.

### 2.6 Validation Pipeline (Normative)

Pseudocode:
```
function validate(fieldSpec, input): ValidationResult {
  // 1. REQUIRED
  if (fieldSpec.required && isEmpty(input)) return error('required');
  if (isEmpty(input)) return ok(); // optional & empty

  // 2. TYPE
  if (!matchesType(input, fieldSpec.dataType, fieldSpec.expectMultipleValues)) return error('type');

  // 3. CLOSED DOMAIN MEMBERSHIP
  if (fieldSpec.valuesEndpoint && fieldSpec.valuesEndpoint.mode !== 'SUGGESTIONS') {
     const domain = resolveDomain(fieldSpec.valuesEndpoint); // remote fetch or inline
     if (fieldSpec.expectMultipleValues) {
        collect membership errors per element (index)
     } else {
        membership check single value
     }
     // MAY short-circuit if membership fails
  }

  // 4. ORDERED CONSTRAINTS
  for each constraint in fieldSpec.constraints in array order:
     applyConstraint(constraint, input)
     (for arrays: apply per element and/or array length depending on constraint semantics)

  // 5. AGGREGATE
  return { isValid: errors.length == 0, errors }
}
```

### 2.7 Error Object (Standard Form)

```json
{
  "constraintName": "minLength",
  "message": "Minimum length is 3",
  "value": "ab",
  "index": 0 // present only for multi-value element-level errors
}
```

Multiple errors MAY share the same `constraintName` (e.g. multi-values). Clients MAY group them for display.

---

## 3. Examples (v2)

### 3.1 Static Enumeration (INLINE Closed Domain)
```json
{
  "displayName": "Status",
  "description": "Current lifecycle status",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "valuesEndpoint": {
    "protocol": "INLINE",
    "mode": "CLOSED",
    "items": [
      { "value": "ACTIVE", "label": "Active" },
      { "value": "INACTIVE", "label": "Inactive" },
      { "value": "PENDING", "label": "Pending" }
    ]
  },
  "constraints": [
    { "name": "patternId", "type": "pattern", "params": { "regex": "^[A-Z]+$" }, "errorMessage": "Must be uppercase letters" }
  ]
}
```

### 3.2 Number With Range & Min/Max Values
```json
{
  "displayName": "Temperature",
  "dataType": "NUMBER",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    { "name": "operationalRange", "type": "range", "params": { "min": 0, "max": 100 }, "errorMessage": "0–100" },
    { "name": "softMax", "type": "maxValue", "params": { "value": 95 }, "errorMessage": "Prefer ≤ 95" }
  ]
}
```

### 3.3 String With Length & Pattern
```json
{
  "displayName": "Username",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    { "name": "minL", "type": "minLength", "params": { "value": 3 }, "errorMessage": "At least 3 chars" },
    { "name": "maxL", "type": "maxLength", "params": { "value": 20 }, "errorMessage": "At most 20 chars" },
    { "name": "syntax", "type": "pattern", "params": { "regex": "^[a-zA-Z0-9_]+$" }, "errorMessage": "Alnum + underscore only" }
  ]
}
```

### 3.4 Multi-Select (Membership + Length)
```json
{
  "displayName": "Tags",
  "dataType": "STRING",
  "expectMultipleValues": true,
  "required": true,
  "valuesEndpoint": {
    "protocol": "HTTPS",
    "uri": "/api/tags",
    "paginationStrategy": "NONE",
    "mode": "CLOSED"
  },
  "constraints": [
    { "name": "minCount", "type": "minValue", "params": { "value": 1 }, "description": "(applies to array length)" },
    { "name": "maxCount", "type": "maxValue", "params": { "value": 10 } }
  ]
}
```

### 3.5 Date With Range and Format Hint
```json
{
  "displayName": "Created Date",
  "dataType": "DATE",
  "expectMultipleValues": false,
  "required": false,
  "formatHint": "iso8601",
  "constraints": [
    { "name": "after", "type": "minDate", "params": { "iso": "2024-01-01T00:00:00Z" } },
    { "name": "before", "type": "maxDate", "params": { "iso": "2025-12-31T23:59:59Z" } }
  ]
}
```

### 3.6 Suggestions (Non-Closed Domain)
```json
{
  "displayName": "Country",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": false,
  "valuesEndpoint": {
    "protocol": "HTTPS",
    "uri": "/api/countries",
    "mode": "SUGGESTIONS",
    "paginationStrategy": "PAGE_NUMBER",
    "responseMapping": { "dataField": "data" },
    "requestParams": { "pageParam": "page", "limitParam": "limit", "searchParam": "q", "defaultLimit": 50 }
  },
  "constraints": [
    { "name": "patternAlpha", "type": "pattern", "params": { "regex": "^[A-Za-z\s]+$" }, "errorMessage": "Letters only" }
  ]
}
```

---

## 4. Conformance Test Matrix (Extract)

| Scenario | Input | Expected |
|----------|-------|----------|
| Closed domain match | value in INLINE items | Valid |
| Closed domain miss | value not in items | 1 membership error |
| Suggestions miss | value not in suggestions | Valid (no membership error) |
| Pattern fail | string violates regex | pattern error |
| Range fail (number) | value outside min/max | range error |
| Multi-values partial | array includes 1 invalid | error with index for invalid element |
| MinLength + Pattern | order respected | first failing constraint MAY short-circuit |

---

## 5. Error Handling (v2)

Standard validation response (example):
```json
{
  "isValid": false,
  "errors": [
    { "constraintName": "membership", "message": "Value not allowed", "value": "XYZ" },
    { "constraintName": "patternAlpha", "message": "Letters only", "value": "123" }
  ]
}
```

Clients MAY aggregate errors by `constraintName` or index for display. Servers SHOULD preserve order for deterministic UX.

---

## 6. Migration (v1 → v2)

| v1 Concept | v1 Example | v2 Replacement |
|------------|-----------|----------------|
| `enumValues` | `"enumValues": [{"value":"A","label":"A"}]` | `valuesEndpoint.protocol = INLINE` + `items` |
| Mixed constraint (min+max+pattern) | single descriptor with multiple scalar fields | Multiple atomic descriptors (one per rule) |
| `valuesEndpoint` inside a constraint | embedded in constraint | Top-level `valuesEndpoint` |
| `pattern` / `min` / `max` fields | inline scalar fields | `constraints[].type` + `params` |
| `format` passive field | part of constraint or root | Field-level `formatHint` |
| Array length via `min`/`max` polymorphism | `min:1, max:10` | Use explicit constraints *OR* range / minValue/maxValue applied to length (implementation note) |

**Automated Migration Strategy (Suggested)**
1. Lift first encountered `valuesEndpoint` (or `enumValues` → create INLINE endpoint) to field level.
2. If multiple endpoints found → specification error (must be manually resolved).
3. For each legacy constraint descriptor:
   * Create a new atomic descriptor per scalar (pattern/min/max/etc.).
4. Replace `enumValues` entirely.
5. Move legacy `format` value (if present) to field-level `formatHint`.
6. Add version marker or serve both under content negotiation if dual support is required temporarily.

**Backward Compatibility**
Implementations MAY offer a transitional mode parsing both v1 & v2 until deprecation.

---

## 7. Security & Performance Notes
* Membership checks for large remote sets SHOULD be cached respecting `cacheStrategy`.
* `SUGGESTIONS` mode SHOULD be used for very large or open sets where closed membership is impractical.
* Clients SHOULD debounce search using `debounceMs` if provided.
* Servers MUST still re‑validate all constraints (never trust client‑side acceptance).

---

## 8. Versioning
Current protocol version: `2.0.0`.

**Breaking Changes Introduced in 2.0.0**
* Removal of `enumValues`.
* Relocation of `valuesEndpoint` to field level.
* Atomic constraint model (`type` + `params`).
* Introduction of `INLINE` protocol & `mode` (CLOSED / SUGGESTIONS).
* Range constraint type.

Additive future extensions (non‑breaking): new constraint types, new pagination strategies, new endpoint modes, hint fields.

---

## 9. Glossary
| Term | Definition |
|------|------------|
| Closed Domain | A finite authoritative set of allowed values (INLINE or resolved remote) |
| Suggestions | Non‑authoritative helper list; values outside remain valid |
| Atomic Constraint | Single semantic validation rule with its own descriptor |
| Membership | Validation step ensuring value ∈ domain (closed mode) |

---

## 10. Summary
The v2 protocol unifies dynamic and static domains, enforces deterministic atomic validation, and clarifies precedence while simplifying extensibility. Migration utilities SHOULD focus on systematic mechanical transformation of v1 descriptors into atomic v2 forms.

---

© 2025 input-spec – Protocol Specification v2.0


## Appendix A: Legacy v1 Specification (Deprecated)

> The following section preserves the original v1-era descriptive text for historical reference. New implementations MUST target the v2 model defined above. This appendix may be removed in a future revision.

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
| `displayName` | string | ✓ | Human-readable field label |
| `description` | string | | Detailed explanation of field purpose |
| `dataType` | string | ✓ | Data type: `STRING`, `NUMBER`, `DATE`, `BOOLEAN` |
| `expectMultipleValues` | boolean | ✓ | Whether field accepts array of values |
| `required` | boolean | ✓ | Whether this field is required (moved from constraints for better API design) |
| `constraints` | ConstraintDescriptor[] | ✓ | Array of constraints with ordered execution |

**Note on types:**
- `dataType` describes the **singleton element type** only
- If `expectMultipleValues` is `true`, the field works with arrays of this type
- **Constraints are executed in order**, allowing for logical sequencing of validation rules
- The `required` field has been moved to the top-level for better API ergonomics

**Example:**
```json
{
  "displayName": "Task Assignee",
  "description": "Select user(s) to assign task to",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "format",
      "description": "User identifier validation",
      "errorMessage": "Please select a valid user",
      "valuesEndpoint": {
        "protocol": "HTTPS",
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
  ]
}
```

### 2.2 ConstraintDescriptor

Describes a single constraint on parameter values.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | ✓ | Unique identifier for this constraint (used for validation ordering) |
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
- **Constraints are processed in array order**, enabling deterministic validation sequencing
- The `required` validation is now handled at the `InputFieldSpec` level, not per constraint

**Validation order:**
1. Check field-level `required` (if field is empty and `required=true` → error)
2. Type validation (implicit from `dataType`)
3. **Execute constraints in array order:**
   - For each constraint in the `constraints` array:
     - Apply `pattern` (if present)
     - Apply `min` and `max` (interpret based on context)
     - Apply `format` (semantic hint, optional strict validation)
     - Apply `enumValues` or `valuesEndpoint` (if present)

**Examples:**

**Text field with length constraint:**
```json
{
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "value",
      "min": 3,
      "max": 20,
      "pattern": "^[a-zA-Z0-9_]+$",
      "description": "Username (3-20 alphanumeric characters)",
      "errorMessage": "Username must be 3-20 characters, alphanumeric with underscores"
    }
  ]
}
```

**Numeric input with range:**
```json
{
  "dataType": "NUMBER",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "value",
      "min": 0,
      "max": 150,
      "description": "Age in years",
      "errorMessage": "Age must be between 0 and 150"
    }
  ]
}
```

**Multi-select with length constraint:**
```json
{
  "dataType": "STRING",
  "expectMultipleValues": true,
  "required": true,
  "constraints": [
    {
      "name": "value",
      "min": 1,
      "max": 10,
      "description": "Select 1 to 10 tags",
      "errorMessage": "You must select between 1 and 10 tags"
    }
  ]
}
```

**Date range:**
```json
{
  "dataType": "DATE",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "startDate",
      "format": "iso8601",
      "description": "Start date",
      "errorMessage": "Start date is required"
    }
  ]
}
```

**Static enum:**
```json
{
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "status",
      "errorMessage": "Please select a status",
      "enumValues": [
        { "value": "ACTIVE", "label": "Active" },
        { "value": "INACTIVE", "label": "Inactive" }
      ]
    }
  ]
}
```

**Remote values:**
```json
{
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": false,
  "constraints": [
    {
      "name": "assignee",
      "description": "Assigned user",
      "errorMessage": "Invalid user selected",
      "valuesEndpoint": {
        "protocol": "HTTPS",
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
  ]
}
```

**Pattern with format hint:**
```json
{
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "email",
      "pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      "format": "email",
      "description": "Valid email address",
      "errorMessage": "Please provide a valid email address"
    }
  ]
}
```

### 2.3 ValuesEndpoint

Configuration for fetching values dynamically from a remote source with search capabilities.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `protocol` | string | | Protocol indicator for client: `HTTPS`, `HTTP`, `GRPC` (default: `HTTPS`) |
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
  "displayName": "Username",
  "description": "User's unique identifier",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "value",
      "min": 3,
      "max": 20,
      "pattern": "^[a-zA-Z0-9_]+$",
      "description": "Username (3-20 alphanumeric characters)",
      "errorMessage": "Username must be 3-20 characters, alphanumeric with underscores"
    }
  ]
}
```

### Example 2: Numeric Range Input
```json
{
  "displayName": "Price",
  "description": "Price filter range",
  "dataType": "NUMBER",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "value",
      "min": 0,
      "description": "Price value",
      "errorMessage": "Price must be greater than 0",
      "defaultValue": 0
    }
  ]
}
```

### Example 3: Email Input with Pattern
```json
{
  "displayName": "Email Address",
  "description": "Contact email address",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "value",
      "pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      "format": "email",
      "description": "Valid email address",
      "errorMessage": "Please provide a valid email address"
    }
  ]
}
```

### Example 4: Static Select Field
```json
{
  "displayName": "Status",
  "description": "Filter by status",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "value",
      "description": "Item status",
      "errorMessage": "Please select a status",
      "enumValues": [
        { "value": "active", "label": "Active" },
        { "value": "inactive", "label": "Inactive" },
        { "value": "pending", "label": "Pending" }
      ]
    }
  ]
}
```

### Example 5: Searchable User Select with Pagination
```json
{
  "displayName": "Assigned To",
  "description": "Assign task to user",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "value",
      "description": "User to assign task to",
      "errorMessage": "Please select a user",
      "valuesEndpoint": {
        "protocol": "HTTPS",
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
  ]
}
```

### Example 6: Multi-Select Tags with Search
```json
{
  "displayName": "Tags",
  "description": "Select relevant tags for content",
  "dataType": "STRING",
  "expectMultipleValues": true,
  "required": true,
  "constraints": [
    {
      "name": "value",
      "min": 1,
      "max": 5,
      "description": "Select 1 to 5 relevant tags",
      "errorMessage": "You must select between 1 and 5 tags",
      "valuesEndpoint": {
        "protocol": "HTTPS",
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
  ]
}
```

### Example 7: Date Range Input
```json
{
  "displayName": "Created Date",
  "description": "Filter by creation date",
  "dataType": "DATE",
  "expectMultipleValues": false,
  "required": false,
  "constraints": [
    {
      "name": "value",
      "format": "iso8601",
      "description": "Creation date",
      "errorMessage": "Please provide a valid date"
    }
  ]
}
```

---

## 4. API Endpoints

### 4.1 Get All Input Field Specifications

**GET** `/api/fields`

**Query Parameters:**
- `dataType` (optional): Filter by data type (`STRING`, `NUMBER`, `DATE`, `BOOLEAN`)

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
   - Look through the `constraints` array for any constraint with `enumValues` → render dropdown/select
   - Look through the `constraints` array for any constraint with `valuesEndpoint` → fetch and render dropdown/select with pagination
   - Otherwise → render based on type and constraints

2. **Determine input type based on `dataType`:**
   - `STRING` → text input (validate with `pattern`, `min`/`max` for length from constraints)
   - `NUMBER` → number input (validate with `min`/`max` for value from constraints)
   - `BOOLEAN` → checkbox/toggle
   - `DATE` → date picker (validate with `min`/`max` for date range from constraints)

3. **Handle `expectMultipleValues`:**
   - If `true` and has value source → multi-select dropdown
   - If `true` and free input → add/remove input fields
   - Validate array length with `min`/`max` from constraints

4. **Apply validation in constraint order:**
   - Check field-level `required`: Show error if empty and required
   - Process each constraint in the `constraints` array sequentially:
     - `pattern`: Validate each element against regex
     - `min`/`max`: Context-dependent validation
     - `format`: Use as hint for input type and optional validation
     - `enumValues`/`valuesEndpoint`: Restrict to allowed values

5. **Display appropriate `errorMessage` when validation fails**

6. **Process constraints in order** to enable deterministic validation flow

**Validation Logic Example:**
```javascript
function validateField(value, fieldSpec) {
  const errors = [];

  // Check field-level required constraint
  if (fieldSpec.required && !value) {
    return ["This field is required"];
  }

  if (!value) return []; // Not required and empty = valid

  // For arrays
  if (fieldSpec.expectMultipleValues) {
    if (!Array.isArray(value)) {
      return ["Expected an array of values"];
    }
    
    // Process constraints in order
    for (const constraint of fieldSpec.constraints) {
      const error = validateArrayConstraint(value, constraint, fieldSpec.dataType);
      if (error) errors.push(error);
    }
    
    // Validate each element
    for (let i = 0; i < value.length; i++) {
      for (const constraint of fieldSpec.constraints) {
        const error = validateSingleValue(value[i], constraint, fieldSpec.dataType);
        if (error) errors.push(`${constraint.name}[${i}]: ${error}`);
      }
    }
    return errors;
  }

  // For single values - process constraints in order
  for (const constraint of fieldSpec.constraints) {
    const error = validateSingleValue(value, constraint, fieldSpec.dataType);
    if (error) errors.push(error);
  }
  
  return errors;
}

function validateArrayConstraint(array, constraint, dataType) {
  if (constraint.min && array.length < constraint.min) {
    return constraint.errorMessage || `Minimum ${constraint.min} items required`;
  }
  if (constraint.max && array.length > constraint.max) {
    return constraint.errorMessage || `Maximum ${constraint.max} items allowed`;
  }
  return null;
}

function validateSingleValue(value, constraint, dataType) {
  if (dataType === "STRING") {
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

  if (dataType === "NUMBER") {
    if (constraint.min !== undefined && value < constraint.min) {
      return constraint.errorMessage || `Minimum value is ${constraint.min}`;
    }
    if (constraint.max !== undefined && value > constraint.max) {
      return constraint.errorMessage || `Maximum value is ${constraint.max}`;
    }
  }

  if (dataType === "DATE") {
    const date = new Date(value);
    if (constraint.min && date < new Date(constraint.min)) {
      return constraint.errorMessage || `Date must be after ${constraint.min}`;
    }
    if (constraint.max && date > new Date(constraint.max)) {
      return constraint.errorMessage || `Date must be before ${constraint.max}`;
    }
  }

  // Check enum values
  if (constraint.enumValues) {
    const validValues = constraint.enumValues.map(ev => ev.value);
    if (!validValues.includes(value)) {
      return constraint.errorMessage || "Invalid value selected";
    }
  }

  return null; // Valid
}
```**Fetching Paginated Values:**
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

**Field Specification:**
1. Define fields with complete metadata
2. Provide clear `description` and `errorMessage` for each constraint
3. Set appropriate `dataType`
4. Define all constraints with proper validation rules

**Constraint Validation:**
1. Check all `required` constraints are satisfied
2. Validate types match declared `dataType`
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

**Field Design:**
- Provide complete, self-contained metadata
- Keep constraints simple and focused
- Use `dataType` precisely (one type per field)

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
- `INVALID_FIELD`: Unknown field name
- `CONSTRAINT_VIOLATION`: Constraint validation failed
- `VALUES_FETCH_ERROR`: Failed to fetch values from endpoint
- `MISSING_REQUIRED_CONSTRAINT`: Required constraint not provided
- `INVALID_CONSTRAINT_VALUE`: Constraint value has wrong type or format

---

## 7. Protocol Versioning

Protocol version: **2.0.0**

**Key Principles:**
- Clients discover field specifications dynamically
- No assumptions about field names or availability
- Metadata is self-contained and complete
- Backward compatibility through additive changes

**Breaking changes:**
- Removing required fields
- Changing field semantics fundamentally
- Incompatible response structure changes

**Non-breaking changes:**
- Adding optional fields
- Adding new field types
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

### Scenario: Dynamic field with paginated user selection

**Step 1:** Client requests field specification
```
GET /api/fields/assignee
```

**Step 2:** Server responds
```json
{
  "field": {
    "displayName": "Assigned To",
    "description": "Assign task to user",
    "dataType": "STRING",
    "expectMultipleValues": false,
    "required": true,
    "constraints": [
      {
        "name": "value",
        "errorMessage": "Please select a user",
        "valuesEndpoint": {
          "protocol": "HTTPS",
          "uri": "/api/users",
          "searchField": "name",
          "paginationStrategy": "PAGE_NUMBER",
          "responseMapping": {
            "dataField": "data",
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
    ]
  }
}
```

**Step 3:** Client fetches first page
```
GET /api/users?page=1&limit=50&search=john
```

**Step 4:** Server returns
```json
{
  "data": [
    { "value": "usr_123", "label": "John Doe" },
    { "value": "usr_456", "label": "John Smith" }
  ],
  "total": 2,
  "hasNext": false
}
```

**Step 5:** Client renders input field, user selects, loads more pages as needed

**Step 6:** User submits:
```json
{
  "field": "assignee",
  "value": "usr_123"
}
```

**Step 7:** Server validates and processes input

---

## Protocol Specification Scope

### What This Protocol Defines

This protocol specification focuses on **data structure and client guidance**:

- ✅ **Field metadata format** - How to describe input fields dynamically
- ✅ **Constraint specification** - Validation rules and their semantics  
- ✅ **Value source configuration** - How to indicate where clients can fetch data
- ✅ **Client protocol hints** - Suggesting `HTTPS`/`HTTP`/`GRPC` for data endpoints
- ✅ **Response structure** - Expected data format from value endpoints

### What This Protocol Does NOT Define

This protocol **does not handle**:

- ❌ **Transport security** - TLS/SSL configuration is implementation-specific
- ❌ **Authentication** - Client credentials and auth mechanisms  
- ❌ **Network infrastructure** - Load balancers, proxies, CDNs
- ❌ **Server implementation** - How endpoints actually fetch/process data

### Protocol Field Semantics

**`protocol` field**: Acts as a **client hint** indicating which communication protocol the endpoint expects. This is **metadata for the client application**, not a security enforcement mechanism.

- **`HTTPS`**: Suggests secure HTTP communication (recommended for production)
- **`HTTP`**: Suggests standard HTTP communication (typically development/internal)  
- **`GRPC`**: Suggests gRPC binary protocol communication

**Security responsibility**: The actual transport security (TLS certificates, encryption, authentication) is handled at the **application/infrastructure level**, not by this protocol specification.

---

## Security Considerations

### Protocol Security

**HTTPS by Default:** The protocol specification uses `HTTPS` as the default protocol hint for all `ValuesEndpoint` configurations. This encourages:

- ✅ **Modern security practices** in client implementations
- ✅ **Secure-by-default** endpoint configuration  
- ✅ **Best practice adoption** across different implementations
- ✅ **Clear intent** when using less secure protocols

**Protocol Selection Guidelines:**
- **`HTTPS`**: Recommended default for production environments
- **`HTTP`**: Should be explicit choice for development/testing or secure internal networks
- **`GRPC`**: For high-performance scenarios requiring binary protocol

### Implementation Security

When implementing this protocol:

1. **Validate all user inputs** on both client and server sides
2. **Sanitize search queries** to prevent injection attacks
3. **Implement proper authentication/authorization** for value endpoints
4. **Use rate limiting** to prevent abuse of search endpoints
5. **Validate constraint adherence** on the server before processing
6. **Honor protocol hints** but implement proper transport security independently

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
