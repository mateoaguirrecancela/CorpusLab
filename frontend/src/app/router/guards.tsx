import { type ReactNode } from 'react';
import { Navigate } from 'react-router';
import { SESSION_AUTH_TOKEN_STORAGE_KEY } from '@/modules/auth/constants/session';
import { getSessionToken } from '@/modules/auth/services/sessionService';

type GuardProps = {
  children: ReactNode;
};

function decodeJwtPayload(token: string): { exp?: number } | null {
  const parts = token.split('.');
  if (parts.length < 2) {
    return null;
  }

  try {
    const normalized = parts[1].replaceAll('-', '+').replaceAll('_', '/');
    const paddingLength = (4 - (normalized.length % 4)) % 4;
    const padded = normalized + '='.repeat(paddingLength);
    const payload = JSON.parse(atob(padded)) as { exp?: number };
    return payload;
  } catch {
    return null;
  }
}

function getValidToken(): string | null {
  const token = getSessionToken();
  if (!token || token.trim().length === 0) {
    return null;
  }

  const payload = decodeJwtPayload(token);
  if (!payload || typeof payload.exp !== 'number') {
    localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
    return null;
  }

  const nowInSeconds = Math.floor(Date.now() / 1000);
  if (payload.exp <= nowInSeconds) {
    localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
    return null;
  }

  return token;
}

export function RequireSession({ children }: Readonly<GuardProps>) {
  if (!getValidToken()) {
    return <Navigate replace to="/auth/login" />;
  }

  return <>{children}</>;
}

export function PublicOnly({ children }: Readonly<GuardProps>) {
  if (getValidToken()) {
    return <Navigate replace to="/home" />;
  }

  return <>{children}</>;
}
