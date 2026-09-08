import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useForgotPasswordForm } from '@/modules/auth/hooks/useForgotPasswordForm';
import {
  getForgotPasswordErrorMessage,
  requestPasswordReset,
} from '@/modules/auth/services/authService';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('@/modules/auth/services/authService', () => ({
  getForgotPasswordErrorMessage: vi.fn(() => 'forgot-password-error'),
  requestPasswordReset: vi.fn(),
}));

function fakeEvent() {
  return { preventDefault: vi.fn() } as unknown as React.FormEvent<HTMLFormElement>;
}

beforeEach(() => {
  vi.mocked(requestPasswordReset).mockReset().mockResolvedValue({ message: 'check your email' });
});

describe('useForgotPasswordForm', () => {
  it('requests a reset and surfaces the server success message', async () => {
    const { result } = renderHook(() => useForgotPasswordForm());
    act(() => result.current.updateField('email', 'a@b.com'));
    await waitFor(() => expect(result.current.canSubmit).toBe(true));

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(requestPasswordReset).toHaveBeenCalledWith({ email: 'a@b.com' });
    expect(result.current.successMessage).toBe('check your email');
  });

  it('rejects an empty submission without calling the service', async () => {
    const { result } = renderHook(() => useForgotPasswordForm());

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(requestPasswordReset).not.toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('auth.forgotPassword.required');
  });

  it('shows the mapped error message when the request fails', async () => {
    vi.mocked(requestPasswordReset).mockRejectedValue(new Error('down'));
    const { result } = renderHook(() => useForgotPasswordForm());
    act(() => result.current.updateField('email', 'a@b.com'));
    await waitFor(() => expect(result.current.canSubmit).toBe(true));

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(getForgotPasswordErrorMessage).toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('forgot-password-error');
  });
});
