# Changelog

All notable changes to the `input-spec` core Java library will be documented in this file.

## [3.0.0] - 2026-02-28

### Added
- **Date Validation Robustness**: `FormSpecValidator` natively accepts short ISO dates (e.g., `YYYY-MM-DD`) alongside full `OffsetDateTime`s, by falling back to `LocalDateTime` and `LocalDate` (at UTC midnight) parsing logic.
- **Enum to INLINE Auto-Mapping**: The Annotation Processor natively infers Java `Enum` types mapped to `@FieldMeta` and outputs `INLINE CLOSED` JSON values lists with auto-capitalized labels.
- **Cross-Constraints**: Full engine support for inter-field rules (`FIELD_COMPARISON`, `AT_LEAST_ONE`, `MUTUALLY_EXCLUSIVE`, `DEPENDS_ON`).
- **Custom Handlers**: Exposed API (`registerCustomHandler`) to supply bespoke validation callbacks for custom constraint namespaces.
- **I18n Skeleton Generation**: Annotation processor generates `[form-id].properties` skeletons, automatically mapping keys based on the schema and field names.
- **MCP Tool Server Integration**: A brand new Spring Boot starter `input-spec-spring-boot-starter` (released at `1.0.0`) brings `@FormHandler` reflection routing and enables exposing InputSpec forms as Model Context Protocol (MCP) Tools to AI agents.

### Changed
- **[BREAKING] Major API Refactoring**: Form validation processing and handler response models have drastically changed to enforce stronger type safety and developer experience.
- **[BREAKING] SubmitResponse API**: Dropped loosely typed arguments for strongly-typed static factories (`SubmitResponse.ok(Map)`, `SubmitResponse.rejected(String)`).
- **Core Versioning Alignment**: `input-spec` and `input-spec-processor` are now strictly synchronized at version `3.x.x` for semantic clarity.

---

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
