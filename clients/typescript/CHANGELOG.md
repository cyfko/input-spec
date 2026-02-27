# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2025-10-06

### Breaking Changes
- Removed legacy `enumValues` on constraints in favor of unified `valuesEndpoint` with domain `mode` (`CLOSED` or `SUGGESTIONS`).
- Replaced composite constraint objects (min/max/pattern combined) with strictly atomic constraint descriptors (`minLength`, `maxLength`, `pattern`, `minValue`, `maxValue`, `minDate`, `maxDate`, `range`, `custom`).
- Moved field-level `format` (previously a failing constraint in some integrations) to a non-failing advisory `formatHint` property on `InputFieldSpec`.
- Constraint error indexing for multi-value fields is now per element with `index` included in each `ValidationError`.
- Membership evaluation semantics clarified: closed membership occurs before atomic constraints; per-element membership errors use `constraintName: 'membership'` and include index for multi-value.

### Added
- Unified `valuesEndpoint` abstraction supporting INLINE (embedded), HTTP/HTTPS, and gRPC protocols plus `CLOSED` vs `SUGGESTIONS` modes.
- Optional, library-only coercion layer (not part of wire protocol) with opt-in global and per-field control: numeric strings → numbers, boolean strings / numeric booleans → booleans, epoch timestamps → ISO dates, configurable trimming, underscore numeric normalization, extensible true/false token sets, and custom number patterns.
- Comprehensive v1 → v2 migration helper `migrateV1Spec` producing canonical atomic descriptors and INLINE domain definitions.
- Expanded validation pipeline stages (required → type → membership → constraints) ensuring deterministic ordering and full error aggregation (no early exit after first failure).
- Extended test suite (now 93 tests) covering conformance, migration fidelity, edge cases (invalid pattern resilience, partial ranges, unresolved remote domains), coercion behavior, and multi-value indexing semantics.

### Changed
- Error message consistency: standardized phrasing for length, size, numeric, and date constraints (e.g., `Minimum X characters`, `Maximum X items`).
- Multi-value string fields interpret `minValue` / `maxValue` applied to STRING dataType as item count semantics for backward compatibility with legacy expectations.

### Deprecated
- Internal legacy adapter `applyLegacyConstraint` retained only for migration tests and marked with `@deprecated`; scheduled for removal in 3.0.0.

### Migration Guide (Summary)
1. Replace any legacy composite constraint object with a list of atomic descriptors.
2. Convert `enumValues` to an INLINE `valuesEndpoint` with `mode: 'CLOSED'`.
3. Move `format` to `formatHint` (advisory only).
4. Run `migrateV1Spec` for automated transformation where possible.
5. Review multi-value fields for per-element error expectations and update consumer logic if it relied on aggregated messaging.

### Notes
- Coercion is intentionally excluded from the protocol specification to preserve deterministic schema interpretation across services. All coercion options are purely library conveniences and default to disabled.
- Remote `valuesEndpoint` resolution (HTTP/gRPC) is intentionally deferred; unresolved closed domains skip membership until externally populated.

### Internal Quality Improvements
- Added underscore numeric normalization to support patterns allowing visual separators.
- Strengthened pattern evaluation with guarded try/catch ensuring invalid regex patterns produce a controlled `Invalid pattern` error without aborting validation.
- Increased test granularity ensuring each atomic constraint is independently verifiable.

---

## [1.0.0] - 2025-10-03

### Added - Initial Release
- 🎉 **Initial public release** of the Dynamic Input Field Specification Protocol TypeScript implementation
- ⚡ **Zero-dependency architecture** - No external runtime dependencies
- 🔧 **Comprehensive validation engine** with ordered constraint execution
- 🌐 **Framework integration support** with adapters for Angular, React/Axios, and Vue.js
- 📝 **Complete TypeScript support** with full type definitions
- 🧪 **Comprehensive test suite** with 58 test cases covering all functionality
- 📚 **Extensive documentation** with examples and integration guides
- 🚀 **Multiple build formats** supporting CJS, ESM, and TypeScript declarations
- 💾 **Built-in caching system** with TTL support using native Map
- 🔌 **HTTP client abstraction** with configurable adapters and interceptor support

### Core Features
- **InputFieldSpec interface** with intuitive API design
- **FieldValidator** with ordered constraint execution
- **HttpClient abstraction** with framework-specific adapters
- **MemoryCacheProvider** for efficient value caching
- **Comprehensive error handling** with detailed error messages
- **Performance optimizations** with lazy validation and efficient caching

### Framework Integration
- **Angular HttpClient adapter** with proper Observable handling
- **Axios adapter** preserving existing interceptor configurations  
- **ConfigurableFetchHttpClient** with custom interceptor support
- **Dependency injection patterns** for seamless framework integration

### Documentation
- Complete API reference with TypeScript examples
- Framework integration guides for Angular, React, and Vue.js
- Performance optimization guidelines
- Best practices and architectural patterns

### Development Experience
- **Jest test framework** with comprehensive coverage
- **ESLint + Prettier** configuration for code quality
- **TypeScript strict mode** for enhanced type safety
- **tsup build system** for optimized bundling
- **Multiple example applications** demonstrating usage patterns