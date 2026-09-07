import { renderHook } from '@testing-library/react';
import { useQuery } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';
import { useAuth } from '@/modules/auth/hooks/useAuth';
import { hasSessionToken } from '@/modules/auth/services/sessionService';

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
}));

vi.mock('@/modules/auth/services/sessionService', () => ({
  hasSessionToken: vi.fn(),
}));

describe('useAuth', () => {
  it('is unauthenticated and not loading when there is no session token', () => {
    vi.mocked(hasSessionToken).mockReturnValue(false);
    vi.mocked(useQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
      isFetching: true,
      isError: false,
    } as never);

    const { result } = renderHook(() => useAuth());

    expect(result.current).toEqual({ isAuthenticated: false, isLoading: false });
  });

  it('is authenticated once a token exists and the profile loads without error', () => {
    vi.mocked(hasSessionToken).mockReturnValue(true);
    vi.mocked(useQuery).mockReturnValue({
      data: { email: 'a@b.com' },
      isLoading: false,
      isFetching: false,
      isError: false,
    } as never);

    const { result } = renderHook(() => useAuth());

    expect(result.current).toEqual({ isAuthenticated: true, isLoading: false });
  });

  it('is not authenticated when the profile query errors despite having a token', () => {
    vi.mocked(hasSessionToken).mockReturnValue(true);
    vi.mocked(useQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isFetching: false,
      isError: true,
    } as never);

    const { result } = renderHook(() => useAuth());

    expect(result.current.isAuthenticated).toBe(false);
  });

  it('reports loading while the token exists and the query is still fetching', () => {
    vi.mocked(hasSessionToken).mockReturnValue(true);
    vi.mocked(useQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isFetching: true,
      isError: false,
    } as never);

    const { result } = renderHook(() => useAuth());

    expect(result.current.isLoading).toBe(true);
  });
});
