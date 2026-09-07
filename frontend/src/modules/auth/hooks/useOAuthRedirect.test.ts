import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useOAuthRedirect } from '@/modules/auth/hooks/useOAuthRedirect';
import { exchangeOAuthCode } from '@/modules/auth/services/authService';
import { clearSession, storeAuthenticatedSession } from '@/modules/auth/services/sessionService';

const navigateMock = vi.fn();
const queryClientMock = { setQueryData: vi.fn(), removeQueries: vi.fn() };

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

vi.mock('react-router', () => ({
  useNavigate: () => navigateMock,
}));

vi.mock('@tanstack/react-query', () => ({
  useQueryClient: () => queryClientMock,
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('@/modules/auth/services/authService', () => ({
  exchangeOAuthCode: vi.fn(),
}));

vi.mock('@/modules/auth/services/sessionService', () => ({
  clearSession: vi.fn(),
  storeAuthenticatedSession: vi.fn(),
}));

beforeEach(() => {
  navigateMock.mockClear();
  vi.mocked(toast.error).mockClear();
  vi.mocked(exchangeOAuthCode).mockReset();
  vi.mocked(storeAuthenticatedSession).mockClear();
  vi.mocked(clearSession).mockClear();
});

describe('useOAuthRedirect', () => {
  it('fails with a missing-code message when there is no code or error', async () => {
    renderHook(() => useOAuthRedirect(new URLSearchParams('')));

    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/auth/login', { replace: true }));
    expect(toast.error).toHaveBeenCalledWith('auth.oauth.errors.missingCode');
  });

  it('fails with the mapped provider error when an oauthError param is present', async () => {
    renderHook(() => useOAuthRedirect(new URLSearchParams('oauthError=missing_email')));

    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/auth/login', { replace: true }));
    expect(toast.error).toHaveBeenCalledWith('auth.oauth.errors.missingEmail');
  });

  it('exchanges a valid code, stores the session and navigates home', async () => {
    vi.mocked(exchangeOAuthCode).mockResolvedValue({
      token: 'tok',
      email: 'a@b.com',
      firstName: 'Jane',
      lastName: 'Doe',
      birth: null,
      gender: null,
      countryCode: null,
      city: null,
    } as never);

    renderHook(() => useOAuthRedirect(new URLSearchParams('code=abc-success')));

    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/home', { replace: true }));
    expect(storeAuthenticatedSession).toHaveBeenCalledWith(
      queryClientMock,
      expect.objectContaining({ firstName: 'Jane' }),
    );
    expect(toast.error).not.toHaveBeenCalled();
  });

  it('clears the session and redirects to login when the exchange fails', async () => {
    vi.mocked(exchangeOAuthCode).mockRejectedValue(new Error('invalid code'));

    renderHook(() => useOAuthRedirect(new URLSearchParams('code=abc-failure')));

    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/auth/login', { replace: true }));
    expect(clearSession).toHaveBeenCalledWith(queryClientMock);
    expect(toast.error).toHaveBeenCalledWith('auth.oauth.errors.profileLoad');
  });

  it('exchanges the same code only once even if the hook re-runs with a new params instance', async () => {
    vi.mocked(exchangeOAuthCode).mockResolvedValue({
      token: 'tok',
      email: 'a@b.com',
      firstName: 'Jane',
      lastName: 'Doe',
      birth: null,
      gender: null,
      countryCode: null,
      city: null,
    } as never);

    const { rerender } = renderHook(
      ({ searchParams }: { searchParams: URLSearchParams }) => useOAuthRedirect(searchParams),
      { initialProps: { searchParams: new URLSearchParams('code=abc-dedup') } },
    );

    await waitFor(() => expect(exchangeOAuthCode).toHaveBeenCalledTimes(1));

    rerender({ searchParams: new URLSearchParams('code=abc-dedup') });

    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/home', { replace: true }));
    expect(exchangeOAuthCode).toHaveBeenCalledTimes(1);
  });

  it('ignores a successful exchange that resolves after the component unmounted', async () => {
    let resolveExchange: (value: unknown) => void = () => {};
    vi.mocked(exchangeOAuthCode).mockReturnValue(
      new Promise((resolve) => {
        resolveExchange = resolve;
      }) as never,
    );

    const { unmount } = renderHook(() => useOAuthRedirect(new URLSearchParams('code=abc-unmount-ok')));
    unmount();
    resolveExchange({
      token: 'tok',
      email: 'a@b.com',
      firstName: 'Jane',
      lastName: 'Doe',
      birth: null,
      gender: null,
      countryCode: null,
      city: null,
    });
    await Promise.resolve();
    await Promise.resolve();

    expect(storeAuthenticatedSession).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('ignores a failed exchange that rejects after the component unmounted', async () => {
    let rejectExchange: (reason: unknown) => void = () => {};
    vi.mocked(exchangeOAuthCode).mockReturnValue(
      new Promise((_resolve, reject) => {
        rejectExchange = reject;
      }) as never,
    );

    const { unmount } = renderHook(() => useOAuthRedirect(new URLSearchParams('code=abc-unmount-fail')));
    unmount();
    rejectExchange(new Error('boom'));
    await Promise.resolve();
    await Promise.resolve();

    expect(clearSession).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('resolves the display label for a known provider and falls back otherwise', () => {
    const known = renderHook(() => useOAuthRedirect(new URLSearchParams('provider=google')));
    expect(known.result.current.providerLabel).not.toBe('auth.oauth.providerFallback');

    const unknown = renderHook(() => useOAuthRedirect(new URLSearchParams('provider=twitter')));
    expect(unknown.result.current.providerLabel).toBe('auth.oauth.providerFallback');
  });
});
