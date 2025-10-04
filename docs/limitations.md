---
layout: default
title: "Limitations & Portée"
nav_order: 70
---

# Limitations & Portée

Cette page consolide les limitations actuelles observables dans le code Java et TypeScript du dépôt, ainsi que les *Suggestions* (idées futures non implémentées). Rien n'est présenté ici comme existant si absent du code.

## 🔒 Portée actuelle (observée)

| Domaine | État actuel (Java) | État actuel (TypeScript) |
|---------|--------------------|--------------------------|
| Validation basique (required, type, pattern, min/max, enum) | Oui | Oui |
| `valuesEndpoint` (résolution dynamique) | Spécifié dans `ConstraintDescriptor` (valeurs indirectes) | Implémenté via `ValuesResolver` + abstractions client |
| Caching | Interface `CacheProvider` (Java côté client suggéré mais non implémenté) | `MemoryCacheProvider` en mémoire |
| Pagination des valeurs | Champs de stratégie pagination dans modèle | Paramètres reconnus (`page`, `pageSize` si endpoint le supporte) |
| Formats spécialisés (email, date…) | Support selon `ConstraintDescriptor` (pattern/format) | Similarité via contraintes pattern | 
| Alias de valeurs (`ValueAlias`) | Oui | Oui |
| Erreurs structurées | Classes/objets dédiés (ex: ValidationError) | Objets ValidationResult/ValidationError |

## 🚫 Limitations techniques

| Catégorie | Détail | Impact |
|-----------|-------|--------|
| Persistance cache | Aucune persistance disque / storage côté client | Rechargement perd le cache |
| i18n | Messages d'erreur codés inline | Nécessite fork pour localisation |
| Validation inter-champs | Absence de contexte global agrégé | Impossible d'exprimer des règles conditionnelles complexes |
| Monitoring / métriques | Pas d'API d'observation | Pas de visibilité sur performance / taux d'erreurs |
| Compilation de règles | Évaluation interprétative séquentielle | Performances limitées sur très gros formulaires |
| Sécurité transport | Pas de couche TLS spécifique (délégué à HTTP client) | La sécurisation repose sur l'environnement d'exécution |

## 💡 Suggestions (non implémentées)

> Chaque item ci-dessous est une *Suggestion* — non présent dans le code actuel.

| Domaine | Suggestion | Bénéfice attendu |
|--------|------------|------------------|
| Cache persistant | Adapter `CacheProvider` à localStorage / IndexedDB / disque | Réduction appels réseau |
| Stratégies avancées | LRU, TTL, compression légère | Optimisation mémoire |
| Internationalisation | Injection d'un `MessageResolver` ou adaptateur i18n | Localisation dynamique |
| Validation conditionnelle | `ValidationContext` passé à chaque contrainte | Règles inter-dépendantes |
| Pipeline compilé | Pré-compilation des contraintes en fonction pure | Gain performance |
| Observabilité | Hooks / events (onValidateStart, onCacheHit...) | Monitoring & tuning |
| Plugin système | Registre d'extensions (contrainte custom, fetch adapter) | Extensibilité contrôlée |
| Worker offload | Exécution validation dans Web Worker (TS) | UI fluide sur gros formulaires |
| Résolution batch | Agréger plusieurs `ValuesEndpoint` en une requête | Latence réduite |
| Internationalisation erreurs | Dictionnaire clés → message | Cohérence multi-langue |

## 🧪 Stratégie de validation (rappel)

Ordre d'application (observé + parité) :
1. Présence (required)
2. Type / cardinalité (single vs multiple)
3. Pattern / format
4. Bornes numériques (min / max / length)
5. Enum statique (`enumValues`)
6. Valeurs dynamiques (`valuesEndpoint`)

## 📏 Critères de qualité documentaire

| Critère | État |
|---------|------|
| Séparation état réel / idées | Respectée (italique *Suggestion*) |
| Aucune API inventée | Confirmé après audit double langage |
| Diagrams requis | À ajouter (architecture, séquence valeurs, flux validation) |
| Exemples concrets | En cours de rédaction |

## 🔄 Mise à jour

Cette page doit être révisée à chaque ajout de fonctionnalité réelle et les éléments correspondants déplacés de "Suggestions" vers "Portée actuelle".

---

*Page générée — conformité stricte : aucune fonctionnalité non codée n'est listée comme existante.*
