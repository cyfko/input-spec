# Frontend Integration (v2) · Angular · React · Vue · Svelte

This guide targets protocol v2 (atomic constraints, field-level `valuesEndpoint`, `formatHint`, removal of `enumValues`). UI styling is intentionally out-of-scope.

Core exports used:
- `InputFieldSpec`, `ConstraintDescriptor` (atomic form)
- `FieldValidator`, `validateField`

Key v2 shifts:
- Composite fields (`min`, `max`, `pattern`, `enumValues`) replaced by atomic constraints: `{ name, type, params }`
- Static enumerations via `valuesEndpoint.protocol = 'INLINE'`
- `formatHint` added (non-failing display hint)
- Membership modes: `CLOSED` (strict), `SUGGESTIONS` (advisory)

---
## 1. Shared Validation Pattern

```typescript
import { FieldValidator, InputFieldSpec, validateField } from 'input-spec';

const validator = new FieldValidator();

export function validateValue(spec: InputFieldSpec, value: any) {
  return validator.validate(spec, value);
}

export function validateValueStateless(spec: InputFieldSpec, value: any) {
  return validateField(spec, value);
}
```

Example field specification (username):
```typescript
const usernameSpec: InputFieldSpec = {
  displayName: 'Username',
  description: '3–20 chars alphanum + underscore',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,
  formatHint: 'username',
  constraints: [
    { name: 'minL', type: 'minLength', params: { value: 3 } },
    { name: 'maxL', type: 'maxLength', params: { value: 20 } },
    { name: 'syntax', type: 'pattern', params: { regex: '^[a-zA-Z0-9_]+' }, errorMessage: 'Alnum + underscore only' }
  ]
};
```

---
## 2. Angular

### 2.1 Async Validator Directive (All Constraints)
```typescript
import { Directive, Input, forwardRef } from '@angular/core';
import { NG_ASYNC_VALIDATORS, AbstractControl, AsyncValidator, ValidationErrors } from '@angular/forms';
import { InputFieldSpec, validateField } from 'input-spec';
import { from, of } from 'rxjs';

@Directive({
  selector: '[inputSpecField]',
  providers: [
    { provide: NG_ASYNC_VALIDATORS, useExisting: forwardRef(() => InputSpecFieldDirective), multi: true }
  ]
})
export class InputSpecFieldDirective implements AsyncValidator {
  @Input('inputSpecField') spec!: InputFieldSpec | null;

  validate(control: AbstractControl) {
    if (!this.spec) return of(null);
    return from(
  validateField(this.spec, control.value).then(result => {
        if (result.isValid) return null;
        const grouped: Record<string, { messages: string[] }> = {};
        for (const err of result.errors) {
          grouped[err.constraintName] ||= { messages: [] };
          grouped[err.constraintName].messages.push(err.message);
        }
        return { inputSpec: grouped } as ValidationErrors;
      }).catch(e => ({ inputSpecInternal: { message: e?.message || 'validation_error' } }))
    );
  }
}
```

### 2.2 Usage
```html
<input formControlName="username" [inputSpecField]="usernameSpec" />
@if (fc('username').errors as errs) {
  @for (k of Object.keys(errs.inputSpec || {}); track k) {
    <strong>{{ k }}</strong>
    @for (m of errs.inputSpec[k].messages; track m) {
      <div>{{ m }}</div>
    }
  }
}
```

---
## 3. React

Minimal hook returning validation state.
```tsx
import { useCallback, useState } from 'react';
import { InputFieldSpec, validateField } from 'input-spec';

export function useInputSpec(spec: InputFieldSpec) {
  const [errors, setErrors] = useState<Record<string,string[]>>({});
  const [valid, setValid] = useState(true);

  const validate = useCallback(async (value: any) => {
  const result = validateField(spec, value);
    if (result.isValid) {
      setErrors({});
      setValid(true);
      return true;
    }
    const grouped: Record<string,string[]> = {};
    for (const err of result.errors) {
      grouped[err.constraintName] ||= [];
      grouped[err.constraintName].push(err.message);
    }
    setErrors(grouped);
    setValid(false);
    return false;
  }, [spec]);

  return { validate, errors, valid };
}
```

Usage:
```tsx
const { validate, errors, valid } = useInputSpec(usernameSpec);
<input onChange={e => validate(e.target.value)} />
{!valid && Object.entries(errors).map(([c, msgs]) => (
  <div key={c}>
    <strong>{c}</strong>
    <ul>{msgs.map(m => <li key={m}>{m}</li>)}</ul>
  </div>
))}
```

---
## 4. Vue 3 (Composition API)
```ts
import { ref } from 'vue';
import { InputFieldSpec, validateField } from 'input-spec';

export function useInputSpec(spec: InputFieldSpec) {
  const errors = ref<Record<string,string[]>>({});
  const valid = ref(true);

  async function validate(value: any) {
  const result = validateField(spec, value);
    if (result.isValid) {
      errors.value = {};
      valid.value = true;
      return true;
    }
    const grouped: Record<string,string[]> = {};
    for (const err of result.errors) {
      grouped[err.constraintName] ||= [];
      grouped[err.constraintName].push(err.message);
    }
    errors.value = grouped;
    valid.value = false;
    return false;
  }

  return { errors, valid, validate };
}
```

Template:
```vue
<input @input="e => validate(e.target.value)" />
<div v-if="!valid">
  <div v-for="(msgs, key) in errors" :key="key">
    <strong>{{ key }}</strong>
    <ul><li v-for="m in msgs" :key="m">{{ m }}</li></ul>
  </div>
</div>
```

---
## 5. Svelte Store Helper
```ts
import { writable } from 'svelte/store';
import { validateField, type InputFieldSpec } from 'input-spec';

export function createInputSpecValidator(spec: InputFieldSpec) {
  const errors = writable<Record<string,string[]>>({});
  const valid = writable(true);

  async function validate(value: any) {
  const result = validateField(spec, value);
    if (result.isValid) {
      errors.set({});
      valid.set(true);
      return true;
    }
    const grouped: Record<string,string[]> = {};
    for (const err of result.errors) {
      grouped[err.constraintName] ||= [];
      grouped[err.constraintName].push(err.message);
    }
    errors.set(grouped);
    valid.set(false);
    return false;
  }

  return { errors, valid, validate };
}
```
Usage:
```svelte
<script lang="ts">
  import { createInputSpecValidator } from './inputSpecStore';
  import type { InputFieldSpec } from 'input-spec';

  export let usernameSpec: InputFieldSpec;
  const { validate, errors, valid } = createInputSpecValidator(usernameSpec);
  let value = '';
</script>

<input bind:value on:input={(e) => validate(value)} />
{#if !$valid}
  {#each Object.entries($errors) as [k,msgs]}
    <div><strong>{k}</strong><ul>{#each msgs as m}<li>{m}</li>{/each}</ul></div>
  {/each}
{/if}
```

---
## 6. Dynamic Values (Autocomplete) Pattern
All frameworks can reuse the same resolver logic. If you expose `ValuesResolver` & `createDefaultValuesEndpoint`:

```typescript
// v2 zero-dep build omits a dynamic fetch helper. Use your HTTP client to resolve remote domains.
// Static INLINE example attached directly in spec → no call required.
```

---
## 7. Error Handling Strategy
- Always consume the `errors: ValidationError[]` array.
- Group by `constraintName` for display.
- Avoid logic branching by pattern: trust the backend specification.

---
## 8. Recommended Folder Layout (Frontend)
```
frontend/
  validation/
    input-spec.directive.ts      # Angular directive
    input-spec.hook.ts           # React hook
    input-spec.composable.ts     # Vue composable
    input-spec.store.ts          # Svelte store helper
```

---
## 9. Testing Suggestions
| Layer | What to test | Example |
|-------|--------------|---------|
| Spec parsing | Required + constraints order | Feed malformed object -> expect failure handling upstream |
| Validator integration | Aggregated errors | Pattern + length wrong -> 2 errors grouped (order preserved) |
| UI binding | Error rendering | Simulate invalid input -> DOM contains messages |

---
## 10. Minimal Migration Checklist
- [ ] Replace legacy client-side regex duplicates with `InputFieldSpec`.
- [ ] Centralize constraint authoring server-side.
- [ ] Use only protocol fields; no ad hoc frontend flags.
- [ ] Group UI error messages by `constraintName`.
- [ ] For remote dynamic values, implement your own fetch & map to `valuesEndpoint` contract.

---
## 11. Notes
- No UI opinion: you own rendering and styling.
- All examples rely on full validation (not per-constraint selective calls).
- Extend by wrapping these utilities, not by altering the protocol shape.

---
## 12. Future Enhancements (Optional)
- Debounced wrapper around `validateField`.
- Caching layer for unchanged value/spec pairs.
- Batch validation for entire forms (array of `InputFieldSpec`).

---
© input-spec – Frontend Integration Guide
