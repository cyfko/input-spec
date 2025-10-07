# Dynamic Input Field Specification Protocol (v2.0.0)

*Protocole agnostique pour décrire, valider et alimenter dynamiquement des champs de saisie – version 2 unifiée (atomic constraints + domaine de valeurs centralisé).* 

[![Spec Docs](https://img.shields.io/badge/spec-v2.0.0-blue)](./PROTOCOL_SPECIFICATION.md)
[![TypeScript Library](https://img.shields.io/badge/TypeScript-2.0.0-3178c6)](./impl/typescript/)
[![Java Library](https://img.shields.io/badge/Java-2.0.0-ed8b00)](./impl/java/)
[![License](https://img.shields.io/badge/license-MIT-green)](./LICENSE)
[![Contributions](https://img.shields.io/badge/contributions-welcome-brightgreen)](./docs/CONTRIBUTING.md)
[![CI Status](https://img.shields.io/badge/build-passing-success)](#)
[![Migration Guide](https://img.shields.io/badge/migration-v1→v2-orange)](./docs/MIGRATION_V1_V2.md)
[![Impl Notes](https://img.shields.io/badge/impl-notes-informational)](./docs/IMPLEMENTATION_NOTES.md)

> La branche principale reflète la spécification **v2**. Le modèle v1 (composite constraints, `enumValues`) est **déprécié**. Voir `docs/MIGRATION_V1_V2.md`.

## 🎯 Vue d'ensemble

Définissez côté serveur les champs (métadonnées, contraintes, domaine de valeurs) et laissez les clients appliquer une validation déterministe et fournir une UX riche (autocomplete, pagination, filtrage) sans logique dupliquée.

### 🔍 Pourquoi pas simplement JSON Schema / Zod / Yup ?
| Besoin | Ce projet | JSON Schema | Zod / Yup |
|--------|-----------|-------------|-----------|
| Domaine de valeurs dynamique (search + pagination) | ✅ Modèle unifié `valuesEndpoint` | ❌ (extensions ad-hoc) | ❌ (logique code) |
| Mode suggestions vs fermé | ✅ `mode: SUGGESTIONS/CLOSED` | ❌ | ❌ |
| Pipeline inter-langages normatif | ✅ Spécifié | Partiel (validation générique) | ❌ (exécution runtime locale) |
| Migration versionnée protocolaire | ✅ Pages dédiées | ⚠️ (schéma évolutif, pas pipeline) | ❌ |
| Séparation norme / impl extensions | ✅ (Impl Notes) | ❌ | ❌ |

> Ce protocole complète plutôt qu’il ne remplace ces outils : vous pouvez générer plus tard un JSON Schema dérivé pour du gating API.

### Le problème résolu

```typescript
// ❌ Avant : Logique dupliquée et incohérente
const validateEmailA = (email:string) => /^[^@]+@[^@]+\.[^@]+$/.test(email);
const validateEmailB = (email:string) => email.includes('@'); // Différent !

// ✅ Après : Spécification centrale
const emailFieldSpec = {
  displayName: 'Email', dataType: 'STRING', required: true,
  constraints: [{ name: 'pattern', type: 'pattern', params: { regex: '^[^@]+@[^@]+\\.[^@]+$' } }]
};
```

## ✨ Fonctionnalités clés (v2)

| Domaine | Description | Statut |
|---------|-------------|--------|
| Modèle unifié | Champ = métadonnées + contraintes atomiques + `valuesEndpoint` | Stable |
| Domaine de valeurs | INLINE ou endpoint (pagination + recherche + mode CLOSED/SUGGESTIONS) | Stable |
| Pipeline validation | REQUIRED → TYPE → MEMBERSHIP → CONTRAINTES ordonnées | Stable |
| Erreurs structurées | Nom de contrainte + message + index multi | Stable |
| Legacy adapter | Traduction v1 → v2 (TS uniquement) | Stable (déprécié) |
| Coercion douce | Conversion nombre, booléen, date epoch (TS) | Extension |
| Short‑circuit | Arrêt sur première erreur (Java) | Extension |
| Hints performance | `debounceMs`, stratégies cache côté client | Stable |
| Extensibilité | `custom` + futurs types | Stable |

> Les éléments "Extension" ne sont pas normatifs (hors cœur protocole) et sont documentés dans `docs/IMPLEMENTATION_NOTES.md`.

## 🚀 Exemple rapide (v2 pur)

### 1. Spécification d’un champ (serveur)
```json
{
  "displayName": "Assigné à",
  "description": "Sélection utilisateur (autocomplete)",
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "valuesEndpoint": {
    "protocol": "HTTPS",
    "uri": "/api/users",
    "searchField": "name",
    "paginationStrategy": "PAGE_NUMBER",
    "mode": "CLOSED",
    "debounceMs": 300,
    "responseMapping": { "dataField": "data" },
    "requestParams": { "pageParam": "page", "limitParam": "limit", "searchParam": "q", "defaultLimit": 20 }
  },
  "constraints": [
    { "name": "syntax", "type": "pattern", "params": { "regex": "^[A-Za-z0-9_]+$" }, "errorMessage": "Identifiant invalide" }
  ]
}
```

### 2. Validation côté client (TypeScript)
```typescript
import { FieldValidator } from '@cyfko/input-spec';
const validator = new FieldValidator();
const result = await validator.validate(fieldSpec, selectedUserId);
if(!result.isValid) console.log(result.errors);
```

### 💡 Hello World minimal
```typescript
import { FieldValidator } from '@cyfko/input-spec';
const spec = {
  displayName: 'Code pays', dataType: 'STRING', required: true,
  expectMultipleValues: false,
  constraints: [{ name: 'iso', type: 'pattern', params: { regex: '^[A-Z]{2}$' }, errorMessage: 'Format ISO2 requis' }]
};
const validator = new FieldValidator();
console.log(await validator.validate(spec, 'FR').isValid); // true
console.log(await validator.validate(spec, 'France').errors[0]); // { constraintName: 'iso', message: 'Format ISO2 requis' }
```

### 🧾 Exemple de ValidationResult (multi-valeurs)
```json
{
  "isValid": false,
  "errors": [
    { "constraintName": "iso", "message": "Format ISO2 requis", "index": 1 },
    { "constraintName": "iso", "message": "Format ISO2 requis", "index": 3 }
  ]
}
```

### 3. Résolution des valeurs (autocomplete)
```typescript
import { ValuesResolver, FetchHttpClient, MemoryCacheProvider } from '@cyfko/input-spec';
const resolver = new ValuesResolver(new FetchHttpClient(), new MemoryCacheProvider());
const { values } = await resolver.resolveValues(fieldSpec.valuesEndpoint!, { search: 'john', page: 1 });
```

## 📚 Documentation

| Niveau | Guide | Contenu |
|--------|-------|---------|
| Débutant | `docs/QUICK_START.md` | Premier champ et validation |
| Intermédiaire | `docs/INTERMEDIATE_GUIDE.md` | Composition, multi-champs |
| Expert | `docs/EXPERT_GUIDE.md` | Architecture interne |

### 📖 Référence

- Spécification : `PROTOCOL_SPECIFICATION.md`
- Migration v1→v2 : `docs/MIGRATION_V1_V2.md`
- Notes implémentation : `docs/IMPLEMENTATION_NOTES.md`
- FAQ : `docs/FAQ.md`
- Contribution : `docs/CONTRIBUTING.md`

## 🛠️ Implémentations

### TypeScript / JavaScript
```bash
npm install @cyfko/input-spec
```

### Java (Maven)
```xml
<dependency>
  <groupId>io.github.cyfko</groupId>
  <artifactId>input-spec</artifactId>
  <version>2.0.0</version>
</dependency>
```

### Statut

| Langage | Validation | Résolution valeurs | Cache | Tests | Statut |
|---------|------------|--------------------|-------|-------|--------|
| TypeScript | ✅ (atomic, membership) | ✅ (INLINE + remote hints) | ✅ mémoire | ✅ | Stable |
| Java | ✅ (atomic) | Partiel (remote hors scope) | 🚧 | ✅ | Beta |
| Python | Planifié | - | - | - | Backlog |
| C# | Planifié | - | - | - | Backlog |

## 🎪 Exemples

- Formulaire complet : `impl/typescript/examples/complete-form.ts`
- Valeurs dynamiques : `impl/typescript/examples/dynamic-values.ts`
- FAQ scénarios : `docs/FAQ.md`

## 🏗️ Architecture (vue conceptuelle)

```mermaid
graph TB
  SPEC[Spécifications Champs] --> VALID[Validation]
  SPEC --> VALUES[Résolution Valeurs]
  VALUES --> CACHE[Cache]
  VALID --> ERR[Résultats Structurés]
```

## 🎯 Cas d'usage

Parfait pour : multi-formulaires, multi-clients, validation métier riche, configuration dynamique. Moins utile pour micro-apps statiques.

## 🚀 Roadmap (post 2.0.0)

| Version | Objectifs | Statut |
|---------|-----------|--------|
| 2.0.x | Corrections mineures, alignement Java (minLength single) | En cours |
| 2.1.0 | Génération JSON Schema, membership strict remote, `collectionSize` | Planifié |
| 2.x | I18n messages, contraintes email/uuid natives | Backlog |
| 3.0.0 | Retrait legacy adapter v1 (TS) | Prévision |

## 🤝 Contribution

Guide : `docs/CONTRIBUTING.md` – tests, implémentations supplémentaires, exemples réels bienvenus.

## 📊 Observabilité communautaire

- Stars / Forks / Issues : onglets GitHub
- Contributions prioritaires : tests de conformité multi-langages, adaptateurs frameworks

## 📄 Licence

Licence MIT – voir `LICENSE`.

## 🔗 Liens

- Doc site : https://cyfko.github.io/input-spec/
- Issues : ../../issues
- Discussions : ../../discussions
- Releases : ../../releases
- Changelog TypeScript : `impl/typescript/CHANGELOG.md`
- Changelog Java : `impl/java/CHANGELOG.md`

**Fait avec ❤️ par la communauté**

**Version protocole**: 2.0.0 • **Dernière mise à jour**: Octobre 2025