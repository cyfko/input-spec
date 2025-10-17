# Appendix: Legacy v1 Specification (Deprecated)

> Ce document archive la spécification v1 pour référence historique. Toute nouvelle implémentation doit cibler la version 2.1 ou supérieure.

---

## 1. Introduction (v1)

Le protocole v1 définit une méthode agnostique pour spécifier dynamiquement les contraintes, sources de valeurs et règles de validation des champs de saisie.

### Objectifs v1
- Définir dynamiquement les spécifications de champs
- Comprendre les contraintes et sources de valeurs sans hardcoding
- Permettre l’auto-complétion et la validation côté client
- Supporter la recherche et la pagination côté serveur
- Maintenir l’interopérabilité multi-langages

---

## 2. Entités principales (v1)

### 2.1 InputFieldSpec
- `displayName`, `description`, `dataType`, `expectMultipleValues`, `required`, `constraints[]`
- Contraintes polymorphes : `min`, `max`, `pattern`, `format`, `enumValues`, `valuesEndpoint`

### 2.2 ConstraintDescriptor
- Un seul objet peut contenir plusieurs règles (min, max, pattern, etc.)
- `enumValues` et/ou `valuesEndpoint` imbriqués dans la contrainte

### 2.3 ValuesEndpoint (v1)
- Protocoles : `HTTPS`, `HTTP`, `GRPC`
- Champs : `uri`, `method`, `searchField`, `paginationStrategy`, `responseMapping`, `requestParams`, `cacheStrategy`, `debounceMs`, `minSearchLength`

### 2.4 Exemples v1

**Champ texte avec contrainte de longueur :**
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
      "description": "Username (3-20 alphanum)",
      "errorMessage": "Username must be 3-20 characters, alphanumeric with underscores"
    }
  ]
}
```

**Champ select statique :**
```json
{
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": true,
  "constraints": [
    {
      "name": "status",
      "enumValues": [
        { "value": "ACTIVE", "label": "Active" },
        { "value": "INACTIVE", "label": "Inactive" }
      ]
    }
  ]
}
```

**Champ avec valeurs distantes :**
```json
{
  "dataType": "STRING",
  "expectMultipleValues": false,
  "required": false,
  "constraints": [
    {
      "name": "assignee",
      "valuesEndpoint": {
        "protocol": "HTTPS",
        "uri": "/api/users",
        "searchField": "name",
        "paginationStrategy": "PAGE_NUMBER",
        "responseMapping": { "dataField": "data" },
        "requestParams": { "pageParam": "page", "limitParam": "limit", "searchParam": "search", "defaultLimit": 50 }
      }
    }
  ]
}
```

---

## 3. Limites et évolutions
- Modèle composite peu extensible
- Enum statique et valeurs distantes non unifiées
- Pipeline de validation implicite
- Difficulté de migration vers des modèles atomiques ou dynamiques

---

Pour migrer vers v2, voir `MIGRATION_V1_V2.md`.
