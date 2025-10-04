// =========================================
// DYNAMIC INPUT FIELD SPECIFICATION PROTOCOL v2.0
// Pure TypeScript Types - No external dependencies
// =========================================

// Core Data Types
export type DataType = 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN';

// Protocol hints for client implementations
export type Protocol = 'HTTPS' | 'HTTP' | 'GRPC';
export type HttpMethod = 'GET' | 'POST';

// Pagination Strategies
export type PaginationStrategy = 'NONE' | 'PAGE_NUMBER';

// Cache Strategies  
export type CacheStrategy = 'NONE' | 'SESSION' | 'SHORT_TERM' | 'LONG_TERM';

// Value Alias - Représente une option de valeur
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

// Values Endpoint Configuration
export interface ValuesEndpoint {
  protocol?: Protocol; // Default: 'HTTPS' (client protocol hint)
  uri: string;
  method?: HttpMethod; // Default: 'GET'
  searchField?: string;
  paginationStrategy?: PaginationStrategy;
  responseMapping: ResponseMapping;
  requestParams?: RequestParams;
  cacheStrategy?: CacheStrategy;
  debounceMs?: number; // Default: 300
  minSearchLength?: number; // Default: 0
}

// Constraint Descriptor - Règles de validation pour un champ
export interface ConstraintDescriptor {
  name: string; // Nom de la contrainte (ex: 'email', 'length', 'pattern')
  description?: string;
  errorMessage?: string;
  defaultValue?: any;
  min?: number | string; // number for STRING length/NUMBER value, string for DATE
  max?: number | string; // number for STRING length/NUMBER value, string for DATE
  pattern?: string;
  format?: string;
  enumValues?: ValueAlias[];
  valuesEndpoint?: ValuesEndpoint;
}

// Input Field Specification - Spécification complète d'un champ
export interface InputFieldSpec {
  displayName: string;
  description?: string;
  dataType: DataType;
  expectMultipleValues: boolean;
  required: boolean; // Required field at top level
  constraints: ConstraintDescriptor[]; // Array for ordered execution
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
  if (!obj || typeof obj !== 'object') {
    return false;
  }
  
  return (
    typeof obj.displayName === 'string' &&
    typeof obj.dataType === 'string' &&
    ['STRING', 'NUMBER', 'DATE', 'BOOLEAN'].includes(obj.dataType) &&
    typeof obj.expectMultipleValues === 'boolean' &&
    typeof obj.required === 'boolean' &&
    Array.isArray(obj.constraints) &&
    obj.constraints.every((constraint: any) => 
      constraint && 
      typeof constraint === 'object' && 
      typeof constraint.name === 'string'
    )
  );
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
    protocol: 'HTTPS', // Recommended default
    uri,
    method: 'GET',
    debounceMs: 300,
    minSearchLength: 0,
    responseMapping: {
      dataField: 'data'
    }
  };
}