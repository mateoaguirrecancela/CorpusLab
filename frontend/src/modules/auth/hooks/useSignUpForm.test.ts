import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useSignUpForm } from '@/modules/auth/hooks/useSignUpForm';
import { getRegisterErrorMessage, signup } from '@/modules/auth/services/authService';
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
  getRegisterErrorMessage: vi.fn(() => 'signup-error'),
  signup: vi.fn(),
}));

vi.mock('@/modules/auth/services/sessionService', () => ({
  storeAuthenticatedSession: vi.fn(),
}));

function isoDateYearsAgo(years: number): string {
  const date = new Date();
  date.setFullYear(date.getFullYear() - years);
  return date.toISOString().slice(0, 10);
}

function fakeEvent() {
  return { preventDefault: vi.fn() } as unknown as React.FormEvent<HTMLFormElement>;
}

async function fillValidForm(result: { current: ReturnType<typeof useSignUpForm> }) {
  act(() => {
    result.current.updateField('firstName', 'Jane');
    result.current.updateField('lastName', 'Doe');
    result.current.updateField('email', 'a@b.com');
    result.current.updateField('password', 'password1');
    result.current.updateField('birth', isoDateYearsAgo(20));
    result.current.updateField('gender', 'FEMALE');
    result.current.updateField('countryCode', 'ES');
    result.current.updateField('city', 'Coruna');
  });
  await waitFor(() => expect(result.current.canSubmit).toBe(true));
}

beforeEach(() => {
  vi.mocked(signup).mockReset().mockResolvedValue({
    token: 'tok',
    email: 'a@b.com',
    firstName: 'Jane',
    lastName: 'Doe',
    birth: isoDateYearsAgo(20),
    gender: 'FEMALE',
    countryCode: 'ES',
    city: 'Coruna',
  } as never);
  navigateMock.mockClear();
  vi.mocked(storeAuthenticatedSession).mockClear();
});

describe('useSignUpForm', () => {
  it('signs up, stores the session and navigates home on success', async () => {
    const { result } = renderHook(() => useSignUpForm());
    await fillValidForm(result);

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(signup).toHaveBeenCalled();
    expect(storeAuthenticatedSession).toHaveBeenCalled();
    expect(navigateMock).toHaveBeenCalledWith('/home');
    expect(result.current.successMessage).toContain('Jane');
  });

  it('resets the whole form back to its initial state on success', async () => {
    const { result } = renderHook(() => useSignUpForm());
    await fillValidForm(result);

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(result.current.form.email).toBe('');
    expect(result.current.form.firstName).toBe('');
  });

  it('shows the mapped error message and does not navigate when signup fails', async () => {
    vi.mocked(signup).mockRejectedValue(new Error('email taken'));
    const { result } = renderHook(() => useSignUpForm());
    await fillValidForm(result);

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(getRegisterErrorMessage).toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('signup-error');
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('rejects submission while under the minimum age, without calling signup', async () => {
    const { result } = renderHook(() => useSignUpForm());
    act(() => {
      result.current.updateField('firstName', 'Jane');
      result.current.updateField('lastName', 'Doe');
      result.current.updateField('email', 'a@b.com');
      result.current.updateField('password', 'password1');
      result.current.updateField('birth', isoDateYearsAgo(10));
      result.current.updateField('gender', 'FEMALE');
      result.current.updateField('countryCode', 'ES');
      result.current.updateField('city', 'Coruna');
    });

    await waitFor(() => expect(result.current.canSubmit).toBe(false));

    await act(async () => {
      await result.current.handleSubmit(fakeEvent());
    });

    expect(signup).not.toHaveBeenCalled();
  });
});
