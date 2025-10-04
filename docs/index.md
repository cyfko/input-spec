---
layout: default
title: "Accueil"
nav_order: 1
description: "Portail de navigation du Dynamic Input Field Specification Protocol"
permalink: /
---

## Pourquoi ce protocole ?

Les formulaires complexes souffrent souvent de duplication (front/back), d’incohérences de validation et de coûts d’évolution dès que les règles changent. Le **Dynamic Input Field Specification Protocol** vise à :

- Centraliser la description des champs (types, contraintes, valeurs dynamiques) dans une représentation partagée.
- Supprimer la divergence entre règles backend et logique frontend.
- Permettre l’injection de sources de valeurs dynamiques (pagination, recherche) sans recoder la logique UI.
- Réduire le couplage framework : une même spécification alimente plusieurs clients (web, mobile, CLI).
- Accélérer la mise à jour métier (ajout d’une contrainte = propagation immédiate aux clients).

Cas d’usage typiques : back‑offices modulaires, SaaS multi-tenant, formulaires hautement variables (RH, e‑commerce B2B, conformité), configurateurs dynamiques.

> Objectif principal : déplacer la complexité de *comment valider / peupler* vers *quoi décrire* (spécification déclarative).

# Documentation Complète - Input Field Specification Protocol

Bienvenue dans la documentation complète du **Dynamic Input Field Specification Protocol** ! 

Ce site vous guidera à travers tous les aspects du protocole, depuis vos premiers pas jusqu'aux techniques avancées d'implémentation.

> Note: Certaines sections avancées (optimisations extrêmes, plugins) sont marquées comme *Suggestion* lorsqu'elles ne sont pas encore implémentées dans le code présent du dépôt.

## 🔍 Perspectives

### Point de vue Client (C2)
- Charge les spécifications (`InputFieldSpec`) depuis le serveur.
- Applique validation locale (ordre pattern → min/max → format → enum/valuesEndpoint) sans inventer de logique.
- Résout les valeurs via endpoint (TypeScript: `ValuesResolver`; Java: implémentation future → Suggestion).

### Point de vue Serveur (C2)
- Expose endpoints fournissant les specs et sources de valeurs paginées.
- Centralise contraintes pour éliminer la duplication côté front.
- Reste source de vérité; aucune logique métier n'est inférée côté client.

### Interaction
```
Client -> GET /api/fields/<field>
Client -> (facultatif selon saisie) GET /api/users?search=...&page=1
Client -> Validation locale (FieldValidator)
```
Les appels sont minimisés (debounce + cache côté client quand disponible).

## 🧭 Navigation par objectif

### 💡 Découvrir le protocole
**Pour comprendre l'intérêt et les possibilités**

- [📖 Vue d'ensemble du protocole](./OVERVIEW.md) - Philosophie et abstraction du protocole
- [📋 README principal](../README.md) - Introduction et exemples rapides
- [❓ FAQ](./FAQ.md) - Questions fréquentes avec scénarios concrets

### 🚀 Commencer rapidement  
**Pour créer votre premier champ intelligent en 5 minutes**

- [🚀 Démarrage rapide](./QUICK_START.md) - Votre premier champ intelligent
- [📋 Spécification protocole](../PROTOCOL_SPECIFICATION.md) - Référence technique

### 🎓 Développer des formulaires complexes
**Pour maîtriser les fonctionnalités avancées**

- [🎓 Guide intermédiaire](./INTERMEDIATE_GUIDE.md) - Formulaires complexes et optimisations
- [💼 Exemples concrets](./FAQ.md#exemples-concrets) - Scénarios e-commerce et RH

### 🔧 Contribuer au projet
**Pour participer à l'évolution du protocole**

- [🔧 Guide expert](./EXPERT_GUIDE.md) - Architecture et développement avancé
- [🤝 Guide de contribution](./CONTRIBUTING.md) - Comment participer

## 🗺️ Plan de la documentation

```mermaid
graph TD
    A[📖 README] --> B{Votre niveau ?}
    
    B -->|Débutant| C[🚀 Démarrage<br/>rapide]
    B -->|Intermédiaire| D[🎓 Guide<br/>intermédiaire] 
    B -->|Expert| E[🔧 Guide<br/>expert]
    
    C --> F[❓ FAQ]
    D --> F
    E --> G[🤝 Contribution]
    
    F --> H[📋 Spécification<br/>protocole]
    G --> H
    
    style A fill:#e1f5fe
    style C fill:#f3e5f5
    style D fill:#fff3e0
    style E fill:#e8f5e8
    style F fill:#fce4ec
    style G fill:#fff8e1
    style H fill:#f1f8e9
```

## 🎯 Parcours recommandés

### 👨‍💻 Développeur front-end
1. [Vue d'ensemble](../README.md#vue-densemble) 
2. [Démarrage rapide](./QUICK_START.md) - TypeScript
3. [Guide intermédiaire](./INTERMEDIATE_GUIDE.md) - Intégrations frameworks
4. [FAQ](./FAQ.md) - Scénarios avancés

### 👩‍💻 Développeur back-end
1. [Spécification protocole](../PROTOCOL_SPECIFICATION.md)
2. [Guide expert](./EXPERT_GUIDE.md) - Implémentation serveur
3. [FAQ](./FAQ.md) - Endpoints et sécurité
4. [Contribution](./CONTRIBUTING.md) - Nouvelles implémentations

### 🏗️ Architecte système
1. [Architecture](./EXPERT_GUIDE.md#architecture-du-protocole)
2. [Cas d'usage](../README.md#cas-dusage-idéaux)
3. [FAQ](./FAQ.md) - Scénarios entreprise
4. [Roadmap](../README.md#roadmap)

### 🎨 Designer UX/UI
1. [Vue d'ensemble](../README.md#vue-densemble)
2. [Exemples concrets](./FAQ.md#exemples-concrets)
3. [Guide intermédiaire](./INTERMEDIATE_GUIDE.md) - Composants UI
4. [FAQ](./FAQ.md) - Expérience utilisateur

## 🛠️ Technologies disponibles

### Implémentations prêtes

| Technologie | Niveau | Guide | Statut |
|-------------|--------|-------|--------|
| **TypeScript** | Tous niveaux | [Démarrage rapide](./QUICK_START.md) | ✅ Stable |
| **Java** | Intermédiaire | [Guide expert](./EXPERT_GUIDE.md) | 🚧 Beta |

### Intégrations frameworks

| Framework | Guide | Exemples |
|-----------|-------|----------|
| **React** | [Guide intermédiaire](./INTERMEDIATE_GUIDE.md#react) | [Formulaire e-commerce](./FAQ.md#scénario-1-e-commerce) |
| **Vue.js** | [Guide intermédiaire](./INTERMEDIATE_GUIDE.md#vuejs) | [Application RH](./FAQ.md#scénario-2-application-rh) |
| **Angular** | [Guide intermédiaire](./INTERMEDIATE_GUIDE.md#angular) | [Système tickets](./FAQ.md#scénario-3-système-de-tickets) |

## 🧩 Diagrammes clés

### Architecture globale
```mermaid
graph LR
    subgraph Client
        UI[UI Form]
        VS[ValuesResolver]
        FV[FieldValidator]
        MC[(MemoryCache)]
    end
    subgraph Serveur
        API[HTTP Endpoint /values]
        SPEC[Endpoint /fields]
        DB[(Data Store)]
    end
    UI --> VS
    VS --> MC
    VS --> API
    API --> DB
    SPEC --> UI
    FV --> UI
```

### Séquence résolution de valeurs (TypeScript)
```mermaid
sequenceDiagram
    participant UI
    participant Resolver as ValuesResolver
    participant Cache as MemoryCacheProvider
    participant HTTP as FetchHttpClient
    participant Backend

    UI->>Resolver: resolveValues(endpoint, params)
    Resolver->>Cache: get(key)
    alt Hit
        Cache-->>Resolver: cached result
        Resolver-->>UI: return cached
    else Miss
        Resolver->>HTTP: fetch(url?query)
        HTTP->>Backend: GET /values?search=...
        Backend-->>HTTP: 200 JSON
        HTTP-->>Resolver: values[]
        Resolver->>Cache: set(key, values)
        Resolver-->>UI: values[]
    end
```

### Flux de validation d'un champ
```mermaid
flowchart TD
    A[Entrée utilisateur] --> B{Required?}
    B -- manquant --> E[Erreur required]
    B -- ok --> C{Type conforme?}
    C -- non --> F[Erreur type]
    C -- oui --> D[Contraintes séquentielles]
    D --> G[Pattern]
    G --> H[Min/Max]
    H --> I[Format]
    I --> J[Enum statique]
    J --> K[ValuesEndpoint]
    K --> L{Erreurs?}
    L -- oui --> M[Retour ValidationResult ko]
    L -- non --> N[ValidationResult ok]
```

## 📚 Ressources complémentaires

### 🔗 Liens externes
- [Repository GitHub](https://github.com/cyfko/input-spec) - Code source et issues
- [Package npm](https://www.npmjs.com/package/@cyfko/input-spec) - Installation TypeScript
- [Maven Central](https://search.maven.org/artifact/io.github.cyfko/input-spec) - Dépendance Java

### 📊 Communauté
- [GitHub Issues](https://github.com/cyfko/input-spec/issues) - Bugs et demandes
- [GitHub Discussions](https://github.com/cyfko/input-spec/discussions) - Questions et échanges
- [Pull Requests](https://github.com/cyfko/input-spec/pulls) - Contributions en cours

## 🆘 Besoin d'aide ?

### Questions fréquentes
- **"Quel niveau minimum requis ?"** → [Prérequis](./QUICK_START.md#prérequis)
- **"Compatible avec mon framework ?"** → [Intégrations](./INTERMEDIATE_GUIDE.md#intégrations-frameworks)
- **"Comment contribuer ?"** → [Guide de contribution](./CONTRIBUTING.md)

### Support
- 🐛 **Bugs** : [Créer une issue](https://github.com/cyfko/input-spec/issues/new?template=bug_report.md)
- 💡 **Idées** : [Créer une discussion](https://github.com/cyfko/input-spec/discussions/new?category=ideas)
- 📖 **Documentation** : [Améliorer cette page](./CONTRIBUTING.md#documentation)

---

## 🚦 Statut de la documentation

| Document | Dernière mise à jour | Statut | Contributeurs |
|----------|---------------------|--------|---------------|
| [README](../README.md) | Oct 2025 | ✅ À jour | Équipe core |
| [Démarrage rapide](./QUICK_START.md) | Oct 2025 | ✅ À jour | Équipe core |
| [Guide intermédiaire](./INTERMEDIATE_GUIDE.md) | Oct 2025 | ✅ À jour | Équipe core |
| [Guide expert](./EXPERT_GUIDE.md) | Oct 2025 | ✅ À jour | Équipe core |
| [FAQ](./FAQ.md) | Oct 2025 | ✅ À jour | Équipe core |
| [Contribution](./CONTRIBUTING.md) | Oct 2025 | ✅ À jour | Équipe core |

---

**Documentation générée avec ❤️ par la communauté**  
*Suggestions d'amélioration ? [Contribuez à cette page !](./CONTRIBUTING.md)*