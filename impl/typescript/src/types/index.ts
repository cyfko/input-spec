// =========================================
// DYNAMIC INPUT FIELD SPECIFICATION PROTOCOL v2.0
// Pure TypeScript Types - No external dependencies
// =========================================

// Core Data Types
export type DataType = 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN';

// Protocol hints for client implementations
export type Protocol = 'HTTPS' | 'HTTP' | 'GRPC' | 'INLINE';
export type HttpMethod = 'GET' | 'POST';

// Pagination Strategies
export type PaginationStrategy = 'NONE' | 'PAGE_NUMBER';

// Cache Strategies
export type CacheStrategy = 'NONE' | 'SESSION' | 'SHORT_TERM' | 'LONG_TERM';

// Domain Modes
export type ClosedDomainMode = 'CLOSED' | 'SUGGESTIONS';

// Value Alias - Represents a selectable value
export interface ValueAlias {
  value: any;
  label: string;
}

// Request Parameters Configuration
export interface RequestParams {
  pageParam?: string;
  limitParam?: string;
  searchParam?: string;
  defaultLimit?: number;
}

// Response Mapping Configuration
export interface ResponseMapping {
  dataField: string;
  pageField?: string;
  pageSizeField?: string;
  totalField?: string;
  hasNextField?: string;
}

// Updated Values Endpoint Configuration (v2)
export interface ValuesEndpoint {
  protocol: Protocol;              // 'INLINE' or remote protocol
  mode?: ClosedDomainMode;         // default: CLOSED
  items?: ValueAlias[];            // required if protocol = INLINE
  uri?: string;                    // required if remote
  method?: HttpMethod;             // default GET
  searchField?: string;
  paginationStrategy?: PaginationStrategy;
  responseMapping?: ResponseMapping; // optional for INLINE
  requestParams?: RequestParams;
  cacheStrategy?: CacheStrategy;
  debounceMs?: number;             // hint
  minSearchLength?: number;        // hint
}

// Atomic Constraint Types
export type ConstraintType =
  | 'pattern'
  | 'minLength'
  | 'maxLength'
  | 'minValue'
  | 'maxValue'
  | 'minDate'
  | 'maxDate'
  | 'range'
  | 'custom';

// Atomic Constraint Descriptor (v2 canonical)
export interface AtomicConstraintDescriptor {
  name: string;              // stable identifier
  type: ConstraintType;      // discriminator
  params: any;               // type specific payload { regex }, { value }, { min, max, step }, etc.
  errorMessage?: string;
  description?: string;
}

// Legacy Descriptor (v1) kept for transitional migration (NOT in spec, will be removed)
// This allows old tests / data to pass through a transformation layer.
export interface LegacyConstraintDescriptor {
  // marker: absence of 'type' & 'params' implies legacy
  name: string;
  description?: string;
  errorMessage?: string;
  defaultValue?: any;
  min?: number | string;
  max?: number | string;
  pattern?: string;
  format?: string;
  enumValues?: ValueAlias[];
  valuesEndpoint?: any; // legacy embedded, will be lifted; kept as any to avoid strict coupling
  [key: string]: any;
}

export type ConstraintDescriptor = AtomicConstraintDescriptor | LegacyConstraintDescriptor;

// Input Field Specification (v2)
export interface InputFieldSpec {
  displayName: string;
  description?: string;
  dataType: DataType;
  expectMultipleValues: boolean;
  required: boolean;
  formatHint?: string;                 // moved from constraint-level format to a global, non-enforced hint
  valuesEndpoint?: ValuesEndpoint;          // top-level domain (optional)
  constraints: ConstraintDescriptor[];       // ordered; legacy entries transformed at runtime
  /**
   * Library-only (non-protocol) optional per-field coercion override.
   * Not serialized as part of the wire protocol unless a custom profile permits.
   */
  coercion?: {
    coerce?: boolean;
    acceptNumericBoolean?: boolean;
    extraTrueValues?: string[];
    extraFalseValues?: string[];
    numberPattern?: RegExp;
    dateEpochSupport?: boolean;
    trimStrings?: boolean;
  };
}

// =========================================
// VALIDATION & API RESPONSE TYPES
// =========================================

// Validation Result
export interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
}

export interface ValidationError {
  constraintName: string;
  message: string;
  value?: any;
  index?: number; // present for multi-value element level errors
}

// API Response Types
export interface FieldsResponse {
  fields: InputFieldSpec[];
  version: string;
}

export interface FieldResponse {
  field: InputFieldSpec;
}

export interface ValuesResponse {
  data?: ValueAlias[];
  page?: number;
  pageSize?: number;
  total?: number;
  hasNext?: boolean;
  [key: string]: any;
}

// =========================================
// TYPE GUARDS & UTILITIES (si besoin)
// =========================================

/**
 * Type guard pour vérifier si un objet est un InputFieldSpec valide
 * Utilisation légère si besoin de runtime validation
 */
export function isInputFieldSpec(obj: any): obj is InputFieldSpec {
  if (!obj || typeof obj !== 'object') return false;
  if (!['STRING', 'NUMBER', 'DATE', 'BOOLEAN'].includes(obj.dataType)) return false;
  if (!Array.isArray(obj.constraints)) return false;
  return typeof obj.displayName === 'string' &&
    typeof obj.expectMultipleValues === 'boolean' &&
    typeof obj.required === 'boolean';
}

/**
 * Type guard pour ValueAlias
 */
export function isValueAlias(obj: any): obj is ValueAlias {
  if (!obj || typeof obj !== 'object') {
    return false;
  }
  
  return obj.value !== undefined &&
    typeof obj.label === 'string';
}

/**
 * Helper function to create ValuesEndpoint with recommended defaults
 */
export function createDefaultValuesEndpoint(uri: string): ValuesEndpoint {
  return {
    protocol: 'HTTPS',
    uri,
    method: 'GET',
    debounceMs: 300,
    minSearchLength: 0,
    // Provide a minimal default response mapping to satisfy tests & typical usage
    responseMapping: { dataField: 'data' }
  };
}

export function buildInlineValuesEndpoint(items: ValueAlias[], mode: ClosedDomainMode = 'CLOSED'): ValuesEndpoint {
  return { protocol: 'INLINE', items, mode };
}

// Utility: detect atomic vs legacy
export function isAtomicConstraint(c: ConstraintDescriptor): c is AtomicConstraintDescriptor {
  return (c as any).type !== undefined && (c as any).params !== undefined;
}