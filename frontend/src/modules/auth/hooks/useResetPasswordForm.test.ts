import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useResetPasswordForm } from '@/modules/auth/hooks/useResetPasswordForm';
import { getResetPasswordErrorMessage, resetPassword } from '@/modules/auth/services/authService';

const navigateMock = vi.fn();

vi.mock('react-router', () => ({
  useNavigate: () => navigateMock,
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('@/modules/auth/services/authService', () => ({
  getResetPasswordErrorMessage: vi.fn(() => 'reset-password-error'),
  resetPassword: vi.fn(),
}));

function fakeEvent() {
  return { preventDefault: vi.fn() } as unknown as React.FormEvent<HTMLFormElement>;
}

const validToken = 'a'.repeat(16);

beforeEach(() => {
  vi.mocked(resetPassword).mockReset().mockResolvedValue({ message: 'ok' });
  navigateMock.mockClear();
});

describe('useResetPasswordForm', () => {
  it('pre-fills the token from the initial value and flags a missing one', () => {
    const withToken = renderHook(() => useResetPasswordForm(validToken));
    expect(withToken.result.current.form.token).toBe(validToken);
    expect(withToken.result.current.errorMessage).toBe('');

    const withoutToken = renderHook(() => useResetPasswordForm(''));
    expect(withoutToken.result.current.errorMessage).toBe('auth.resetPassword.missingToken');
  });

  it('resets the password and navigates to login on success', async () => {
    const { result } = renderHook(() => useResetPasswordForm(validToken));
    act(() => {
      result.current.updateField('newPassword', 'password1');
      result.current.updateField('confirmPassword', 'password1');
    });
    await waitFor(() => expect(result.current.canSubmit).toBe(true));

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(resetPassword).toHaveBeenCalledWith({ token: validToken, newPassword: 'password1' });
    expect(navigateMock).toHaveBeenCalledWith('/auth/login', { replace: true });
  });

  it('rejects submission when the passwords do not match', async () => {
    const { result } = renderHook(() => useResetPasswordForm(validToken));
    act(() => {
      result.current.updateField('newPassword', 'password1');
      result.current.updateField('confirmPassword', 'password2');
    });

    await waitFor(() => expect(result.current.fieldErrors.confirmPassword).toBeDefined());
    expect(result.current.canSubmit).toBe(false);
  });

  it('shows the mapped error message and does not navigate when the request fails', async () => {
    vi.mocked(resetPassword).mockRejectedValue(new Error('expired token'));
    const { result } = renderHook(() => useResetPasswordForm(validToken));
    act(() => {
      result.current.updateField('newPassword', 'password1');
      result.current.updateField('confirmPassword', 'password1');
    });
    await waitFor(() => expect(result.current.canSubmit).toBe(true));

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(getResetPasswordErrorMessage).toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('reset-password-error');
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
