import { FieldValidator } from '../validation';
import { InputFieldSpec } from '../types';

/**
 * Edge case / completeness tests for protocol v2 library behavior (non-core spec guarantees).
 */
describe('Protocol v2 Edge Cases', () => {
  const validator = new FieldValidator();

  async function run(spec: InputFieldSpec, value: any) {
    return validator.validate(spec, value);
  }

  describe('Invalid pattern handling', () => {
    const spec: InputFieldSpec = {
      displayName: 'Broken',
      dataType: 'STRING',
      expectMultipleValues: false,
      required: false,
      // Intentionally invalid regex pattern to trigger graceful error path
      constraints: [ { name: 'bad', type: 'pattern', params: { regex: '[' } } ]
    };
    it('reports pattern as invalid pattern (non crash)', async () => {
      const r = await run(spec, 'abc');
      expect(r.isValid).toBe(false);
      expect(r.errors[0].message.toLowerCase()).toContain('invalid');
    });
  });

  describe('Multi-error aggregation (no early stop)', () => {
    const spec: InputFieldSpec = {
      displayName: 'Username',
      dataType: 'STRING',
      expectMultipleValues: false,
      required: true,
      constraints: [
        { name: 'minL', type: 'minLength', params: { value: 5 } },
        { name: 'pat', type: 'pattern', params: { regex: '^[a-z]+$' } }
      ]
    };
    it('returns both minLength and pattern errors', async () => {
      const r = await run(spec, 'A1'); // fails length & pattern
      expect(r.isValid).toBe(false);
      const names = r.errors.map(e => e.constraintName);
      expect(names).toContain('minL');
      expect(names).toContain('pat');
    });
  });

  describe('Partial range constraints (min only / max only)', () => {
    const minOnly: InputFieldSpec = {
      displayName: 'TempMin',
      dataType: 'NUMBER',
      expectMultipleValues: false,
      required: false,
      constraints: [ { name: 'r', type: 'range', params: { min: 10 } } ]
    };
    const maxOnly: InputFieldSpec = {
      displayName: 'TempMax',
      dataType: 'NUMBER',
      expectMultipleValues: false,
      required: false,
      constraints: [ { name: 'r', type: 'range', params: { max: 5 } } ]
    };
    it('enforces min only', async () => {
      const below = await run(minOnly, 5);
      expect(below.isValid).toBe(false);
      const ok = await run(minOnly, 12);
      expect(ok.isValid).toBe(true);
    });
    it('enforces max only', async () => {
      const above = await run(maxOnly, 9);
      expect(above.isValid).toBe(false);
      const ok = await run(maxOnly, 3);
      expect(ok.isValid).toBe(true);
    });
  });

  describe('Extended coercion options', () => {
    it('accepts custom truthy/falsey tokens', async () => {
      const spec: InputFieldSpec = {
        displayName: 'Flag',
        dataType: 'BOOLEAN',
        expectMultipleValues: false,
        required: true,
        constraints: []
      };
      const v = new FieldValidator({ coerce: true, extraTrueValues: ['yes'], extraFalseValues: ['no'] });
      const r1 = await v.validate(spec, 'yes');
      const r2 = await v.validate(spec, 'no');
      expect(r1.isValid).toBe(true);
      expect(r2.isValid).toBe(true);
    });
    it('honors custom number pattern', async () => {
      const spec: InputFieldSpec = {
        displayName: 'Amount',
        dataType: 'NUMBER',
        expectMultipleValues: false,
        required: true,
        constraints: []
      };
      // Accept numbers with underscore separators e.g. 1_000
      const custom = new FieldValidator({ coerce: true, numberPattern: /^\d+(?:_\d{3})*$/ });
      const r = await custom.validate(spec, '1_000');
      expect(r.isValid).toBe(true);
    });
  });

  describe('Required empty array handling', () => {
    const spec: InputFieldSpec = {
      displayName: 'Tags',
      dataType: 'STRING',
      expectMultipleValues: true,
      required: true,
      constraints: []
    };
    it('fails required on empty array', async () => {
      const r = await run(spec, []);
      expect(r.isValid).toBe(false);
      expect(r.errors[0].constraintName).toBe('required');
    });
  });

  describe('formatHint neutrality', () => {
    const spec: InputFieldSpec = {
      displayName: 'Code',
      dataType: 'STRING',
      expectMultipleValues: false,
      required: false,
      formatHint: 'alphaCode',
      constraints: [ { name: 'alpha', type: 'pattern', params: { regex: '^[A-Z]{3}$' } } ]
    };
    it('ignores formatHint for validation outcome', async () => {
      const good = await run(spec, 'ABC');
      const bad = await run(spec, 'AB1');
      expect(good.isValid).toBe(true);
      expect(bad.isValid).toBe(false);
    });
  });

  describe('trimStrings default & disabled', () => {
    const spec: InputFieldSpec = {
      displayName: 'City',
      dataType: 'STRING',
      expectMultipleValues: false,
      required: true,
      constraints: [ { name: 'syntax', type: 'pattern', params: { regex: '^[A-Z]+$' } } ]
    };
    it('trims by default (coercion on)', async () => {
      const v = new FieldValidator({ coerce: true });
      const r = await v.validate(spec, '  PARIS  ');
      expect(r.isValid).toBe(true); // after trim matches pattern
    });
    it('can skip trimming when disabled', async () => {
      const v = new FieldValidator({ coerce: true, trimStrings: false });
      const r = await v.validate(spec, '  PARIS  ');
      // Not trimmed ⇒ pattern fails
      expect(r.isValid).toBe(false);
    });
  });
});
