import { describe, expect, it } from 'vitest';
import {
  EMPTY_PROFILE_FORM,
  mergeProfileFormValues,
  pickProfileFieldErrors,
  profileToFormState,
} from '@/modules/auth/profile/utils/profileForm';
import { type ProfileResponse } from '@/modules/auth/types/profile';

const profile: ProfileResponse = {
  email: 'a@b.com',
  firstName: 'Jane',
  lastName: 'Doe',
  birth: '2000-01-01',
  gender: 'FEMALE',
  countryCode: 'ES',
  city: 'Coruna',
} as ProfileResponse;

describe('profileToFormState', () => {
  it('maps a profile response to form values, defaulting nullable fields', () => {
    expect(profileToFormState({ ...profile, birth: null, countryCode: null, city: null })).toEqual({
      firstName: 'Jane',
      lastName: 'Doe',
      birth: '',
      gender: 'FEMALE',
      countryCode: '',
      city: '',
    });
  });

  it('falls back to empty gender when the value is not a known option', () => {
    expect(profileToFormState({ ...profile, gender: 'UNKNOWN' as never }).gender).toBe('');
  });

  it('keeps a valid gender', () => {
    expect(profileToFormState(profile).gender).toBe('FEMALE');
  });
});

describe('mergeProfileFormValues', () => {
  it('overlays partial watched values on the empty form', () => {
    expect(mergeProfileFormValues({ firstName: 'Jane' })).toEqual({
      ...EMPTY_PROFILE_FORM,
      firstName: 'Jane',
    });
  });
});

describe('pickProfileFieldErrors', () => {
  it('extracts only string messages for known profile fields', () => {
    const errors = pickProfileFieldErrors({
      firstName: { message: 'Required', type: 'required' },
      lastName: undefined,
    } as never);

    expect(errors).toEqual({ firstName: 'Required' });
  });
});
