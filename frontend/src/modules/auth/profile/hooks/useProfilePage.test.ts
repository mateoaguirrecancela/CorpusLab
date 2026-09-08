import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useProfilePage } from '@/modules/auth/profile/hooks/useProfilePage';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { getProfileErrorMessage } from '@/modules/auth/services/authService';
import { type ProfileResponse } from '@/modules/auth/types/profile';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

vi.mock('@tanstack/react-query', () => ({
  useQueryClient: () => ({ setQueryData: vi.fn() }),
}));

vi.mock('@/modules/auth/hooks/useProfileQuery', () => ({
  useProfileQuery: vi.fn(),
}));

vi.mock('@/modules/auth/services/authService', () => ({
  getProfileErrorMessage: vi.fn(() => 'profile-load-error'),
  getUpdateProfileErrorMessage: vi.fn(() => 'update-profile-error'),
  updateProfile: vi.fn(),
}));

const profile: ProfileResponse = {
  email: 'a@b.com',
  firstName: 'Jane',
  lastName: 'Doe',
  birth: '2000-01-01',
  gender: 'FEMALE',
  countryCode: 'ES',
  city: 'Coruna',
};

beforeEach(() => {
  vi.mocked(toast.error).mockClear();
});

describe('useProfilePage', () => {
  it('derives initials from the loaded profile', () => {
    vi.mocked(useProfileQuery).mockReturnValue({
      data: profile,
      error: null,
      isError: false,
      isLoading: false,
    } as never);

    const { result } = renderHook(() => useProfilePage());

    expect(result.current.userInitials).toBe('JD');
    expect(result.current.profile).toEqual(profile);
  });

  it('falls back to a generic initial while there is no profile yet', () => {
    vi.mocked(useProfileQuery).mockReturnValue({
      data: undefined,
      error: null,
      isError: false,
      isLoading: true,
    } as never);

    const { result } = renderHook(() => useProfilePage());

    expect(result.current.userInitials).toBe('c');
    expect(result.current.isLoading).toBe(true);
  });

  it('surfaces a load-error toast when the profile query fails', () => {
    vi.mocked(useProfileQuery).mockReturnValue({
      data: undefined,
      error: new Error('network error'),
      isError: true,
      isLoading: false,
    } as never);

    renderHook(() => useProfilePage());

    expect(getProfileErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('profile-load-error', { id: 'profile-load-error' });
  });
});
