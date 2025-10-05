---
layout: default
title: Frontend Integration
nav_order: 6
description: "Intégration Frontend (Angular · React · Vue · Svelte)."
---
# Intégration Frontend (Angular · React · Vue · Svelte)

Ce guide montre comment consommer le protocole **input-spec** côté client sans imposer de design visuel. Tous les exemples s'appuient uniquement sur les API publiques exportées.

API utilisées :
- `InputFieldSpec`, `ConstraintDescriptor`
- `FieldValidator` (ou les helpers `validateField`, `validateAllConstraints`)
- `createDefaultValuesEndpoint`, `ValuesEndpoint`, `ValuesResolver` (pour les valeurs dynamiques)

---
## 1. Schéma de validation partagé

Exemple de spécification de champ (username) :
```typescript
const usernameSpec: InputFieldSpec = {
  displayName: 'Nom d\'utilisateur',
  description: '3–20 caractères alphanum + underscore',
  dataType: 'STRING',
  expectMultipleValues: false,
  required: true,
  constraints: [
    {
      name: 'usernamePattern',
      pattern: '^[a-zA-Z0-9_]{3,20}$',
      errorMessage: 'Doit comporter 3 à 20 caractères (lettres, chiffres, underscore)'
    }
  ]
};
```

---
## 2. Angular

### 2.1 Directive Async Validator (toutes contraintes)
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

### 2.2 Utilisation
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

Hook minimal retournant l'état de validation.
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

Utilisation :
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
Template :
```html
<input @input="e => validate(e.target.value)" />
<div v-if="!valid">
  <div v-for="(msgs, key) in errors" :key="key">
    <strong>{{ key }}</strong>
    <ul><li v-for="m in msgs" :key="m">{{ m }}</li></ul>
  </div>
</div>
```

---
## 5. Svelte (Store helper)
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
Utilisation :
```html
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
## 6. Valeurs dynamiques (Autocomplete)
Logique réutilisable pour tout framework :
```typescript
import { ValuesResolver, FetchHttpClient, MemoryCacheProvider, createDefaultValuesEndpoint } from 'input-spec';

const resolver = new ValuesResolver(new FetchHttpClient(), new MemoryCacheProvider());
const countriesEndpoint = createDefaultValuesEndpoint('https://api.example.com/countries');

export async function rechercherPays(query: string) {
  const result = await resolver.resolveValues(countriesEndpoint, { search: query, page: 1, limit: 10 });
  return result.values; // ValueAlias[] (value,label)
}
```

---
## 7. Stratégie de gestion des erreurs
- Toujours consommer le tableau `errors: ValidationError[]`.
- Regrouper par `constraintName` pour l'affichage.
- Ne pas re-dupliquer les règles côté client (source unique serveur).

---
## 8. Arborescence recommandée
```
frontend/
  validation/
    input-spec.directive.ts      # Directive Angular
    input-spec.hook.ts           # Hook React
    input-spec.composable.ts     # Composable Vue
    input-spec.store.ts          # Store helper Svelte
```

---
## 9. Suggestions de tests
| Couche | Cible | Exemple |
|--------|-------|---------|
| Parsing spec | Obligatoire + ordre contraintes | Objet mal formé -> gestion amont |
| Intégration validateur | Agrégation d'erreurs | Pattern + longueur -> 2 erreurs |
| Liaison UI | Rendu des erreurs | Input invalide -> messages présents |

---
## 10. Checklist de migration minimale
- [ ] Remplacer les regex historiques dispersées par `InputFieldSpec`.
- [ ] Centraliser la définition des contraintes côté serveur.
- [ ] Utiliser uniquement les champs du protocole (aucun flag ad hoc).
- [ ] Regrouper les messages UI par `constraintName`.
- [ ] Utiliser `ValuesResolver` pour les valeurs dynamiques.

---
## 11. Notes
- Aucune opinion sur l'UI : total contrôle visuel.
- Les exemples font toujours une validation complète.
- Étendre via des wrappers sans modifier la forme du protocole.

---
## 12. Améliorations futures (optionnel)
- Wrapper avec debounce autour de `validateAllConstraints`.
- Cache pour paires (spec, valeur) inchangées.
- Validation batch d'un ensemble de `InputFieldSpec`.

---
© input-spec – Guide d'intégration Frontend (FR)
