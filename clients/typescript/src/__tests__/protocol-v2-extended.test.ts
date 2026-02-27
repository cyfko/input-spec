import { FieldValidator } from '../validation';
import { InputFieldSpec, buildInlineValuesEndpoint } from '../types';
import { migrateV1Spec } from '../migration/v1-to-v2';

describe('Protocol v2 Extended Coverage', () => {
  const validator = new FieldValidator();

  async function run(spec: InputFieldSpec, value: any) {
    return validator.validate(spec, value);
  }

  describe('Date Range Constraint (range + minDate/maxDate)', () => {
    const dateSpec: InputFieldSpec = {
      displayName: 'Booking Date',
      dataType: 'DATE',
      expectMultipleValues: false,
      required: true,
      constraints: [
        { name: 'window', type: 'range', params: { min: '2025-01-01', max: '2025-12-31' } }
      ]
    };

    it('rejects before min', async () => {
      const r = await run(dateSpec, '2024-12-31');
      expect(r.isValid).toBe(false);
      expect(r.errors[0].constraintName).toBe('window');
    });
    it('rejects after max', async () => {
      const r = await run(dateSpec, '2026-01-01');
      expect(r.isValid).toBe(false);
    });
    it('accepts in-range', async () => {
      const r = await run(dateSpec, '2025-06-15');
      expect(r.isValid).toBe(true);
    });
  });

  describe('Membership default CLOSED mode when omitted', () => {
    const spec: InputFieldSpec = {
      displayName: 'Status',
      dataType: 'STRING',
      expectMultipleValues: false,
      required: false,
      valuesEndpoint: { protocol: 'INLINE', items: [ { value: 'ON', label: 'On' } ] }, // mode omitted
      constraints: []
    };
    it('treats omitted mode as CLOSED', async () => {
      const ok = await run(spec, 'ON');
      expect(ok.isValid).toBe(true);
      const bad = await run(spec, 'OFF');
      expect(bad.isValid).toBe(false);
      expect(bad.errors.some(e => e.constraintName === 'membership')).toBe(true);
    });
  });

  describe('Unresolved remote CLOSED membership skipped', () => {
    const remoteSpec: InputFieldSpec = {
      displayName: 'Country',
      dataType: 'STRING',
      expectMultipleValues: false,
      required: false,
      valuesEndpoint: { protocol: 'HTTPS', uri: 'https://api.example.com/countries', mode: 'CLOSED' },
      constraints: [ { name: 'syntax', type: 'pattern', params: { regex: '^[A-Z]{2}$' } } ]
    };
    it('does not produce membership error pre-resolution', async () => {
      const r = await run(remoteSpec, 'ZZ');
      // Only pattern should fail
      if (!r.isValid) {
        expect(r.errors.every(e => e.constraintName !== 'membership')).toBe(true);
      }
    });
  });

  describe('Migration adds formatHint and removes enumValues', () => {
    it('maps legacy format to formatHint', () => {
      const legacy: any = {
        displayName: 'Email',
        dataType: 'STRING',
        expectMultipleValues: false,
        required: true,
        constraints: [ { name: 'legacyEmail', pattern: '^.+@.+$', format: 'email', enumValues: [ { value: 'a@b', label: 'a@b' } ] } ]
      };
      const migrated = migrateV1Spec(legacy);
      expect(migrated.formatHint).toBe('email');
      expect(migrated.valuesEndpoint).toBeDefined();
      expect(migrated.valuesEndpoint!.protocol).toBe('INLINE');
      // enumValues removed from constraints
      expect(JSON.stringify(migrated.constraints).includes('enumValues')).toBe(false);
    });
  });

  describe('Boolean coercion membership (INLINE string values)', () => {
    const boolSpec: InputFieldSpec = {
      displayName: 'Flag',
      dataType: 'BOOLEAN',
      expectMultipleValues: false,
      required: true,
      valuesEndpoint: buildInlineValuesEndpoint([ { value: 'true', label: 'Yes' }, { value: 'false', label: 'No' } ]),
      constraints: []
    };
    const coercingValidator = new FieldValidator({ coerce: true });
    it('accepts string true via coercion and loose membership', async () => {
      const r = await coercingValidator.validate(boolSpec, 'true');
      expect(r.isValid).toBe(true);
    });
    it('accepts boolean true against string domain', async () => {
      const r = await coercingValidator.validate(boolSpec, true);
      expect(r.isValid).toBe(true);
    });
  });

  describe('Graceful ignore of unknown custom constraint type', () => {
    const spec: InputFieldSpec = {
      displayName: 'Data',
      dataType: 'STRING',
      expectMultipleValues: false,
      required: false,
      constraints: [ { name: 'mystery', type: 'custom', params: { foo: 'bar' } } ]
    };
    it('ignores custom constraint without handler', async () => {
      const r = await run(spec, 'value');
      expect(r.isValid).toBe(true);
    });
  });

  describe('Multi-value membership indexing', () => {
    const spec: InputFieldSpec = {
      displayName: 'Codes',
      dataType: 'STRING',
      expectMultipleValues: true,
      required: false,
      valuesEndpoint: buildInlineValuesEndpoint([ { value: 'A', label: 'A' }, { value: 'B', label: 'B' } ]),
      constraints: []
    };
    it('indexes membership errors per element', async () => {
      const r = await run(spec, ['A','X','B','Y']);
      const membershipErrors = r.errors.filter(e => e.constraintName === 'membership');
      expect(membershipErrors.length).toBe(2);
      expect(membershipErrors.some(e => e.index === 1)).toBe(true);
      expect(membershipErrors.some(e => e.index === 3)).toBe(true);
    });
  });
});
