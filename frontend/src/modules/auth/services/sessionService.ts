import { type QueryClient, type QueryKey } from '@tanstack/react-query';
import { PROFILE_QUERY_KEY } from '@/modules/auth/constants/queryKeys';
import { SESSION_AUTH_TOKEN_STORAGE_KEY } from '@/modules/auth/constants/session';

export function getSessionToken(): string | null {
  return localStorage.getItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
}

export function hasSessionToken(): boolean {
  const token = getSessionToken();
  return Boolean(token && token.trim().length > 0);
}

export function storeSessionToken(token: string): void {
  localStorage.setItem(SESSION_AUTH_TOKEN_STORAGE_KEY, token);
}

export function clearSession(
  queryClient: QueryClient,
  additionalQueryKeys: readonly QueryKey[] = [],
): void {
  localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
  queryClient.removeQueries({ queryKey: PROFILE_QUERY_KEY });

  for (const queryKey of additionalQueryKeys) {
    queryClient.removeQueries({ queryKey });
  }
}
