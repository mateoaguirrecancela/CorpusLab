import { type QueryClient, type QueryKey } from '@tanstack/react-query';
import { authQueryKeys } from '@/modules/auth/queryKeys';
import { SESSION_AUTH_TOKEN_STORAGE_KEY } from '@/modules/auth/constants/session';
import { type ProfileResponse } from '@/modules/auth/types/profile';

type AuthSessionProfile = ProfileResponse & {
  token: string;
};

export function getSessionToken(): string | null {
  return localStorage.getItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
}

export function hasSessionToken(): boolean {
  return (getSessionToken()?.trim().length ?? 0) > 0;
}

export function storeSessionToken(token: string): void {
  localStorage.setItem(SESSION_AUTH_TOKEN_STORAGE_KEY, token);
}

export function storeAuthenticatedSession(
  queryClient: QueryClient,
  session: AuthSessionProfile,
): void {
  storeSessionToken(session.token);
  queryClient.setQueryData<ProfileResponse>(authQueryKeys.profile, toSessionProfile(session));
}

function toSessionProfile(session: AuthSessionProfile): ProfileResponse {
  return {
    email: session.email,
    firstName: session.firstName,
    lastName: session.lastName,
    birth: session.birth,
    gender: session.gender,
    countryCode: session.countryCode,
    city: session.city,
  };
}

export function clearSession(
  queryClient: QueryClient,
  additionalQueryKeys: readonly QueryKey[] = [],
): void {
  localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
  queryClient.removeQueries({ queryKey: authQueryKeys.profile });

  for (const queryKey of additionalQueryKeys) {
    queryClient.removeQueries({ queryKey });
  }
}
