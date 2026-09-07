import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { useProfileForm } from '@/modules/auth/profile/hooks/useProfileForm';
import { type ProfileResponse } from '@/modules/auth/types/profile';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

function isoDateYearsAgo(years: number): string {
  const date = new Date();
  date.setFullYear(date.getFullYear() - years);
  return date.toISOString().slice(0, 10);
}

const profile: ProfileResponse = {
  email: 'a@b.com',
  firstName: 'Jane',
  lastName: 'Doe',
  birth: isoDateYearsAgo(20),
  gender: 'FEMALE',
  countryCode: 'ES',
  city: 'Coruna',
};

describe('useProfileForm', () => {
  it('starts empty and invalid', () => {
    const { result } = renderHook(() => useProfileForm());
    expect(result.current.form.firstName).toBe('');
    expect(result.current.isValid).toBe(false);
  });

  it('syncWithProfile fills the form and passes validation', async () => {
    const { result } = renderHook(() => useProfileForm());

    act(() => result.current.syncWithProfile(profile));

    await waitFor(() => expect(result.current.isValid).toBe(true));
    expect(result.current.form).toEqual({
      firstName: 'Jane',
      lastName: 'Doe',
      birth: profile.birth,
      gender: 'FEMALE',
      countryCode: 'ES',
      city: 'Coruna',
    });
  });

  it('getValidValues returns null when the form is invalid', async () => {
    const { result } = renderHook(() => useProfileForm());

    const values = await result.current.getValidValues();

    expect(values).toBeNull();
  });

  it('getValidValues returns the parsed values once the form is valid', async () => {
    const { result } = renderHook(() => useProfileForm());
    act(() => result.current.syncWithProfile(profile));
    await waitFor(() => expect(result.current.isValid).toBe(true));

    const values = await result.current.getValidValues();

    expect(values).toEqual({
      firstName: 'Jane',
      lastName: 'Doe',
      birth: profile.birth,
      gender: 'FEMALE',
      countryCode: 'ES',
      city: 'Coruna',
    });
  });

  it('setField updates a single field and re-validates', async () => {
    const { result } = renderHook(() => useProfileForm());
    act(() => result.current.syncWithProfile(profile));
    await waitFor(() => expect(result.current.isValid).toBe(true));

    act(() => result.current.setField('city', ''));

    await waitFor(() => expect(result.current.fieldErrors.city).toBeDefined());
    expect(result.current.isValid).toBe(false);
  });
});
