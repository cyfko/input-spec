import { FieldValidator } from '../validation';
import { InputFieldSpec } from '../types';

describe('Coercion (library-only extension)', () => {
  const baseNumber: InputFieldSpec = {
    displayName: 'Num',
    dataType: 'NUMBER',
    expectMultipleValues: false,
    required: true,
    constraints: []
  };

  it('rejects numeric string when coercion disabled', async () => {
    const v = new FieldValidator();
    const r = await v.validate(baseNumber, '42');
    expect(r.isValid).toBe(false);
    expect(r.errors[0].constraintName).toBe('type');
  });

  it('accepts numeric string when global coercion enabled', async () => {
    const v = new FieldValidator({ coerce: true });
    const r = await v.validate(baseNumber, '42');
    expect(r.isValid).toBe(true);
  });

  it('accepts boolean strings when coercion enabled', async () => {
    const spec: InputFieldSpec = { ...baseNumber, dataType: 'BOOLEAN' };
    const v = new FieldValidator({ coerce: true });
    const r = await v.validate(spec, 'true');
    expect(r.isValid).toBe(true);
  });

  it('accepts numeric boolean when enabled', async () => {
    const spec: InputFieldSpec = { ...baseNumber, dataType: 'BOOLEAN' };
    const v = new FieldValidator({ coerce: true, acceptNumericBoolean: true });
    const r = await v.validate(spec, '1');
    expect(r.isValid).toBe(true);
  });

  it('per-field coercion override activates even if global disabled', async () => {
    const spec: InputFieldSpec = { ...baseNumber, coercion: { coerce: true } };
    const v = new FieldValidator({ coerce: false });
    const r = await v.validate(spec, '7');
    expect(r.isValid).toBe(true);
  });

  it('epoch date coercion (seconds) when enabled', async () => {
    const spec: InputFieldSpec = { ...baseNumber, dataType: 'DATE', coercion: { coerce: true, dateEpochSupport: true } };
    const v = new FieldValidator();
    const r = await v.validate(spec, '1700000000');
    expect(r.isValid).toBe(true);
  });

  it('membership loose match after coercion', async () => {
    const spec: InputFieldSpec = {
      ...baseNumber,
      valuesEndpoint: { protocol: 'INLINE', mode: 'CLOSED', items: [ { value: '5', label: 'five' } ] },
      coercion: { coerce: true }
    };
    const v = new FieldValidator();
    const r = await v.validate(spec, '5'); // remains string -> membership ok
    expect(r.isValid).toBe(true);
    const r2 = await v.validate(spec, 5); // direct number no coercion needed
    expect(r2.isValid).toBe(true);
    const r3 = await v.validate(spec, '6');
    expect(r3.isValid).toBe(false);
  });
});
