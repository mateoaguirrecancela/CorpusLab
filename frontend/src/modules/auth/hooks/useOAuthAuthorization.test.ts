import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useOAuthAuthorization } from '@/modules/auth/hooks/useOAuthAuthorization';
import { redirectToOAuthAuthorization } from '@/modules/auth/services/authService';

vi.mock('@/modules/auth/services/authService', () => ({
  redirectToOAuthAuthorization: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(redirectToOAuthAuthorization).mockClear();
});

describe('useOAuthAuthorization', () => {
  it('redirects to the provider authorization url when enabled', () => {
    const { result } = renderHook(() => useOAuthAuthorization(false));

    result.current('google');

    expect(redirectToOAuthAuthorization).toHaveBeenCalledWith('google');
  });

  it('does nothing while disabled', () => {
    const { result } = renderHook(() => useOAuthAuthorization(true));

    result.current('github');

    expect(redirectToOAuthAuthorization).not.toHaveBeenCalled();
  });
});
