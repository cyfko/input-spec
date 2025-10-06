## 2.0.0 (2025-10-06)

Breaking release aligning Java implementation with Protocol v2.0.

### Added
- Atomic constraint model (`AtomicConstraint`, `ConstraintType`) replacing composite semantics.
- Unified `ValuesEndpoint` with `INLINE` protocol and `mode` (`CLOSED` vs `SUGGESTIONS`).
- Field-level `formatHint` property.
- Membership validation (closed domain) ahead of atomic constraints.
- Migration helper `V1ToV2Migration` to adapt legacy `ConstraintDescriptor` + enum values.
- Coercion utilities (best-effort, non-throwing) parity with TypeScript.

### Changed
- In-place refactor of `InputFieldSpec` (no parallel v2 package) to avoid namespace churn.
- `enumValues` removed in favor of `valuesEndpoint.protocol = INLINE` static list.
- Validation pipeline: required → type → membership → atomic constraints → legacy fallback.

### Deprecated
- `ConstraintDescriptor` (legacy composite); retained only for migration path and backward compatibility.

### Removed
- Duplicate `io.github.cyfko.inputspec.v2` package (replaced by in-place model).

### Serialization
- Restored Jackson annotations with stable `@JsonPropertyOrder` and `@JsonInclude(Include.NON_NULL)`.

### Tests
- Added v2 migration, membership mode, atomic constraints (range, date, length, count) test coverage.

### Notes
- Next major (3.x) will remove `ConstraintDescriptor` and legacy validation path once downstream consumers fully adopt atomic constraints.
