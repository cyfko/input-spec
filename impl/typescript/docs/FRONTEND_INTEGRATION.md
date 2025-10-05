# Frontend Integration Utilities (Angular · React · Vue · Svelte)

This guide shows how to consume the **input-spec** protocol on the client side without imposing any visual design. All examples rely only on the public exported APIs:

Exports used:
- `InputFieldSpec`, `ConstraintDescriptor`
- `FieldValidator` (or the helper `validateField`, `validateAllConstraints`)
- `createDefaultValuesEndpoint` and `ValuesEndpoint` (for dynamic values)

> Fidelity note: Only actual fields present in `InputFieldSpec` are used (`displayName`, `description`, `dataType`, `expectMultipleValues`, `required`, `constraints`). Length or pattern logic is expressed through individual `ConstraintDescriptor` entries (`min`, `max`, `pattern`, `enumValues`).

---
## 1. Shared Validation Pattern

```typescript
import { FieldValidator, InputFieldSpec, validateAllConstraints } from 'input-spec';

const validator = new FieldValidator();

export async function validateValue(spec: InputFieldSpec, value: any) {
  return validator.validate(spec, value); // all constraints
}

export async function validateValueStateless(spec: InputFieldSpec, value: any) {
  return validateAllConstraints(spec, value); // helper wrapper
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
  constraints: [
    {
      name: 'usernamePattern',
      pattern: '^[a-zA-Z0-9_]{3,20}$',
      errorMessage: 'Username must be 3-20 chars (letters, digits, underscore)'
    }
  ]
};
```

---
## 2. Angular

### 2.1 Async Validator Directive (All Constraints)
```typescript
import { Directive, Input, forwardRef } from '@angular/core';
import { NG_ASYNC_VALIDATORS, AbstractControl, AsyncValidator, ValidationErrors } from '@angular/forms';
import { InputFieldSpec, validateAllConstraints } from 'input-spec';
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
      validateAllConstraints(this.spec, control.value).then(result => {
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
<div *ngIf="fc('username').errors as errs">
  <div *ngFor="let k of objectKeys(errs.inputSpec || {})">
    <strong>{{ k }}</strong>
    <div *ngFor="let m of errs.inputSpec[k].messages">{{ m }}</div>
  </div>
</div>
```

---
## 3. React

Minimal hook returning validation state.
```tsx
import { useCallback, useState } from 'react';
import { InputFieldSpec, validateAllConstraints } from 'input-spec';

export function useInputSpec(spec: InputFieldSpec) {
  const [errors, setErrors] = useState<Record<string,string[]>>({});
  const [valid, setValid] = useState(true);

  const validate = useCallback(async (value: any) => {
    const result = await validateAllConstraints(spec, value);
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
import { InputFieldSpec, validateAllConstraints } from 'input-spec';

export function useInputSpec(spec: InputFieldSpec) {
  const errors = ref<Record<string,string[]>>({});
  const valid = ref(true);

  async function validate(value: any) {
    const result = await validateAllConstraints(spec, value);
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
import { validateAllConstraints, type InputFieldSpec } from 'input-spec';

export function createInputSpecValidator(spec: InputFieldSpec) {
  const errors = writable<Record<string,string[]>>({});
  const valid = writable(true);

  async function validate(value: any) {
    const result = await validateAllConstraints(spec, value);
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
import { ValuesResolver, FetchHttpClient, MemoryCacheProvider, createDefaultValuesEndpoint } from 'input-spec';

const resolver = new ValuesResolver(new FetchHttpClient(), new MemoryCacheProvider());
const countriesEndpoint = createDefaultValuesEndpoint('https://api.example.com/countries');

export async function searchCountries(query: string) {
  const result = await resolver.resolveValues(countriesEndpoint, { search: query, page: 1, limit: 10 });
  return result.values; // ValueAlias[] (value,label)
}
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
| Validator integration | Aggregated errors | Pattern + length wrong -> 2 errors grouped |
| UI binding | Error rendering | Simulate invalid input -> DOM contains messages |

---
## 10. Minimal Migration Checklist
- [ ] Replace legacy client-side regex duplicates with `InputFieldSpec`.
- [ ] Centralize constraint authoring server-side.
- [ ] Use only protocol fields; no ad hoc frontend flags.
- [ ] Group UI error messages by `constraintName`.
- [ ] Async value lookups use `ValuesResolver`.

---
## 11. Notes
- No UI opinion: you own rendering and styling.
- All examples rely on full validation (not per-constraint selective calls).
- Extend by wrapping these utilities, not by altering the protocol shape.

---
## 12. Future Enhancements (Optional)
- Debounced wrapper around `validateAllConstraints`.
- Caching layer for unchanged value/spec pairs.
- Batch validation for entire forms (array of `InputFieldSpec`).

---
© input-spec – Frontend Integration Guide
