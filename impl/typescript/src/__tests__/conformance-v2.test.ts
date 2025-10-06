import { FieldValidator } from '../validation';
import { InputFieldSpec } from '../types';
import { migrateV1Spec } from '../migration/v1-to-v2';

// Jest globals (describe/it/expect) assumed via test environment; file is TS.

describe('Protocol v2 Conformance', () => {
  const validator = new FieldValidator();

  async function run(spec: InputFieldSpec, value: any) {
    return validator.validate(spec, value);
  }

  describe('Closed vs Suggestions Membership', () => {
    const closedSpec: InputFieldSpec = {
      displayName: 'Status',
      dataType: 'STRING',
      expectMultipleValues: false,
      required: false,
      valuesEndpoint: {
        protocol: 'INLINE',
        mode: 'CLOSED',
        items: [
          { value: 'ACTIVE', label: 'Active' },
          { value: 'INACTIVE', label: 'Inactive' }
        ]
      },
      constraints: []
    };

    const suggestionsSpec: InputFieldSpec = {
      ...closedSpec,
      valuesEndpoint: {
        protocol: 'INLINE',
        mode: 'SUGGESTIONS',
        items: closedSpec.valuesEndpoint!.items
      }
    };

    it('rejects value not in CLOSED domain', async () => {
      const result = await run(closedSpec, 'UNKNOWN');
      expect(result.isValid).toBe(false);
      expect(result.errors.some(e => e.constraintName === 'membership')).toBe(true);
    });

    it('accepts value not in SUGGESTIONS domain', async () => {
      const result = await run(suggestionsSpec, 'UNKNOWN');
      expect(result.isValid).toBe(true);
    });
  });

  describe('Pattern Failure', () => {
    const spec: InputFieldSpec = {
      displayName: 'Code',
      dataType: 'STRING',
      expectMultipleValues: false,
      required: true,
      constraints: [
        { name: 'syntax', type: 'pattern', params: { regex: '^[A-Z]{3}$' }, errorMessage: 'Three uppercase letters' }
      ]
    };

    it('reports pattern error', async () => {
      const result = await run(spec, 'ab');
      expect(result.isValid).toBe(false);
      expect(result.errors[0].constraintName).toBe('syntax');
    });
  });

  describe('Range Constraint', () => {
    const spec: InputFieldSpec = {
      displayName: 'Temperature',
      dataType: 'NUMBER',
      expectMultipleValues: false,
      required: true,
      constraints: [
        { name: 'operational', type: 'range', params: { min: 0, max: 10 } }
      ]
    };

    it('flags value below min', async () => {
      const r = await run(spec, -1);
      expect(r.isValid).toBe(false);
      expect(r.errors[0].constraintName).toBe('operational');
    });
    it('flags value above max', async () => {
      const r = await run(spec, 11);
      expect(r.isValid).toBe(false);
    });
    it('accepts in-range value', async () => {
      const r = await run(spec, 5);
      expect(r.isValid).toBe(true);
    });
  });

  describe('Multi-value Index Errors', () => {
    const spec: InputFieldSpec = {
      displayName: 'Codes',
      dataType: 'STRING',
      expectMultipleValues: true,
      required: true,
      constraints: [
        { name: 'syntax', type: 'pattern', params: { regex: '^[A-Z]{2}$' }, errorMessage: 'Two uppercase letters' }
      ]
    };

    it('provides index on element errors', async () => {
      const r = await run(spec, ['OK', 'bad', 'NO']);
      expect(r.isValid).toBe(false);
      const idxError = r.errors.find(e => e.index === 1);
      expect(idxError).toBeTruthy();
      expect(idxError!.constraintName).toBe('syntax');
    });
  });

  describe('Migration Fidelity (v1 → v2)', () => {
  it('migrates legacy composite and enumValues', async () => {
      const legacy: any = {
        displayName: 'Priority',
        dataType: 'STRING',
        expectMultipleValues: false,
        required: true,
        constraints: [
          {
            name: 'legacyComposite',
            min: 3,
            max: 5,
            pattern: '^[A-Z]+$',
            enumValues: [
              { value: 'LOW', label: 'Low' },
              { value: 'MED', label: 'Medium' },
              { value: 'HIGH', label: 'High' }
            ]
          }
        ]
      };

  const migrated = migrateV1Spec(legacy);
      expect(migrated.valuesEndpoint).toBeDefined();
      expect(migrated.valuesEndpoint!.protocol).toBe('INLINE');
      expect(migrated.constraints.length).toBeGreaterThanOrEqual(3); // min + max + pattern

      // Validate a legal value
      const ok = await run(migrated, 'HIGH');
      expect(ok.isValid).toBe(true);

      // Membership fail
      const fail = await run(migrated, 'UNKNOWN');
      expect(fail.isValid).toBe(false);
      expect(fail.errors.some(e => e.constraintName === 'membership')).toBe(true);
    });
  });
});
