import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useProfileEditor } from '@/modules/auth/profile/hooks/useProfileEditor';
import { getUpdateProfileErrorMessage, updateProfile } from '@/modules/auth/services/authService';
import { type ProfileResponse } from '@/modules/auth/types/profile';

const queryClientMock = { setQueryData: vi.fn() };

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

vi.mock('@tanstack/react-query', () => ({
  useQueryClient: () => queryClientMock,
}));

vi.mock('@/modules/auth/services/authService', () => ({
  getUpdateProfileErrorMessage: vi.fn(() => 'update-profile-error'),
  updateProfile: vi.fn(),
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

beforeEach(() => {
  vi.mocked(updateProfile).mockReset().mockResolvedValue(profile);
  vi.mocked(toast.error).mockClear();
  vi.mocked(toast.success).mockClear();
  queryClientMock.setQueryData.mockClear();
});

describe('useProfileEditor', () => {
  it('does nothing when starting to edit without a loaded profile', () => {
    const { result } = renderHook(() => useProfileEditor(undefined));

    act(() => result.current.startEditing());

    expect(result.current.isEditing).toBe(false);
  });

  it('syncs the form from the profile and enters edit mode', async () => {
    const { result } = renderHook(() => useProfileEditor(profile));

    act(() => result.current.startEditing());

    expect(result.current.isEditing).toBe(true);
    await waitFor(() => expect(result.current.canSave).toBe(true));
    expect(result.current.form.firstName).toBe('Jane');
  });

  it('cancelEditing restores the original profile values and leaves edit mode', async () => {
    const { result } = renderHook(() => useProfileEditor(profile));
    act(() => result.current.startEditing());
    await waitFor(() => expect(result.current.canSave).toBe(true));

    act(() => result.current.setField('city', 'Vigo'));
    await waitFor(() => expect(result.current.form.city).toBe('Vigo'));

    act(() => result.current.cancelEditing());

    expect(result.current.isEditing).toBe(false);
    expect(result.current.form.city).toBe('Coruna');
  });

  it('rejects saving an invalid form without calling the service', async () => {
    const { result } = renderHook(() => useProfileEditor(profile));
    act(() => result.current.startEditing());
    await waitFor(() => expect(result.current.canSave).toBe(true));

    act(() => result.current.setField('city', ''));
    await waitFor(() => expect(result.current.canSave).toBe(false));

    await act(async () => {
      await result.current.saveProfile();
    });

    expect(updateProfile).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('home.profile.requiredFields');
  });

  it('saves a valid form, updates the cache and exits edit mode', async () => {
    const updated = { ...profile, city: 'Vigo' };
    vi.mocked(updateProfile).mockResolvedValue(updated);
    const { result } = renderHook(() => useProfileEditor(profile));
    act(() => result.current.startEditing());
    await waitFor(() => expect(result.current.canSave).toBe(true));

    await act(async () => {
      await result.current.saveProfile();
    });

    expect(updateProfile).toHaveBeenCalled();
    expect(queryClientMock.setQueryData).toHaveBeenCalledWith(expect.anything(), updated);
    expect(result.current.isEditing).toBe(false);
    expect(toast.success).toHaveBeenCalledWith('home.profile.updated');
  });

  it('shows an error toast and stays in edit mode when saving fails', async () => {
    vi.mocked(updateProfile).mockRejectedValue(new Error('network error'));
    const { result } = renderHook(() => useProfileEditor(profile));
    act(() => result.current.startEditing());
    await waitFor(() => expect(result.current.canSave).toBe(true));

    await act(async () => {
      await result.current.saveProfile();
    });

    expect(getUpdateProfileErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('update-profile-error');
    expect(result.current.isEditing).toBe(true);
    expect(result.current.isSaving).toBe(false);
  });
});
