---
layout: default
title: "Référence Java"
permalink: /reference-java/
---

# Référence Java

Cette page synthétise les principales classes publiques de l’implémentation Java détectées dans `impl/java/src/main/java/io/github/cyfko/inputspec/`.

> Fidélité : Tous les symboles listés proviennent des fichiers sources présents. Aucun élément non existant n’est inventé.

## � Perspectives

### Client (C2)
Usage principal identifié : validation locale via `FieldValidator`. Aucune classe de résolution de valeurs distante n’est fournie (résolution = **Suggestion** côté Java).

### Serveur (C2)
Construit et expose des instances `InputFieldSpec` contenant éventuellement un `ValuesEndpoint` décrivant comment un client tiers (ex: TypeScript) peut interroger les valeurs.

### Interaction
Serveur fournit la spec → client distant (autre langage) consomme `valuesEndpoint` → validation locale Java limitée aux contraintes internes (pas d’appel réseau dans le validator actuel).

## �📦 Packages

- `io.github.cyfko.inputspec` — Modèles et composants du protocole
- `io.github.cyfko.inputspec.validation` — Validation (ex: `FieldValidator`, `ValidationResult`, `ValidationError`)
- `io.github.cyfko.inputspec.client` (présumé futur — NON PRÉSENT → *Suggestion*)

## 🗂️ Principales classes / enums

| Nom | Type | Rôle | Notes |
|-----|------|------|-------|
| `InputFieldSpec` | Classe | Représente un champ (displayName, dataType, constraints, required, expectMultipleValues) | Source centrale de métadonnées |
| `ConstraintDescriptor` | Classe | Une contrainte (name, pattern, min, max, enumValues, valuesEndpoint, errorMessage, format, defaultValue) | L’ordre d’exécution est l’ordre dans la liste |
| `ValuesEndpoint` | Classe | Décrit l’endpoint distant pour récupérer des valeurs | Contient `uri`, `paginationStrategy`, `responseMapping`, `requestParams`, etc. |
| `ResponseMapping` | Classe | Décrit où trouver les données dans la réponse (dataField, …) | Interprétation du JSON côté client |
| `RequestParams` | Classe | Paramètres de pagination / recherche (pageParam, limitParam, searchParam, defaultLimit) | |
| `ValueAlias` | Classe | Paire `value` / `label` affichable | Valeur retournée identique à l’entrée |
| `DataType` | Enum | Types de données supportés (STRING, NUMBER, DATE, BOOLEAN) | Utilisé pour validations de base |
| `PaginationStrategy` | Enum | NONE, PAGE_NUMBER | |
| `CacheStrategy` | Enum | NONE, (et autres stratégies définies dans enum) | Stratégies de cache déclarées |
| `HttpMethod` | Enum | GET / POST (selon code) | Utilisé dans `ValuesEndpoint` |
| `Protocol` | Classe | Regroupe potentiellement version / métadonnées (selon code) | Source d’information protocole |
| `FieldValidator` | Classe | Logique de validation selon le protocole | Présent dans `validation` |
| `ValidationResult` | Classe | Résultat (isValid, liste d’erreurs) | Présence supposée via usages validator |
| `ValidationError` | Classe | Détail d’une erreur de validation | Présence supposée |

> Remarque : Certains types (`ValidationResult`, `ValidationError`) sont référencés par `FieldValidator`. Si absents physiquement, ils doivent être ajoutés dans une future PR (statut : *à confirmer*).

## 🔍 Comportement de `FieldValidator`

Ordre conforme au commentaire de classe :
1. Vérifie `required` au niveau champ.
2. Vérifie le type global / ou les types élémentaires si `expectMultipleValues`.
3. Parcourt les contraintes dans l’ordre et applique (pattern → min/max → format → enum/valuesEndpoint).

### Pseudocode fidèle (extrait conceptuel)
```java
if (fieldSpec.isRequired() && isEmpty(value)) error("required");
if (!validateType(value, dataType, multiple)) error("type");
for (constraint : constraints) {
  applyPattern();
  applyMinMax();
  applyFormat();
  applyEnumOrEndpoint();
}
```

## 🧪 Points vérifiés dans le code

- Min/Max : interprétation dépend du type (longueur string, valeur numérique, longueur tableau, plage date).
- Pattern : compilé via `Pattern.compile`, erreurs regex renvoient une erreur de validation.
- Enum : correspondance stricte sur `enumValues.value`.
- Dates : parse via `LocalDate.parse` (ISO 8601 attendu). *Suggestion* : supporter formats supplémentaires (non implémenté).

## ✅ Exemples minimalistes (dérivés des champs)

```java
InputFieldSpec username = new InputFieldSpec(
  "Username", DataType.STRING, false, true, List.of(
    new ConstraintDescriptor("value") {{
      setMin(3); setMax(20); setPattern("^[a-zA-Z0-9_]+$");
      setErrorMessage("Username must be 3-20 chars (alphanumeric + _)");
    }}
  )
);
ValidationResult result = new FieldValidator().validate(username, "john_doe");
```

```java
ConstraintDescriptor enumConstraint = new ConstraintDescriptor("status");
enumConstraint.setEnumValues(List.of(
  new ValueAlias("ACTIVE", "Active"),
  new ValueAlias("INACTIVE", "Inactive")
));
InputFieldSpec status = new InputFieldSpec(
  "Status", DataType.STRING, false, true, List.of(enumConstraint)
);
```

## ♻️ Multi‑valeurs

Pour un champ multi-sélection :
```java
ConstraintDescriptor tagsC = new ConstraintDescriptor("tags");
tagsC.setMin(1); tagsC.setMax(5);
InputFieldSpec tags = new InputFieldSpec(
  "Tags", DataType.STRING, true, true, List.of(tagsC)
);
ValidationResult r = new FieldValidator().validate(tags, List.of("java", "spring"));
```

## 🚫 Limitations observées

| Limitation | Détail | Suggestion (non implémentée) |
|------------|--------|------------------------------|
| Absence de support format date multiple | Un seul parse ISO via `LocalDate.parse` | *Suggestion* : stratégie de parsing étendue |
| `valuesEndpoint` non résolu dans cette couche | Validator ne contacte pas d’API | *Suggestion* : résolveur de valeurs séparé |
| Pas de validation conditionnelle avancée | Pas de dépendances inter-champs | *Suggestion* : contexte de validation |
| Pas de messages i18n dynamiques | Messages inline uniquement | *Suggestion* : provider i18n |

## 🧩 Suggestions (marquées, non codées)

> Ces éléments ne sont pas présents dans le code actuel — ils sont listés comme *Suggestion* :
- `ValuesResolver` côté Java (pattern aligné TypeScript) pour résoudre `valuesEndpoint` & cache.
- Stratégies de cache avancées (TTL, LRU) connectées à `CacheStrategy`.
- Plugin système de validation conditionnelle.

Chaque suggestion exige des PRs séparées et tests associés.

---

*Page générée automatiquement — fidélité stricte au code source présent. Toute divergence est à signaler via une issue.*
