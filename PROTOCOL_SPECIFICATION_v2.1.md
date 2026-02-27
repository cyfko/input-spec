
# Dynamic Input Field Specification Protocol v2.1 (DRAFT)

> THIS IS THE NORMATIVE SPECIFICATION FOR VERSION 2.1. This document is self-contained.


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

---

## 2. Core Entities (v2 Model)

### 2.1 InputFieldSpec

Represents a single logical input field.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `displayName` | string | ✓ | Human readable label |
| `description` | string |  | Human readable explanation / help text |
| `dataType` | string | ✓ | One of: `STRING`, `NUMBER`, `DATE`, `BOOLEAN`, `OBJECT` |
| `expectMultipleValues` | boolean | ✓ | Accepts an array (true) or single value (false) |
| `required` | boolean | ✓ | Field level required flag (applied before constraints) |
| `valuesEndpoint` | ValuesEndpoint |  | Defines a (possibly closed) value domain (static or remote) |
| `constraints` | ConstraintDescriptor[] | ✓ | Ordered list of **atomic** constraints |
| `formatHint` | string |  | Non-enforced formatting/display hint (moved from constraint type) |
| `subFields` | InputFieldSpec[] | conditional | Required if `dataType = OBJECT`. Defines the nested fields of the object. Supports full recursion. |

**Key Semantics**
1. Validation order is strictly: required → type → (closed domain membership, if any) → ordered constraints.
2. If `valuesEndpoint.mode = CLOSED`, membership validation MUST occur and, if any element fails, subsequent scalar constraints MUST still run only if they *do not* redefine membership (i.e. pattern/minLength etc. still apply unless the implementation chooses to short‑circuit for performance). Implementations MAY short‑circuit after the first membership failure for UX performance.
3. If `valuesEndpoint.mode = SUGGESTIONS`, returned values are helpers only; membership MUST NOT cause failures. Scalar constraints proceed as if no closed domain.
4. Static enumerations MUST use `valuesEndpoint.protocol = "INLINE"` with `items`.
5. Each constraint is atomic: exactly one semantic unit per descriptor.
6. If `dataType = OBJECT`, the `subFields` array MUST be present and non-empty. Each sub-field is a fully self-contained `InputFieldSpec` processed recursively. `constraints` on the parent OBJECT field apply to the object as a whole (e.g. `custom` validators); scalar constraints (`minLength`, `minValue`, etc.) MUST NOT be used on `OBJECT` fields.
7. When `dataType = OBJECT` and `expectMultipleValues = true`, the value is an array of objects, each validated against `subFields` independently.


### 2.2 ValuesEndpoint

Unified representation for value sourcing.

| Field                | Type                  | Required    | Description                                                                                 |
|----------------------|-----------------------|-------------|---------------------------------------------------------------------------------------------|
| `protocol`           | string                | ✓           | `INLINE` \| `HTTPS` \| `HTTP` \| `GRPC` (default: `HTTPS` if omitted)                       |
| `mode`               | string                |             | `CLOSED` (default) or `SUGGESTIONS`                                                        |
| `items`              | ValueAlias[]          | conditional | Required iff `protocol = INLINE`                                                            |
| `uri`                | string                | conditional | Required if protocol is remote (`HTTPS`/`HTTP`/`GRPC`)                                      |
| `method`             | string                |             | `GET` (default) or `POST`                                                                   |
| `searchParams`       | object                |             | Key-value pairs for advanced search/filtering. Used as query params (GET) or body (POST).   |
| `searchParamsSchema` | object (JSON Schema)  |             | JSON Schema describing the structure, type, and semantics of each search parameter.         |
| `paginationStrategy` | string                |             | `NONE` \| `PAGE_NUMBER` (default: `NONE` if absent)                                         |
| `responseMapping`    | ResponseMapping       |             | Where to extract data (required for non-INLINE if structure not root array)                 |
| `requestParams`      | RequestParams         |             | Names for query or body parameters                                                          |
| `cacheStrategy`      | string                |             | `NONE` \| `SESSION` \| `SHORT_TERM` \| `LONG_TERM`                                          |
| `debounceMs`         | number                |             | Client hint for search debounce                                                             |
| `minSearchLength`    | number                |             | Minimum characters before search (default 0)                                                |

**Membership Semantics**
* `mode = CLOSED`: Value(s) MUST belong to the provided (static or fetched) set.
* `mode = SUGGESTIONS`: Value(s) MAY lie outside; no membership failure generated.

**Notes:**
- `searchParams` allows for multi-criteria and structured search/filtering.
- `searchParamsSchema` enables clients (including AI agents) to understand, validate, and document the expected search parameters.

**Example: Advanced Search Parameters**
```json
{
  "protocol": "HTTPS",
  "uri": "/api/items",
  "method": "POST",
  "searchParams": { "name": "foo", "status": "active" },
  "searchParamsSchema": {
    "type": "object",
    "properties": {
      "name": {
        "type": "string",
        "description": "Nom de l’item à rechercher (recherche partielle autorisée)"
      },
      "status": {
        "type": "string",
        "description": "Statut de l’item (ex: active, archived, pending)",
        "enum": ["active", "archived", "pending"]
      }
    },
    "required": ["name"]
  },
  "paginationStrategy": "PAGE_NUMBER",
  "responseMapping": { "dataField": "data" }
}
```

### 2.3 ValueAlias

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `value` | any | ✓ | Canonical value returned to server |
| `label` | LocalizedString | ✓ | UI label (no automatic transformation) |

### 2.4 LocalizedString

A `LocalizedString` is the normative type for **all human-readable string fields** in this protocol: `displayName`, `description`, `errorMessage`, `label`, `formatHint`.

It is polymorphic and MUST be one of:

| Form | Description |
|------|-------------|
| `string` | Single locale value. The server SHOULD resolve it according to the `Accept-Language` request header. |
| `{ [BCP-47]: string, "default"?: string }` | Inline multi-locale map. Keys MUST be valid BCP-47 locale tags (e.g. `"fr"`, `"en-US"`). A `"default"` key SHOULD be present as fallback. |

**Resolution Rules (normative)**
1. Clients SHOULD send an `Accept-Language` header on all specification requests.
2. When the server returns a plain `string`, the client MUST treat it as the resolved value and MUST NOT attempt to parse it as a locale map.
3. When the server returns a locale map, the client MUST select the best-matching locale using BCP-47 lookup. If no match, it MUST fall back to `"default"`, then to the first key in declaration order.
4. Servers MAY return either form. Mixing forms across fields in a single `InputFieldSpec` is ALLOWED.

**Examples:**
```json
"displayName": "Status"

"displayName": { "default": "Status", "fr": "Statut", "en": "Status", "de": "Status" }

"errorMessage": { "default": "Required field", "fr": "Champ obligatoire" }
```

> **AI agent note:** When consuming a locale map, agents SHOULD select the locale that best matches the user's context. When the field is used for machine-to-machine processing (no end-user), agents MAY use `"default"` unconditionally.

---

### 2.5 ConstraintDescriptor (Atomic)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | ✓ | Unique identifier within the field (stable for telemetry/UI) |
| `type` | string | ✓ | Constraint type discriminator (see registry) |
| `params` | any | ✓ | Type‑specific payload (structure depends on `type`) |
| `errorMessage` | LocalizedString |  | Message used on failure (MAY be omitted) |
| `description` | LocalizedString |  | Human description / UX hint |

Each constraint is self-contained: all configuration lives in `params`, keyed by the constraint `type`.

### 2.6 Constraint Type Registry (Initial Set)

| Type | params Shape | Applies To | Validation Rule |
|------|--------------|-----------|-----------------|
| `pattern` | `{ regex: string, flags?: string }` | STRING | Value MUST match regex |
| `minLength` | `{ value: number }` | STRING | length ≥ value |
| `maxLength` | `{ value: number }` | STRING | length ≤ value |
| `minValue` | `{ value: number }` | NUMBER | value ≥ number |
| `maxValue` | `{ value: number }` | NUMBER | value ≤ number |
| `minDate` | `{ iso: string }` | DATE | date ≥ iso timestamp |
| `maxDate` | `{ iso: string }` | DATE | date ≤ iso timestamp |
| `range` | `{ min: number\|string, max: number\|string, step?: number }` | NUMBER or DATE | Combined inclusive bounds (DATE uses ISO strings). When `expectMultipleValues = true`, applies **per element** (not to array length). Use `minValue`/`maxValue` on array length separately. |
| `custom` | `{ key: string, [extra: string]: any }` | ANY | Implementation-defined; MUST NOT break default validators |

> Future extensions MUST define param schema clearly. Unknown `type` values MUST be ignored (tolerant) or cause a controlled validation warning; they MUST NOT crash a generic validator.

### 2.7 Validation Pipeline (Normative)

```
ALGORITHM validate(fieldSpec, input) → ValidationResult

  errors ← empty list

  ── 1. REQUIRED ──────────────────────────────────────────────────────────────
  IF fieldSpec.required AND isEmpty(input) THEN
    RETURN Failure({ constraintName: "required" })
  END IF
  IF isEmpty(input) THEN
    RETURN Success()                          ▷ optional and empty → always valid
  END IF

  ── 2. TYPE ───────────────────────────────────────────────────────────────────
  IF NOT matchesType(input, fieldSpec.dataType, fieldSpec.expectMultipleValues) THEN
    RETURN Failure({ constraintName: "type" })
  END IF

  ── 3. CLOSED DOMAIN MEMBERSHIP ──────────────────────────────────────────────
  IF fieldSpec.valuesEndpoint IS PRESENT
      AND fieldSpec.valuesEndpoint.mode ≠ "SUGGESTIONS" THEN

    domain ← resolveDomain(fieldSpec.valuesEndpoint)  ▷ inline or remote fetch

    IF fieldSpec.expectMultipleValues THEN
      FOR EACH element AT index i IN input DO
        IF element ∉ domain THEN
          append MembershipError(index: i) to errors
        END IF
      END FOR
    ELSE
      IF input ∉ domain THEN
        append MembershipError() to errors
      END IF
    END IF

    ▷ Implementations MAY short-circuit here for UX performance
  END IF

  ── 4. OBJECT RECURSION ───────────────────────────────────────────────────────
  IF fieldSpec.dataType = "OBJECT" THEN
    items ← IF fieldSpec.expectMultipleValues THEN input ELSE [input]
    FOR EACH item AT index i IN items DO
      FOR EACH subField IN fieldSpec.subFields DO
        subResult ← validate(subField, item[subField.name])
        IF subResult.errors IS NOT EMPTY THEN
          append each error prefixed with path "item[i].subField.name" to errors
        END IF
      END FOR
    END FOR
  END IF

  ── 5. ORDERED CONSTRAINTS ───────────────────────────────────────────────────
  FOR EACH constraint IN fieldSpec.constraints IN DECLARED ORDER DO
    IF fieldSpec.expectMultipleValues THEN
      ▷ Array-level types (minValue, maxValue, range) apply to array length
      result ← applyArrayLevelConstraint(constraint, input)
      IF result IS FAILURE THEN append result.error to errors END IF
      ▷ Per-element types apply to each element individually
      FOR EACH element AT index i IN input DO
        result ← applyConstraint(constraint, element, fieldSpec.dataType)
        IF result IS FAILURE THEN
          append result.error WITH index: i to errors
        END IF
      END FOR
    ELSE
      result ← applyConstraint(constraint, input, fieldSpec.dataType)
      IF result IS FAILURE THEN append result.error to errors END IF
    END IF
  END FOR

  ── 6. AGGREGATE ─────────────────────────────────────────────────────────────
  RETURN ValidationResult(isValid: errors IS EMPTY, errors: errors)

END ALGORITHM
```

### 2.8 Error Object (Standard Form)

```json
{
  "constraintName": "minLength",
  "message": "Minimum length is 3",
  "value": "ab",
  "index": 0 // present only for multi-value element-level errors
}
```

Multiple errors MAY share the same `constraintName` (e.g. multi-values). Clients MAY group them for display.

### 2.9 FormSpec (Container)

A `FormSpec` is an **optional top-level container** grouping multiple `InputFieldSpec` instances and declaring cross-field constraints. It is the recommended structure when fields have interdependencies or when an AI agent needs a complete interaction contract.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✓ | Unique identifier for this form definition |
| `version` | string | | Semver of this specific form definition |
| `displayName` | LocalizedString | | Human-readable form title |
| `description` | LocalizedString | | Human-readable form description |
| `fields` | InputFieldSpec[] | ✓ | Ordered list of fields. Each field MUST carry a unique `name` key (see below) |
| `crossConstraints` | CrossConstraintDescriptor[] | | Ordered list of cross-field rules (see §2.10) |
| `submitEndpoint` | SubmitEndpoint | | Where and how to submit the completed form |

> **`name` on `InputFieldSpec` in a `FormSpec`:** When a field is declared inside a `FormSpec`, it MUST carry a `name` field (string, unique within the form) used as its stable identifier for cross-constraint references, error paths, and submission payloads.

**`SubmitEndpoint`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `protocol` | string | ✓ | `HTTPS` \| `HTTP` \| `GRPC` |
| `uri` | string | ✓ | Submission endpoint URI |
| `method` | string | | `POST` (default) or `PUT` |

---

### 2.10 CrossConstraintDescriptor

Describes a validation rule that spans two or more fields. Cross-constraints are evaluated **after** all individual field validations pass.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | ✓ | Unique identifier within the form |
| `type` | string | ✓ | Constraint type (see registry below) |
| `fields` | string[] | ✓ | Names of the involved `InputFieldSpec` fields (order matters for ordered types) |
| `params` | any | ✓ | Type-specific payload |
| `errorMessage` | LocalizedString | | Message on failure; SHOULD name the involved fields |
| `description` | LocalizedString | | Human / AI-readable explanation of the rule |

**Cross-Constraint Type Registry (Initial Set)**

| Type | `fields` semantics | `params` shape | Rule |
|------|--------------------|----------------|------|
| `fieldComparison` | `[fieldA, fieldB]` | `{ operator: "lt"\|"lte"\|"gt"\|"gte"\|"eq"\|"neq" }` | `fieldA operator fieldB` MUST hold (e.g. `endDate gte startDate`) |
| `atLeastOne` | any subset | `{ min?: number }` (default 1) | At least `min` of the listed fields MUST be non-empty |
| `mutuallyExclusive` | any subset | `{ max?: number }` (default 1) | At most `max` of the listed fields MAY be non-empty at the same time |
| `dependsOn` | `[dependent, source]` | `{ sourceValues?: any[] }` | `dependent` is required when `source` is non-empty (or matches one of `sourceValues`) |
| `custom` | any | `{ key: string, [extra: string]: any }` | Implementation-defined cross-field logic |

**Error Object for CrossConstraints**

```json
{
  "crossConstraintName": "dateRange",
  "message": "End date must be after start date",
  "fields": ["startDate", "endDate"]
}
```

**Example: Date range + mutual exclusion**

```json
{
  "id": "booking-form",
  "displayName": { "default": "Booking Form", "fr": "Formulaire de réservation" },
  "fields": [
    { "name": "startDate", "displayName": "Start Date", "dataType": "DATE", "expectMultipleValues": false, "required": true, "constraints": [] },
    { "name": "endDate",   "displayName": "End Date",   "dataType": "DATE", "expectMultipleValues": false, "required": true, "constraints": [] },
    { "name": "promoCode", "displayName": "Promo Code", "dataType": "STRING", "expectMultipleValues": false, "required": false, "constraints": [] },
    { "name": "giftCard",  "displayName": "Gift Card",  "dataType": "STRING", "expectMultipleValues": false, "required": false, "constraints": [] }
  ],
  "crossConstraints": [
    {
      "name": "dateRange",
      "type": "fieldComparison",
      "fields": ["endDate", "startDate"],
      "params": { "operator": "gt" },
      "errorMessage": { "default": "End date must be after start date", "fr": "La date de fin doit être après la date de début" }
    },
    {
      "name": "oneDiscountOnly",
      "type": "mutuallyExclusive",
      "fields": ["promoCode", "giftCard"],
      "params": { "max": 1 },
      "errorMessage": { "default": "Cannot combine a promo code and a gift card" }
    }
  ],
  "submitEndpoint": { "protocol": "HTTPS", "uri": "/api/bookings", "method": "POST" }
}
```

> **AI agent note:** Before starting to fill a form, agents SHOULD load the `FormSpec` in full and build an internal dependency graph from `crossConstraints`. This allows preemptive field ordering and avoids submission failures due to relational rule violations.

---

## 3. Examples (v2.1)

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
Exemple d’un champ qui propose des suggestions mais n’impose pas l’appartenance à un domaine fermé :
```json
{
  "displayName": "Ville de naissance",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": false,
  "valuesEndpoint": {
    "protocol": "HTTPS",
    "uri": "/api/cities",
    "mode": "SUGGESTIONS",
    "searchParams": { "q": "paris" },
    "searchParamsSchema": {
      "type": "object",
      "properties": {
        "q": { "type": "string", "description": "Nom partiel de la ville" }
      },
      "required": ["q"]
    },
    "paginationStrategy": "PAGE_NUMBER"
  },
  "constraints": []
}
```

### 3.7 Advanced Search (searchParams & searchParamsSchema)
Exemple d’un champ avec recherche multi-critères structurée :
```json
{
  "displayName": "Produit",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "valuesEndpoint": {
    "protocol": "HTTPS",
    "uri": "/api/products",
    "method": "POST",
    "searchParams": { "name": "chaise", "category": "mobilier" },
    "searchParamsSchema": {
      "type": "object",
      "properties": {
        "name": { "type": "string", "description": "Nom du produit (recherche partielle)" },
        "category": { "type": "string", "description": "Catégorie du produit" }
      },
      "required": ["name"]
    },
    "paginationStrategy": "PAGE_NUMBER",
    "responseMapping": { "dataField": "results" }
  },
  "constraints": []
}
```

### 3.8 Boolean Field
Champ booléen simple :
```json
{
  "displayName": "Actif ?",
  "dataType": "BOOLEAN",
  "expectMultipleValues": false,
  "required": true,
  "constraints": []
}
```

### 3.9 Custom Constraint
Exemple d’utilisation d’un validateur customisé :
```json
{
  "displayName": "Code promotionnel",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": false,
  "constraints": [
    {
      "name": "promoCheck",
      "type": "custom",
      "params": { "key": "promoCode", "minDiscount": 10 },
      "errorMessage": "Code non valide ou réduction insuffisante"
    }
  ]
}
```

### 3.10 Nested Object Field (`OBJECT` type)

Un champ `OBJECT` pour une adresse postale, utilisable seul ou en tableau :

```json
{
  "displayName": { "default": "Billing Address", "fr": "Adresse de facturation" },
  "dataType": "OBJECT",
  "expectMultipleValues": false,
  "required": true,
  "subFields": [
    {
      "displayName": { "default": "Street", "fr": "Rue" },
      "dataType": "STRING",
      "expectMultipleValues": false,
      "required": true,
      "constraints": [
        { "name": "streetLen", "type": "maxLength", "params": { "value": 100 } }
      ]
    },
    {
      "displayName": { "default": "City", "fr": "Ville" },
      "dataType": "STRING",
      "expectMultipleValues": false,
      "required": true,
      "constraints": []
    },
    {
      "displayName": { "default": "Postal Code", "fr": "Code postal" },
      "dataType": "STRING",
      "expectMultipleValues": false,
      "required": true,
      "constraints": [
        { "name": "postalFormat", "type": "pattern", "params": { "regex": "^[0-9]{5}$" },
          "errorMessage": { "default": "5-digit code required", "fr": "Code à 5 chiffres requis" } }
      ]
    }
  ],
  "constraints": []
}
```

> Pour un tableau d'adresses, poser `"expectMultipleValues": true` : chaque élément du tableau est validé indépendamment via `subFields`.

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
  "version": "2.1"
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

### 4.4 Get Form Specification

**GET** `/api/forms/{formId}`

Returns a complete `FormSpec` including all field definitions and cross-field constraints.

**Response:**
```json
{
  "form": FormSpec
}
```

**GET** `/api/forms`

Returns all available form definitions.

**Response:**
```json
{
  "forms": [FormSpec],
  "version": "2.1"
}
```

---

## 5. Implementation Guidelines

### 5.1 Client-Side Behavior

**Rendering UI Controls:**

1. **Check for value sources (field-level `valuesEndpoint`):**
   - If `valuesEndpoint.protocol = "INLINE"` → render dropdown from `valuesEndpoint.items`
   - If `valuesEndpoint.protocol` is remote (`HTTPS`/`HTTP`/`GRPC`) → fetch and render dropdown/select with pagination support
   - Otherwise → render based on `dataType` and `constraints`

2. **Determine input type based on `dataType`:**
   - `STRING` → text input (apply `pattern`, `minLength`, `maxLength` from constraint `params`)
   - `NUMBER` → number input (apply `minValue`, `maxValue`, `range` from constraint `params`)
   - `BOOLEAN` → checkbox/toggle
   - `DATE` → date picker (apply `minDate`, `maxDate`, `range` from constraint `params`)

3. **Handle `expectMultipleValues`:**
   - If `true` and has `valuesEndpoint` → multi-select dropdown
   - If `true` and free input → add/remove input fields
   - Validate array length using `minValue`/`maxValue` constraint `params.value`

4. **Apply validation in constraint order (pipeline §2.7):**
   - Check field-level `required` first
   - For each constraint, dispatch on `constraint.type` and read config from `constraint.params`

5. **Display `constraint.errorMessage` when validation fails (falls back to generic message if absent)**

6. **Process constraints in declared array order** for deterministic, reproducible validation

**Validation Algorithm (implementation guide):**

```
── PROCEDURE isEmpty(value) → Boolean ───────────────────────────────────────
  RETURN value IS null
      OR value IS undefined / absent
      OR value IS empty string ""
      OR value IS empty array []
END PROCEDURE

── PROCEDURE matchesType(value, dataType, expectMultiple) → Boolean ─────────
  PROCEDURE checkScalar(v, dataType) → Boolean
    MATCH dataType
      "STRING"  → RETURN v IS a text value
      "NUMBER"  → RETURN v IS a numeric value AND v IS NOT NaN
      "BOOLEAN" → RETURN v IS true OR false
      "DATE"    → RETURN v IS parseable as an ISO 8601 date/datetime
      "OBJECT"  → RETURN v IS a key-value map / object structure
    END MATCH
  END PROCEDURE

  IF expectMultiple THEN
    RETURN value IS an ordered list
       AND every element passes checkScalar(element, dataType)
  ELSE
    RETURN checkScalar(value, dataType)
  END IF
END PROCEDURE

── PROCEDURE resolveErrorMessage(constraint, fallback) → LocalizedString ────
  RETURN constraint.errorMessage IF PRESENT, ELSE fallback
END PROCEDURE

── PROCEDURE applyArrayLevelConstraint(constraint, array) → Error | None ────
  ▷ minValue / maxValue / range operate on array LENGTH when expectMultipleValues = true
  MATCH constraint.type
    "minValue" →
      IF length(array) < constraint.params.value THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Minimum " + constraint.params.value + " items required"))
      END IF
    "maxValue" →
      IF length(array) > constraint.params.value THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Maximum " + constraint.params.value + " items allowed"))
      END IF
    "range" →
      IF length(array) < constraint.params.min
          OR length(array) > constraint.params.max THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Array length must be between "
                              + constraint.params.min + " and " + constraint.params.max))
      END IF
    OTHERWISE → RETURN None
  END MATCH
  RETURN None
END PROCEDURE

── PROCEDURE applyConstraint(constraint, value, dataType) → Error | None ────
  p ← constraint.params

  MATCH constraint.type

    "pattern" →
      IF value does NOT match regex(p.regex, flags: p.flags) THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint, "Invalid format"))
      END IF

    "minLength" →
      IF characterLength(value) < p.value THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Minimum " + p.value + " characters"))
      END IF

    "maxLength" →
      IF characterLength(value) > p.value THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Maximum " + p.value + " characters"))
      END IF

    "minValue" →   ▷ scalar numeric comparison (array length handled separately)
      IF dataType = "NUMBER" AND value < p.value THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Minimum value is " + p.value))
      END IF

    "maxValue" →
      IF dataType = "NUMBER" AND value > p.value THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Maximum value is " + p.value))
      END IF

    "minDate" →
      IF parseDate(value) < parseDate(p.iso) THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Date must be after " + p.iso))
      END IF

    "maxDate" →
      IF parseDate(value) > parseDate(p.iso) THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Date must be before " + p.iso))
      END IF

    "range" →
      ▷ Applies per-element; for DATE, comparisons are chronological
      v   ← IF dataType = "DATE" THEN parseDate(value) ELSE value
      min ← IF dataType = "DATE" THEN parseDate(p.min)  ELSE p.min
      max ← IF dataType = "DATE" THEN parseDate(p.max)  ELSE p.max
      IF v < min OR v > max THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Value must be between " + p.min + " and " + p.max))
      END IF
      IF p.step IS PRESENT AND dataType = "NUMBER"
          AND (value − p.min) MOD p.step ≠ 0 THEN
        RETURN Error(name: constraint.name,
                     message: resolveErrorMessage(constraint,
                              "Value must be a multiple of " + p.step
                              + " starting from " + p.min))
      END IF

    "custom" →
      ▷ Delegate to implementation-registered handler; MUST NOT crash if unregistered
      IF a handler is registered for key p.key THEN
        result ← invokeHandler(p.key, value, p)
        IF result IS NOT None THEN
          RETURN Error(name: constraint.name,
                       message: resolveErrorMessage(constraint, result))
        END IF
      END IF
      ▷ Unknown custom key → tolerated silently (no error)

    OTHERWISE →
      ▷ Unknown constraint type → tolerated per spec; MUST NOT crash
      RETURN None

  END MATCH
  RETURN None
END PROCEDURE
```

**Fetching Paginated Values:**

```
── PROCEDURE fetchValues(valuesEndpoint, page = 1) → FetchResult ────────────

  ep ← valuesEndpoint

  ── Build query parameters ───────────────────────────────────────────────────
  queryParams ← empty map

  IF ep.paginationStrategy = "PAGE_NUMBER" THEN
    queryParams[ ep.requestParams.pageParam  ] ← page
    queryParams[ ep.requestParams.limitParam ] ← ep.requestParams.defaultLimit
  END IF

  IF ep.searchParams IS PRESENT AND NOT EMPTY THEN
    FOR EACH (key, value) IN ep.searchParams DO
      queryParams[key] ← value           ▷ merged into query (GET) or body (POST)
    END FOR
  END IF

  ── Execute HTTP request ─────────────────────────────────────────────────────
  IF ep.method = "POST" THEN
    response ← HTTP POST ep.uri WITH body: queryParams
  ELSE
    response ← HTTP GET  ep.uri WITH queryString: queryParams
  END IF

  IF response IS NOT successful THEN
    RETURN Failure("VALUES_FETCH_ERROR")
  END IF

  data ← parse response body as structured data (e.g. JSON)

  ── Extract value list using responseMapping ─────────────────────────────────
  IF ep.responseMapping IS PRESENT AND ep.responseMapping.dataField IS PRESENT THEN
    items ← data[ ep.responseMapping.dataField ]
  ELSE
    items ← data                           ▷ root is assumed to be the value array
  END IF

  ── Extract pagination metadata ───────────────────────────────────────────────
  hasNext ← IF ep.responseMapping.hasNextField IS PRESENT
              THEN data[ ep.responseMapping.hasNextField ]
              ELSE false

  total   ← IF ep.responseMapping.totalField IS PRESENT
              THEN data[ ep.responseMapping.totalField ]
              ELSE undefined

  RETURN FetchResult(items: items, hasNext: hasNext, total: total)

END PROCEDURE
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
3. Dispatch on `constraint.type` and validate against `constraint.params`
4. Return appropriate `errorMessage` (as `LocalizedString`) when validation fails

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
- Use `valuesEndpoint.protocol = "INLINE"` for small, static lists (< 20 items)
- Use `valuesEndpoint` with `paginationStrategy: "NONE"` for medium lists (< 100 items)
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

Protocol version: **2.1.0**

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
| `OBJECT` | Nested object | N/A (use `custom`) | Array length (each item validated via `subFields`) |

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
    "valuesEndpoint": {
      "protocol": "HTTPS",
      "uri": "/api/users",
      "mode": "CLOSED",
      "searchParams": { "name": "" },
      "searchParamsSchema": {
        "type": "object",
        "properties": {
          "name": { "type": "string", "description": "Partial user name for search" }
        }
      },
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
    },
    "constraints": [
      {
        "name": "required-value",
        "type": "custom",
        "params": { "key": "nonempty" },
        "errorMessage": "Please select a user"
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
## 10. FormSpec and Cross-Field Validation

### 10.1 When to Use FormSpec

`InputFieldSpec` SHOULD be used standalone for single-field validation scenarios (search filters, single inputs). `FormSpec` SHOULD be used when:

- Two or more fields have relational constraints between them
- A submit action must be declared as part of the contract
- An AI agent or automation client needs a complete, self-contained interaction contract

### 10.2 Validation Order (FormSpec Level)

```
1. Validate each field individually (pipeline §2.7)
2. If all individual fields are valid → evaluate crossConstraints in array order
3. Aggregate all errors (field-level + cross-level) before returning
```

Cross-constraints MUST NOT be evaluated if any field they reference has an individual validation error. This avoids misleading error messages (e.g. "end date must be after start date" when start date itself is invalid).

### 10.3 AI Agent Contract

A `FormSpec` is the ideal MCP resource for an AI agent interacting with a form-based system. The agent can:

1. Load `FormSpec` → discover all fields, types, constraints, and cross-rules
2. Use `searchParamsSchema` on `ValuesEndpoint` to call value endpoints as structured tools
3. Build values respecting both individual constraints and `crossConstraints`
4. Submit to `submitEndpoint` with confidence the payload is valid

The `description` and `displayName` fields (as `LocalizedString`) provide the semantic context the agent needs to infer intent without hardcoded mappings.


## 11. Protocol Specification Scope

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

## 12. Security Considerations

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




## 13. Summary

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

---
