---
layout: default
title: "Exemples pratiques"
nav_order: 60
---

# Exemples pratiques

> Fidélité : Chaque exemple se limite aux API présentes dans le dépôt (Java ou TypeScript). Toute idée future est marquée *Suggestion* et non utilisée dans le code.

## 1. Hello World (TypeScript) — Résolution de valeurs

```typescript
import {
  ValuesResolver,
  FetchHttpClient,
  MemoryCacheProvider,
  createDefaultValuesEndpoint
} from 'input-field-spec-ts';

const resolver = new ValuesResolver(
  new FetchHttpClient(),
  new MemoryCacheProvider()
);

// Endpoint simple (GET)
const endpoint = createDefaultValuesEndpoint('https://api.example.com/countries');

const result = await resolver.resolveValues(endpoint, { search: 'fra' });
console.log(result); // Tableau d'alias potentiels (selon backend)
```

## 2. Hello World (Java) — Construction d'un champ

```java
import io.github.cyfko.inputspec.*;

InputFieldSpec emailField = new InputFieldSpec(
    "email", // displayName
    DataType.STRING,
    true,     // required
    false,    // expectMultipleValues
    java.util.List.of(
        new ConstraintDescriptor("pattern", "^[\\w-\\.+]+@([\\w-]+\\.)+[\\w-]{2,}$", null, null, null, null, "Email invalide"),
        new ConstraintDescriptor("length", null, 5, 120, null, null, null)
    )
);

System.out.println(emailField.getDisplayName());
```

> Java : Aucune classe de validation complète fournie dans ce dépôt (pas de `FieldValidator` Java). *Suggestion* : introduire un validateur côté client Java si nécessaire.

## 3. Validation d'un champ (TypeScript)

```typescript
import { FieldValidator, InputFieldSpec } from 'input-field-spec-ts';

const field: InputFieldSpec = {
  displayName: 'Age',
  dataType: 'NUMBER',
  required: true,
  expectMultipleValues: false,
  constraints: [
    { name: 'min', min: 18 },
    { name: 'max', max: 130 }
  ]
};

const validator = new FieldValidator();
const ok = await validator.validate(field, 42);
console.log(ok.isValid); // true

const fail = await validator.validate(field, 10);
console.log(fail.isValid); // false
console.log(fail.errors);  // Message min
```

## 4. Pagination d'un ValuesEndpoint (TypeScript)

```typescript
import { ValuesResolver, createDefaultValuesEndpoint } from 'input-field-spec-ts';

// Hypothèse : l'endpoint backend supporte ?page=&pageSize=
const endpoint = createDefaultValuesEndpoint('https://api.example.com/users');

// Paramètres de recherche paginée
const page1 = await resolver.resolveValues(endpoint, { page: 1, pageSize: 20 });
const page2 = await resolver.resolveValues(endpoint, { page: 2, pageSize: 20 });

console.log(page1, page2);
```

> La logique de pagination réelle dépend du backend; aucun adaptateur spécifique supplémentaire n'est présent.

## 5. Alias & sélection multiple (TypeScript)

```typescript
import { InputFieldSpec, FieldValidator } from 'input-field-spec-ts';

const countryField: InputFieldSpec = {
  displayName: 'Countries',
  dataType: 'STRING',
  required: true,
  expectMultipleValues: true,
  constraints: [
    { name: 'enum', enumValues: ['FR', 'DE', 'ES', 'IT'] }
  ]
};

const validator = new FieldValidator();
const result = await validator.validate(countryField, ['FR', 'ES']);
console.log(result.isValid); // true
```

## Synthèse

| Exemple | Langage | Objectif | Points clés |
|---------|---------|----------|-------------|
| 1 | TS | Résolution basique | Resolver + cache mémoire |
| 2 | Java | Déclaration de champ | Contraintes pattern/length |
| 3 | TS | Validation numérique | min/max séquentiel |
| 4 | TS | Pagination | Paramètres page/pageSize |
| 5 | TS | Enum multiple | expectMultipleValues + enum |

---

*Page générée — toutes les API utilisées existent dans le code. Les idées futures sont marquées en italique si mentionnées.*
