import { describe, expect, it } from 'vitest';
import { mergeFormValues, pickFieldErrors } from '@/shared/utils/formUtils';

describe('mergeFormValues', () => {
  it('overrides defaults with watched values', () => {
    expect(mergeFormValues({ a: 1, b: 2 }, { b: 5 })).toEqual({ a: 1, b: 5 });
  });

  it('returns the defaults unchanged when nothing is watched', () => {
    expect(mergeFormValues({ a: 1, b: 2 }, {})).toEqual({ a: 1, b: 2 });
  });
});

describe('pickFieldErrors', () => {
  it('extracts only string error messages for the requested fields', () => {
    const errors = pickFieldErrors(
      {
        a: { message: 'Required', type: 'required' },
        b: { message: undefined, type: 'required' },
      } as never,
      ['a', 'b'],
    );

    expect(errors).toEqual({ a: 'Required' });
  });

  it('ignores fields without an error entry', () => {
    expect(pickFieldErrors({} as never, ['a'])).toEqual({});
  });
});
