---
layout: default
title: Frontend Integration
nav_order: 3
description: "Intégration Frontend (Angular · React · Vue · Svelte)."
---
[🇫🇷 Français](./FRONTEND_INTEGRATION.md) | [🇬🇧 English](./en/FRONTEND_INTEGRATION.md)
---
# Intégration Frontend (Angular · React · Vue · Svelte)

Ce guide montre comment consommer le protocole **input-spec** côté client sans imposer de design visuel. Tous les exemples s'appuient uniquement sur les API publiques exportées.

API principales :
- `InputFieldSpec`
- `FieldValidator.validate(fieldSpec, value)`

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
  formatHint: 'username',
  constraints: [
    { name: 'minL', type: 'minLength', params: { value: 3 } },
    { name: 'maxL', type: 'maxLength', params: { value: 20 } },
    { name: 'syntax', type: 'pattern', params: { regex: '^[a-zA-Z0-9_]+' }, errorMessage: 'Alphanum + underscore uniquement' }
  ]
};
```

---
## 2. Angular (>= v17, standalone, Typed Forms)

### 2.1 AsyncValidator standalone + Signal de statut
```typescript
import { Directive, Input, forwardRef, computed, signal } from '@angular/core';
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

  status = signal<'idle'|'valid'|'invalid'|'checking'>('idle');
  errorsMap = signal<Record<string,string[]>>({});

  validate(control: AbstractControl) {
    if (!this.spec) return of(null);
    this.status.set('checking');
    return from(
      validateField(this.spec, control.value).then(result => {
        if (result.isValid) {
          this.status.set('valid');
          this.errorsMap.set({});
          return null;
        }
        const grouped: Record<string,string[]> = {};
        for (const err of result.errors) {
          (grouped[err.constraintName] ||= []).push(err.message);
        }
        this.errorsMap.set(grouped);
        this.status.set('invalid');
        return { inputSpec: grouped } as ValidationErrors;
      }).catch(e => {
        this.status.set('invalid');
        return { inputSpecInternal: { message: e?.message || 'validation_error' } };
      })
    );
  }
}
```

### 2.2 Utilisation (standalone component)
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
## 3. React (18+, Hooks, Transition Safe)

Hook avec gestion debounced + état détaillé.
```tsx
import { useCallback, useState, useRef, useTransition } from 'react';
import { InputFieldSpec, validateField } from 'input-spec';

export function useInputSpec(spec: InputFieldSpec, { debounceMs = 250 } = {}) {
  const [errors, setErrors] = useState<Record<string,string[]>>({});
  const [valid, setValid] = useState(true);
  const [isPending, startTransition] = useTransition();
  const timer = useRef<number | undefined>();

  const run = useCallback(async (value: any) => {
    const result = await validateField(spec, value);
    startTransition(() => {
      if (result.isValid) {
        setErrors({});
        setValid(true);
      } else {
        const grouped: Record<string,string[]> = {};
        for (const err of result.errors) {
          (grouped[err.constraintName] ||= []).push(err.message);
        }
        setErrors(grouped);
        setValid(false);
      }
    });
  }, [spec]);

  const validate = useCallback((value: any) => {
    if (timer.current) window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => { run(value); }, debounceMs);
  }, [debounceMs, run]);

  return { validate, errors, valid, isPending };
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
## 4. Vue 3 (Composition API + <script setup>)
```ts
import { ref } from 'vue';
import { InputFieldSpec, validateField } from 'input-spec';

export function useInputSpec(spec: InputFieldSpec, { debounceMs = 250 } = {}) {
  const errors = ref<Record<string,string[]>>({});
  const valid = ref(true);
  const pending = ref(false);
  let handle: number | undefined;

  function schedule(value: any) {
    if (handle) window.clearTimeout(handle);
    handle = window.setTimeout(async () => {
      pending.value = true;
      const result = await validateField(spec, value);
      if (result.isValid) {
        errors.value = {};
        valid.value = true;
      } else {
        const grouped: Record<string,string[]> = {};
        for (const err of result.errors) {
          (grouped[err.constraintName] ||= []).push(err.message);
        }
        errors.value = grouped;
        valid.value = false;
      }
      pending.value = false;
    }, debounceMs);
  }

  return { errors, valid, pending, validate: schedule };
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
## 5. Svelte (Store helper + debounce)
```ts
import { writable } from 'svelte/store';
import { validateField, type InputFieldSpec } from 'input-spec';

export function createInputSpecValidator(spec: InputFieldSpec, { debounceMs = 250 } = {}) {
  const errors = writable<Record<string,string[]>>({});
  const valid = writable(true);
  const pending = writable(false);
  let handle: number | undefined;

  function validate(value: any) {
    if (handle) clearTimeout(handle);
    handle = setTimeout(async () => {
      pending.set(true);
      const result = await validateField(spec, value);
      if (result.isValid) {
        errors.set({});
        valid.set(true);
      } else {
        const grouped: Record<string,string[]> = {};
        for (const err of result.errors) {
          (grouped[err.constraintName] ||= []).push(err.message);
        }
        errors.set(grouped);
        valid.set(false);
      }
      pending.set(false);
    }, debounceMs) as unknown as number;
  }

  return { errors, valid, pending, validate };
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
## 6. Valeurs dynamiques (Autocomplete v2)
Exemple générique (fetch JSON) pour un champ avec `fieldSpec.valuesEndpoint` en mode `SUGGESTIONS`.
```ts
export async function fetchSuggestions(fieldSpec: InputFieldSpec, query: string) {
  if (!fieldSpec.valuesEndpoint) return [];
  const ve = fieldSpec.valuesEndpoint;
  if (ve.mode === 'CLOSED' && query.length === 0) {
    // Charger la première page si pagination
  }
  const url = new URL(ve.uri, window.location.origin);
  if (ve.requestParams?.searchParam) {
    url.searchParams.set(ve.requestParams.searchParam, query);
  }
  const res = await fetch(url.toString(), { method: ve.method || 'GET' });
  const data = await res.json();
  const container = ve.responseMapping?.dataField ? data[ve.responseMapping.dataField] : data;
  return Array.isArray(container) ? container : [];
}
```

---
## 7. Gestion des erreurs
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
## 9. Tests ciblés
| Couche | Cible | Exemple |
|--------|-------|---------|
| Parsing spec | Obligatoire + ordre contraintes | Objet mal formé -> gestion amont |
| Intégration validateur | Agrégation d'erreurs | Pattern + longueur -> 2 erreurs (ordre préservé) |
| Liaison UI | Rendu des erreurs | Input invalide -> messages présents |

---
## 10. Checklist de migration minimale
- [ ] Remplacer les regex historiques dispersées par `InputFieldSpec`.
- [ ] Centraliser la définition des contraintes côté serveur.
- [ ] Utiliser uniquement les champs du protocole (aucun flag ad hoc).
- [ ] Regrouper les messages UI par `constraintName`.
- [ ] Domaine dynamique distant : implémenter un fetch custom.

---
## 11. Notes
- Aucune opinion sur l'UI : total contrôle visuel.
- Les exemples font toujours une validation complète.
- Étendre via des wrappers sans modifier la forme du protocole.

---
## 12. Améliorations futures (optionnel)
- Wrapper avec debounce autour de `validateField`.
- Cache pour paires (spec, valeur) inchangées.
- Validation batch d'un ensemble de `InputFieldSpec`.

---
## 13. FAQ rapide Frontend

| Question | Réponse courte | Exemple |
|----------|----------------|---------|
| Valider seulement au blur ? | N'appelez `validate` que dans `onBlur` / `@blur` / `on:blur`. | React: `<input onBlur={e => validate(e.target.value)} />` |
| Debounce différent selon champ ? | Passez `debounceMs` différent par hook/composable/store. | `useInputSpec(spec, { debounceMs: 600 })` |
| Comment pré-charger un domain fermé ? | Si `valuesEndpoint.mode==='CLOSED'`, fetch au montage et mettez les options en cache local. | `useEffect(()=>{ fetchSuggestions(spec,''); },[])` |
| Mapping i18n des erreurs ? | Transformez chaque message avant affichage via une table locale. | `const msg = t(err.messageKey || err.message)` |
| Validation multi‑champs (dépendances) ? | Validez chaque champ après mise à jour du contexte partagé. | Mettre un état global `formData` + re‑valider dépendants |
| Intégration React Hook Form ? | Utiliser un resolver async qui appelle `validateField` pour chaque champ. | Voir pattern resolver classique (non répété ici) |
| Annuler une validation en cours ? | Utiliser un contrôleur Abort ou un flag « génération » de requête. | Stocker `currentRunId` et ignorer les réponses obsolètes |
| Suggestions côté Vue avec composition ? | Exposer `suggestions` + méthode `load(query)` dans le composable. | `const suggestions = ref([]);` puis assigner après fetch |
| Angular: afficher état pending ? | Lire `directive.status()` signal. | `@if (dir.status()==='checking'){ <span>…</span> }` |
| Svelte: reset après submit ? | Réinitialiser stores `errors.set({}); valid.set(true);`. | Dans handler `on:submit` |

### Snippet: Resolver React Hook Form minimal
```ts
import { validateField } from 'input-spec';

export const makeResolver = (specs: Record<string, InputFieldSpec>) => async (values: any) => {
  const errors: Record<string, any> = {};
  await Promise.all(Object.entries(specs).map(async ([name, spec]) => {
    const res = await validateField(spec, values[name]);
    if (!res.isValid) {
      errors[name] = {
        type: 'input-spec',
        message: res.errors[0]?.message,
        messages: res.errors.map(e => e.message)
      };
    }
  }));
  return { values: Object.keys(errors).length ? {} : values, errors };
};
```

### Snippet: Pré-chargement de suggestions (React)
```ts
useEffect(() => {
  let cancelled = false;
  (async () => {
    if (fieldSpec.valuesEndpoint && fieldSpec.valuesEndpoint.mode === 'CLOSED') {
      const initial = await fetchSuggestions(fieldSpec, '');
      if (!cancelled) setInitialOptions(initial);
    }
  })();
  return () => { cancelled = true; };
}, [fieldSpec]);
```

### Snippet: Adapter i18n simple
```ts
const dictionary: Record<string,string> = {
  'validation.required': 'Champ obligatoire',
  'validation.pattern': 'Format invalide'
};
function translate(message: string) {
  return dictionary[message] || message;
}
```

---
© input-spec – Guide d'intégration Frontend (FR)
