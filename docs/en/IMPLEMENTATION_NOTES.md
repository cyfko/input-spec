---
layout: default
title: Implementation Notes
nav_order: 6
description: "Intégration Frontend (Angular · React · Vue · Svelte)."
---
[🇫🇷 Français](./IMPLEMENTATION_NOTES.md) | [🇬🇧 English](./en/IMPLEMENTATION_NOTES.md)

# Implementation Notes (Non-normative Extensions)

> Tag legend: 🔧 Non-normative extension | ⚠️ Divergence to resolve | 🚀 Optimization / Performance | 🧩 Compatibility | 📦 Normative behavior

This page complements the protocol specification (normative) by describing **extensions**, optimizations, and minor divergences specific to reference implementations.

> Nothing here is required by the protocol. Third-party clients may ignore or re-implement these aspects differently.

## 1. Summary Table
| Domain | TypeScript | Java | Normative? | Tag | Comment |
|--------|-----------|------|------------|-----|---------|
| Legacy v1 adapter | Yes | No | No | 🔧 | Automatic translation composites → atomics |
| Flexible coercion | Yes | Partial (manual) | No | 🔧 | Number, boolean, date from strings |
| Short-circuit validation | No (aggregates) | Optional | No | 🔧🚀 | Stop at first error for performance |
| Lazy remote membership | Yes | Yes | Yes | 📦 | Ignores membership if domain not yet resolved |
| In-memory value cache | Yes | No | No | 🚀 | Pluggable interface on TS side |
| Debounce resolution | Yes (hint) | No | Hint | 🚀 | Based on `debounceMs` from endpoint |
| Date normalization | ISO / epoch accepted | Must be valid Java date | No | 🔧 | TS tries to parse multiple formats |
| minLength/maxLength single value | Applied | Ignored (current) | Yes (should apply) | ⚠️ | Divergence to fix |

## 2. Legacy v1 Adapter (TypeScript)
- Detects composites (presence of multiple recognized keys in a constraint).
- Generates the ordered list of equivalent atomic constraints.
- Marks transformed objects to avoid double adaptation.
- Optionally logs a deprecation warning.

## 3. Coercion (TypeScript)
| Target | Accepted Inputs | Strategy |
|--------|----------------|----------|
| NUMBER | numeric string, number | `parseFloat` + valid end of string |
| BOOLEAN | 'true'/'false' (case-insensitive), bool | Direct mapping |
| DATE | ISO 8601, timestamp ms | `new Date()` + validation `!isNaN` |

Can be disabled via `FieldValidator` options (non-normative).

## 4. Validation Pipeline
Implemented order (same as protocol):
1. Required / cardinality
2. Type & possible coercion
3. Membership (if domain resolved or inline)
4. Atomic constraints (declaration order)

Error aggregation (TS) vs short-circuit (Java optional). Both remain compliant as long as logical order is preserved.

## 5. Value Domain Management
- `mode: CLOSED`: strict membership if values resolved.
- `mode: SUGGESTIONS`: membership may be skipped (external values tolerated) — server business validation may reinforce.
- Async resolution: if endpoint not yet queried, validation does not block user (TS & Java usage docs).

## 6. Known Divergences
| Subject | Status | Plan | Tag |
|--------|--------|------|-----|
| Java ignores minLength/maxLength single | Open | Align with TS in 2.0.x | ⚠️ |
| Non-uniform error messages multi-language | Expected | Introduce i18n in 2.x | 🔧 |
| Absence of dedicated `collectionSize` | Limitation | Planned for 2.1.0 | 🔧 |

## 7. Performance
- Simple in-memory cache with hashed key (endpoint + parameters).
- Client-side debounce before search requests.
- Future possibility: persistent cache + LRU strategy.

## 8. Custom Constraint Extension
Minimal expected contract for a custom constraint:
```json
{ "name": "business", "type": "custom", "params": { "kind": "VIP_TIER" }, "errorMessage": "Client not VIP" }
```
The validator delegates to a registered resolver for `custom`.

## 9. Compliance Tests
For a third-party implementation:
- Replay datasets (valid/invalid) by constraint type.
- Check error order if aggregating.
- Simulate endpoint latency (lazy membership).

## 10. Documented Future Additions
| Idea | Justification |
|------|---------------|
| JSON Schema export | Form tooling interoperability | 
| Strict remote mode | Force failure if domain not resolved | 
| i18n message bundle | Multi-locale experience | 

## 11. Cross References
- Specification: `../PROTOCOL_SPECIFICATION.md`
- Migration: `./MIGRATION_V1_V2.md`

---
Last updated: October 2025
