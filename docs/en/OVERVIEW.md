layout: default
title: Overview
nav_order: 2
description: "Protocol overview, objectives, and comparison with existing solutions."



[🇫🇷 French](../OVERVIEW.md) | [🇬🇧 English](./OVERVIEW.md)


# Dynamic Input Field Specification Protocol v2.1

*The modern standard for smart, adaptive, and interoperable form fields*



## 🚩 Why is this protocol unique?

The **Dynamic Input Field Specification Protocol v2.1** finally solves the fragmentation of dynamic forms:

- 🔥 **Total standardization**: a single structure to describe constraints, values, validation, and behavior
- 🧩 **Interoperability**: frontend and backend speak the same language, no duplication
- 🛠️ **Advanced search**: native support for multi-criteria search, pagination, caching, debouncing
- 🧑‍💻 **Extensible and agnostic**: not tied to any framework or technology
- 🏗️ **Atomic constraints**: each rule is independent, clear, and traceable



## 🔍 What is this protocol?

A **universal specification** to dynamically describe every form field:

- Complete metadata (label, type, help, etc.)
- Atomic constraints (regex, min/max, custom, etc.)
- Value sources (static or remote, paginated, filtered)
- Ordered, deterministic validation pipeline
- Advanced search via `searchParams` and `searchParamsSchema` (JSON Schema)


### General Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        UI[🎨 User Interface]
        VALID[✅ Validation Engine]
        CACHE[💾 Local Cache]
    end
    
    subgraph "Protocol Core"
        SPEC[📋 InputFieldSpec]
        CONST[🔒 ConstraintDescriptor]
        ENDPOINT[🌐 ValuesEndpoint]
    end
    
    subgraph "Server Layer"
        API[🔌 Spec API]
        VALUES[📊 Data Sources]
        META[📝 Metadata]
    end
    
    UI --> SPEC
    VALID --> CONST
    CACHE --> ENDPOINT
    SPEC --> API
    CONST --> META
    ENDPOINT --> VALUES
    
    classDef client fill:#e1f5fe
    classDef protocol fill:#f3e5f5
    classDef server fill:#e8f5e8
    
    class UI,VALID,CACHE client
    class SPEC,CONST,ENDPOINT protocol
    class API,VALUES,META server
```


## 🚫 What we are NOT

- ❌ A form framework (React, Angular, Vue...)
- ❌ A data validator (Joi, Yup, Zod...)
- ❌ An imposed SDK or library

> **We are** a **universal protocol**: it describes, it does not code. It lets your tools, frameworks, and languages collaborate without friction or duplication.

## ✅ Concrete value added

### For frontend developers
- **No more copy-paste** of validation logic between projects
- **Self-adaptive forms** based on server metadata
- **Real-time validation** with debouncing and automatic caching
- **Generic components** reusable across projects

### For backend developers
- **Single source of truth** for validation constraints
- **Uniform API** to expose field metadata
- **Scalability** without breaking existing clients
- **Simple integration** with your existing endpoints

### For teams
- **Drastic reduction** in duplicated code maintenance
- **Automatic consistency** between frontend and backend
- **Easier onboarding** with standard patterns
- **Faster time-to-market** for new forms
- **Time-to-market réduit** pour les nouveaux formulaires


## 🎪 Démonstration avancée (v2.1)

**Exemple : Recherche multi-critères sur un champ produit**

**🖥️ Côté serveur** - Spécification du champ :
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

**💻 Côté client** - Adaptation automatique :
```typescript
const ProductField = ({ fieldSpec }) => (
    <SmartSelectField spec={fieldSpec} /> // Recherche multi-critères, pagination, validation pipeline
)
```

**🔄 Flux d'interaction** :
```mermaid
sequenceDiagram
        participant U as Utilisateur
        participant C as Client
        participant S as Serveur
        U->>C: Saisit "chaise" + sélectionne "mobilier"
        C->>S: POST /api/products {name: "chaise", category: "mobilier", page:1, limit:10}
        S->>C: {results: [{value:"prod_001", label:"Chaise design"}]}
        C->>U: Affiche "Chaise design"
        U->>C: Sélectionne produit
        C->>C: Valide: prod_001 ✅
```

## 🚀 Pour qui est-ce fait ?

### ✅ Vous devriez considérer ce protocole si :
- Vous développez des applications avec **beaucoup de formulaires**
- Vous voulez **réduire la duplication** entre front et back
- Vous cherchez à **standardiser** vos patterns de validation
- Vous construisez des **systèmes multi-clients** (web, mobile, API)
- Vous voulez des **formulaires adaptatifs** et configurables

### ❌ Ce protocole n'est probablement pas pour vous si :
- Votre application a **moins de 5 formulaires** au total
- Vous préférez **tout contrôler manuellement** côté front
- Vos formulaires sont **ultra-spécifiques** sans patterns communs
- Vous n'avez **pas le contrôle du back-end**

## 📚 Commencer maintenant

### Pour les pressés (5 minutes)
👉 [Guide de démarrage rapide](./QUICK_START.md)

### Pour l'intégration (30 minutes)  
👉 [Guide intermédiaire](./INTERMEDIATE_GUIDE.md)

### Pour maîtriser le protocole (2 heures)
👉 [Guide expert](./EXPERT_GUIDE.md)

## 🏗️ Implémentations disponibles

| Langage | Status | Validation | Recherche avancée | Client HTTP | Cache | Tests |
|---------|--------|------------|-------------------|-------------|--------|--------|
| **TypeScript** | ✅ Stable | ✅ Complète | ✅ searchParams | ✅ Fetch/Axios | ✅ Mémoire | ✅ Jest |
| **Java** | ✅ Stable | ✅ Complète | ✅ searchParams | 🚧 En cours | 🚧 En cours | ✅ JUnit |
| **Python** | 📋 Planifié | - | - | - | - | - |
| **C#** | 📋 Planifié | - | - | - | - | - |

## 🔧 Écosystème et intégrations

```mermaid
graph LR
    subgraph "Frameworks Front"
        REACT[React]
        VUE[Vue.js]
        ANGULAR[Angular]
        SVELTE[Svelte]
    end
    
    subgraph "Core Protocol"
        PROTO[Input Spec Protocol]
    end
    
    subgraph "Frameworks Back"
        SPRING[Spring Boot]
        EXPRESS[Express.js]
        DJANGO[Django]
        NEST[NestJS]
    end
    
    REACT --> PROTO
    VUE --> PROTO
    ANGULAR --> PROTO
    SVELTE --> PROTO
    
    PROTO --> SPRING
    PROTO --> EXPRESS
    PROTO --> DJANGO
    PROTO --> NEST
    
    classDef frontend fill:#61dafb20
    classDef protocol fill:#9c27b020
    classDef backend fill:#4caf5020
    
    class REACT,VUE,ANGULAR,SVELTE frontend
    class PROTO protocol
    class SPRING,EXPRESS,DJANGO,NEST backend
```


## 🗺️ Feuille de route

### ✅ Version 2.1 (Actuelle)
- ✅ Recherche avancée multi-critères (`searchParams`, `searchParamsSchema`)
- ✅ Atomicité des contraintes
- ✅ Documentation exhaustive et guides
- ✅ Implémentations TypeScript & Java

### 🚧 Version 2.2 (En cours)
- 🚧 Client HTTP Java complet
- 🚧 Système de cache Java
- 🚧 Adaptateurs React/Vue/Svelte
- 🚧 Métriques de performance

### 📋 Versions futures
- 📋 Support des validations conditionnelles
- 📋 Internationalisation native
- 📋 Validation côté serveur intégrée
- 📋 SDK Python et C#

## 🤝 Contribuer

Ce projet évolue grâce aux retours des développeurs qui l'utilisent en production. 

**Types de contributions recherchées :**
- 🐛 **Bugs** et cas d'usage non couverts
- 💡 **Améliorations** du protocole (rétrocompatibles)
- 🔌 **Adaptateurs** pour nouveaux frameworks
- 📖 **Documentation** et guides d'intégration
- 🧪 **Tests** et exemples concrets

👉 [Guide de contribution](./CONTRIBUTING.md)

## 📊 Adoption

Utilisé en production par :
- *(En cours de collecte des retours d'adoption)*

Témoignages :
- *(À venir avec les premiers utilisateurs)*

---

**🔗 Liens rapides**
- 📋 [Spécification complète du protocole](../PROTOCOL_SPECIFICATION.md)
- 🚀 [Démarrage rapide](./QUICK_START.md)
- 🎓 [Exemples TypeScript](../impl/typescript/examples/)
- ☕ [Exemples Java](../impl/java/src/test/java/)
- 🤔 [FAQ](./FAQ.md)
- 💬 [Discussions](../../discussions)

*Dernière mise à jour : Octobre 2025 (v2.1)*