---
layout: default
title: Notes d'Intégration
nav_order: 6
description: "Intégration Frontend (Angular · React · Vue · Svelte)."
nav_exclude: true
---
[🇫🇷 Français](./IMPLEMENTATION_NOTES.md) | [🇬🇧 English](./en/IMPLEMENTATION_NOTES.md)
# Notes d’implémentation (Extensions non normatives)

> Légende des tags : 🔧 Extension non normative | ⚠️ Divergence à résoudre | 🚀 Optimisation / Performance | 🧩 Compatibilité | 📦 Comportement normatif

Cette page complète la spécification protocolaire (normative) en décrivant les **extensions**, optimisations et divergences mineures propres aux implémentations de référence.

> Rien ici n’est requis par le protocole. Les clients tiers peuvent ignorer ou ré‑implémenter différemment ces aspects.

## 1. Vue synthétique
| Domaine | TypeScript | Java | Normatif ? | Tag | Commentaire |
|---------|-----------|------|------------|-----|-------------|
| Legacy v1 adapter | Oui | Non | Non | 🔧 | Traduction automatique composites → atomiques |
| Coercion souple | Oui | Partiel (manuelle) | Non | 🔧 | Nombre, booléen, date depuis chaînes |
| Short‑circuit validation | Non (agrège) | Optionnel | Non | 🔧🚀 | Arrêt sur 1ère erreur pour performance |
| Membership remote paresseux | Oui | Oui | Oui | 📦 | Ignore membership si domaine non résolu encore |
| Cache valeurs mémoire | Oui | Non | Non | 🚀 | Interface pluggable côté TS |
| Debounce résolution | Oui (hint) | Non | Hint | 🚀 | Basé sur `debounceMs` de l’endpoint |
| Normalisation dates | ISO / epoch acceptés | Doit être valide Java date | Non | 🔧 | TS tente parse multiple formats |
| minLength/maxLength single valeur | Appliqué | Ignoré (actuel) | Oui (devrait s’appliquer) | ⚠️ | Divergence à corriger |

## 2. Adaptateur Legacy v1 (TypeScript)
- Détecte composites (présence de plusieurs clefs reconnues dans une contrainte).
- Génère la liste ordonnée de contraintes atomiques équivalentes.
- Marque les objets transformés pour éviter double adaptation.
- Loggue (optionnel) un avertissement de dépréciation.

## 3. Coercion (TypeScript)
| Cible | Entrées acceptées | Stratégie |
|-------|-------------------|-----------|
| NUMBER | string numérique, number | `parseFloat` + fin de chaîne valide |
| BOOLEAN | 'true'/'false' (case-insensitive), bool | Mapping direct |
| DATE | ISO 8601, timestamp ms | `new Date()` + validation `!isNaN` |

Désactivable via options du `FieldValidator` (non normatif).

## 4. Pipeline de validation
Ordre implémenté (identique protocole) :
1. Required / cardinalité
2. Type & coercion éventuelle
3. Membership (si domaine résolu ou inline)
4. Contraintes atomiques (ordre de déclaration)

Agrégation d’erreurs (TS) vs short‑circuit (Java optionnel). Les deux restent conformes tant que l’ordre logique est préservé.

## 5. Gestion du domaine de valeurs
- `mode: CLOSED` : membership strict si valeurs résolues.
- `mode: SUGGESTIONS` : membership peut être sautée (les valeurs externes sont tolérées) — la validation métier serveur peut renforcer.
- Résolution asynchrone : si l’endpoint n’a pas encore été interrogé, la validation ne bloque pas l’utilisateur (TS & Java docs d’usage).

## 6. Divergences connues
| Sujet | Statut | Plan | Tag |
|-------|--------|------|-----|
| Java ignore minLength/maxLength single | Ouvert | Aligner sur TS dans 2.0.x | ⚠️ |
| Messages d’erreur non uniformisés multi-langues | Attendu | Introduire i18n dans 2.x | 🔧 |
| Absence de `collectionSize` dédiée | Limitation | Ajout prévu 2.1.0 | 🔧 |

## 7. Performance
- Cache mémoire simple clé hashée (endpoint + paramètres).
- Debounce côté client avant requêtes search.
- Possibilité future : cache persistent + stratégie LRU.

## 8. Extension de contraintes custom
Contrat minimal attendu pour une contrainte custom :
```json
{ "name": "business", "type": "custom", "params": { "kind": "VIP_TIER" }, "errorMessage": "Client non VIP" }
```
Le validateur délègue à un resolver enregistré pour `custom`.

## 9. Tests de conformité
Pour une implémentation tierce :
- Rejouer jeux de données (valid/invalid) par type de contrainte.
- Vérifier ordre des erreurs si agrégation.
- Simuler latence endpoint (membership paresseux).

## 10. Futurs ajouts documentés
| Idée | Justification |
|------|---------------|
| JSON Schema export | Interop outillage formulaire | 
| Mode strict remote | Forcer échec si domaine non résolu | 
| i18n message bundle | Expérience multi-locale | 

## 11. Références croisée
- Spécification: `../PROTOCOL_SPECIFICATION.md`
- Migration: `./MIGRATION_V1_V2.md`

---
Dernière mise à jour : Octobre 2025
