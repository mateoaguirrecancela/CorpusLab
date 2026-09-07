import { describe, expect, it } from 'vitest';
import { type TFunction } from 'i18next';
import { getProfileGenderOptions } from '@/modules/auth/profile/utils/profileOptions';

const t = ((key: string) => key) as TFunction;

describe('getProfileGenderOptions', () => {
  it('returns the placeholder plus the three gender options in order', () => {
    expect(getProfileGenderOptions(t)).toEqual([
      { value: '', label: 'auth.gender.select' },
      { value: 'MALE', label: 'auth.gender.male' },
      { value: 'FEMALE', label: 'auth.gender.female' },
      { value: 'OTHER', label: 'auth.gender.other' },
    ]);
  });
});
