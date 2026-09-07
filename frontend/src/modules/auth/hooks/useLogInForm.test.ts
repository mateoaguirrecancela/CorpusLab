import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useLogInForm } from '@/modules/auth/hooks/useLogInForm';
import { getLoginErrorMessage, login } from '@/modules/auth/services/authService';
import { storeAuthenticatedSession } from '@/modules/auth/services/sessionService';

const navigateMock = vi.fn();
const queryClientMock = { setQueryData: vi.fn() };

vi.mock('react-router', () => ({
  useNavigate: () => navigateMock,
}));

vi.mock('@tanstack/react-query', () => ({
  useQueryClient: () => queryClientMock,
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) =>
      opts ? `${key}:${JSON.stringify(opts)}` : key,
  }),
}));

vi.mock('@/modules/auth/services/authService', () => ({
  getLoginErrorMessage: vi.fn(() => 'login-error'),
  login: vi.fn(),
}));

vi.mock('@/modules/auth/services/sessionService', () => ({
  storeAuthenticatedSession: vi.fn(),
}));

function fakeEvent() {
  return { preventDefault: vi.fn() } as unknown as React.FormEvent<HTMLFormElement>;
}

async function fillValidForm(result: { current: ReturnType<typeof useLogInForm> }) {
  act(() => result.current.updateField('email', 'a@b.com'));
  act(() => result.current.updateField('password', 'password1'));
  await waitFor(() => expect(result.current.canSubmit).toBe(true));
}

beforeEach(() => {
  vi.mocked(login).mockReset().mockResolvedValue({
    token: 'tok',
    email: 'a@b.com',
    firstName: 'Jane',
    lastName: 'Doe',
    birth: null,
    gender: null,
    countryCode: null,
    city: null,
  } as never);
  navigateMock.mockClear();
  vi.mocked(storeAuthenticatedSession).mockClear();
});

describe('useLogInForm', () => {
  it('logs in, stores the session and navigates home on success', async () => {
    const { result } = renderHook(() => useLogInForm());
    await fillValidForm(result);

    await act(async () => {
      await result.current.handleSubmit({
        preventDefault: vi.fn(),
      } as unknown as React.FormEvent<HTMLFormElement>);
    });

    expect(login).toHaveBeenCalledWith({ email: 'a@b.com', password: 'password1' });
    expect(storeAuthenticatedSession).toHaveBeenCalledWith(
      queryClientMock,
      expect.objectContaining({ firstName: 'Jane' }),
    );
    expect(navigateMock).toHaveBeenCalledWith('/home');
    expect(result.current.successMessage).toContain('Jane');
  });

  it('shows the mapped error message and does not navigate when login fails', async () => {
    vi.mocked(login).mockRejectedValue(new Error('bad credentials'));
    const { result } = renderHook(() => useLogInForm());
    await fillValidForm(result);

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(getLoginErrorMessage).toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('login-error');
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
