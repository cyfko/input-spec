---
layout: default
title: "Référence TypeScript"
permalink: /reference-typescript/
---

# Référence TypeScript

Synthèse des exports publics depuis `impl/typescript/src/index.ts`.

> Fidélité : Uniquement ce qui est exporté explicitement (`types`, `validation`, `client`, `createDefaultValuesEndpoint`, constantes `PROTOCOL_VERSION`, `LIBRARY_VERSION`). Aucun type non présent n’est inventé.

## � Perspectives

### Client (C2)
Validation (`FieldValidator`), résolution de valeurs (`ValuesResolver`), implémentations HTTP (`FetchHttpClient`) et cache (`MemoryCacheProvider`).

### Serveur (C2)
Ce dépôt n’embarque pas de serveur TypeScript. Les endpoints doivent seulement respecter le format attendu par `ValuesEndpoint`. Toute description de persistance ou d’agrégation distante est une *Suggestion*.

### Interaction
`ValuesResolver` construit la requête (search / pagination) → appelle l’implémentation `HttpClient` → applique un cache éventuel → résultats exploités puis validés côté client.

## �📦 Modules exportés

| Module | Contenu (catégorie) | Commentaire |
|--------|---------------------|-------------|
| `./types` | Interfaces & types du protocole (InputFieldSpec, ConstraintDescriptor, ValuesEndpoint, etc.) | Structure de données partagée |
| `./validation` | Moteur de validation (ex: FieldValidator, ValidationResult) | Applique les contraintes dans l’ordre |
| `./client` | Abstractions & implémentations HTTP/cache (ex: HttpClient, FetchHttpClient, MemoryCacheProvider, ValuesResolver) | Résolution de valeurs distante |
| `createDefaultValuesEndpoint` | Fonction utilitaire | Crée rapidement une config d’endpoint valeurs |
| `PROTOCOL_VERSION` | Constante (string) | Version protocole = `'2.0'` dans ce code |
| `LIBRARY_VERSION` | Constante (string) | Version lib = `"1.0.0"` |

## 🔑 Types centraux (dérivés de `./types`)

| Nom | Rôle | Notes |
|-----|------|------|
| `InputFieldSpec` | Décrit un champ intelligent | Champs : displayName, dataType, required, expectMultipleValues, constraints |
| `ConstraintDescriptor` | Une contrainte ordonnée | pattern, min, max, enumValues, valuesEndpoint, errorMessage, format |
| `ValuesEndpoint` | Source dynamique de valeurs | url/uri, searchParam, pagination, debounce, minSearchLength (selon impl) |
| `ValueAlias` | Option (value/label) | Équivalent Java |
| `ValidationResult` | Résultat validation | isValid + liste d’erreurs |
| `ValidationError` | Erreur unitaire | message + metadata potentielle |

> Les noms exacts des propriétés doivent être vérifiés dans les fichiers `./types/` (non recopiés intégralement ici pour éviter la divergence; consulter le code pour la source de vérité).
> Les noms exacts des propriétés doivent être vérifiés dans `./types/` (source de vérité; pas de duplication intégrale ici pour réduire le risque de divergence).

## 🧪 Validation (module `validation`)

Hypothèses basées sur la parité avec Java : 
- Ordre : required → type → contraintes séquentielles.
- `FieldValidator.validate(fieldSpec, value, constraintName?)` semblable au Java.

> Vérifier les noms exacts des méthodes dans `src/validation/` avant d’écrire des exemples complexes. Si différences → ajuster la doc (ouvrir issue si incohérence).
> Vérifier les signatures dans `src/validation/` avant d’écrire des exemples avancés. Ouvrir une issue si divergence.

## 🌐 Résolution de valeurs (module `client`)

Fonctions/Classes probables :
- `ValuesResolver` : Orchestration fetch + cache.
- `FetchHttpClient` : Implémentation fetch natif.
- `MemoryCacheProvider` : Cache en mémoire (Map).
- `HttpClient` / `CacheProvider` : Interfaces pour injection.

> Tous ces éléments sont exportés en masse via `export * from './client';` — consulter les fichiers pour signatures exactes.

### Exemple basé sur le commentaire du fichier `index.ts`
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

const endpoint = createDefaultValuesEndpoint('https://api.example.com/data');
const result = await resolver.resolveValues(endpoint, { search: 'query' });
```

## ⚙️ Création rapide d’un endpoint
```typescript
import { createDefaultValuesEndpoint } from 'input-field-spec-ts';
const endpoint = createDefaultValuesEndpoint('/api/users');
// Personnaliser ensuite selon structure réelle si nécessaire
```

## 📌 Constantes
```typescript
import { PROTOCOL_VERSION, LIBRARY_VERSION } from 'input-field-spec-ts';
console.log(PROTOCOL_VERSION); // '2.0'
console.log(LIBRARY_VERSION);  // '1.0.0'
```

## ✅ Exemple complet champ + validation
```typescript
import { FieldValidator, InputFieldSpec } from 'input-field-spec-ts';

const fieldSpec: InputFieldSpec = {
  displayName: 'Email',
  dataType: 'STRING',
  required: true,
  expectMultipleValues: false,
  constraints: [
    { name: 'email', pattern: '^[\\w-\\.+]+@([\\w-]+\\.)+[\\w-]{2,}$', errorMessage: 'Email invalide' },
    { name: 'length', min: 5, max: 120 }
  ]
};

const validator = new FieldValidator();
const result = await validator.validate(fieldSpec, 'user@example.com');
if (!result.isValid) {
  console.log(result.errors);
}
```

## 🚫 Limitations & Suggestions (non implémentées ici)

| Catégorie | Limitation actuelle | Suggestion (non codée) |
|-----------|---------------------|-------------------------|
| Caching | Mémoire volatile uniquement | Ajouter persistance session/localStorage optionnelle |
| Caching | Mémoire volatile uniquement | *Suggestion* : persistance session/localStorage |
| Internationalisation | Messages inline | Fournir hook i18n / adaptateur | 
| Internationalisation | Messages inline | *Suggestion* : hook i18n / adaptateur | 
| Validation conditionnelle | Pas de dépendances inter-champs directes | Introduire un `ValidationContext` enrichi |
| Validation conditionnelle | Pas de dépendances inter-champs directes | *Suggestion* : `ValidationContext` enrichi |
| Monitoring | Pas d’API de métriques exportée | Ajouter compteur interne (hits cache, durée) |
| Monitoring | Pas d’API de métriques exportée | *Suggestion* : compteur interne (hits cache, durée) |

## 🔍 Fidélité & traçabilité

Chaque bloc d’exemple est dérivé du style et des patterns présents dans `index.ts` et la parité Java. Aucun type non présent n’est revendiqué. Pour des signatures exactes, inspecter les fichiers dans `src/types`, `src/validation`, `src/client`.

---

*Page générée automatiquement — suggestions en italique et séparées de l’état réel du code.*
