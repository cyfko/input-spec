// Migration utility from legacy v1 shape to v2 atomic model
// NOTE: Best-effort; callers should validate output with isInputFieldSpec.

import {
  InputFieldSpec,
  ConstraintDescriptor,
  AtomicConstraintDescriptor,
  ValueAlias,
  buildInlineValuesEndpoint,
  ConstraintType
} from '../types';

interface V1ConstraintLike {
  name: string;
  min?: number | string;
  max?: number | string;
  pattern?: string;
  format?: string;
  enumValues?: ValueAlias[];
  valuesEndpoint?: any;
  errorMessage?: string;
  description?: string;
  [k: string]: any;
}

interface V1SpecLike {
  displayName: string;
  description?: string;
  dataType: string;
  expectMultipleValues: boolean;
  required: boolean;
  constraints: V1ConstraintLike[];
  [k: string]: any;
}

function toAtomic(name: string, type: ConstraintType, params: any, errorMessage?: string, description?: string): AtomicConstraintDescriptor {
  const atomic: AtomicConstraintDescriptor = {
    name,
    type,
    params,
    ...(errorMessage ? { errorMessage } : {}),
    ...(description ? { description } : {})
  } as AtomicConstraintDescriptor;
  return atomic;
}

export function migrateV1Spec(v1: V1SpecLike): InputFieldSpec {
  // Detect top-level valuesEndpoint (legacy nested style)
  let liftedValuesEndpoint: any | undefined;
  const atomic: ConstraintDescriptor[] = [];

  for (const legacy of v1.constraints) {
    // enumValues -> INLINE domain (lift only once)
    if (legacy.enumValues && !liftedValuesEndpoint) {
      liftedValuesEndpoint = buildInlineValuesEndpoint(legacy.enumValues);
    }
    // embedded valuesEndpoint -> lift (first wins)
    if (legacy.valuesEndpoint && !liftedValuesEndpoint) {
      liftedValuesEndpoint = legacy.valuesEndpoint;
    }

    // scalar conversions
    if (legacy.pattern) {
      atomic.push(toAtomic(legacy.name + '_pattern', 'pattern', { regex: legacy.pattern }, legacy.errorMessage, legacy.description));
    }
    if (legacy.min !== undefined || legacy.max !== undefined) {
      // Map based on dataType
      if (v1.dataType === 'STRING') {
        if (typeof legacy.min === 'number') atomic.push(toAtomic(legacy.name + '_minLength', 'minLength', { value: legacy.min }, legacy.errorMessage));
        if (typeof legacy.max === 'number') atomic.push(toAtomic(legacy.name + '_maxLength', 'maxLength', { value: legacy.max }, legacy.errorMessage));
      } else if (v1.dataType === 'NUMBER') {
        if (typeof legacy.min === 'number') atomic.push(toAtomic(legacy.name + '_minValue', 'minValue', { value: legacy.min }, legacy.errorMessage));
        if (typeof legacy.max === 'number') atomic.push(toAtomic(legacy.name + '_maxValue', 'maxValue', { value: legacy.max }, legacy.errorMessage));
      } else if (v1.dataType === 'DATE') {
        if (legacy.min !== undefined) atomic.push(toAtomic(legacy.name + '_minDate', 'minDate', { iso: legacy.min }, legacy.errorMessage));
        if (legacy.max !== undefined) atomic.push(toAtomic(legacy.name + '_maxDate', 'maxDate', { iso: legacy.max }, legacy.errorMessage));
      }
    }
    // legacy.format now maps to field-level formatHint (handled after loop)
  }

  const spec: InputFieldSpec = {
    displayName: v1.displayName,
    ...(v1.description ? { description: v1.description } : {}),
    dataType: v1.dataType as any,
    expectMultipleValues: v1.expectMultipleValues,
    required: v1.required,
    ...(v1.constraints.find(c => c.format) ? { formatHint: v1.constraints.find(c => c.format)!.format } : {}),
    ...(liftedValuesEndpoint ? { valuesEndpoint: liftedValuesEndpoint } : {}),
    constraints: atomic
  } as InputFieldSpec;

  return spec;
}

export function migrateV1Specs(list: V1SpecLike[]): InputFieldSpec[] {
  return list.map(migrateV1Spec);
}
