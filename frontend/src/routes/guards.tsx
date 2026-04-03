import { type ReactNode } from 'react';
import { Navigate } from 'react-router';
import { SESSION_AUTH_TOKEN_STORAGE_KEY } from '@/modules/auth/constants/session';

function hasSession(): boolean {
  const token = localStorage.getItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
  return Boolean(token && token.trim().length > 0);
}

type GuardProps = {
  children: ReactNode;
};

export function RequireSession({ children }: GuardProps) {
  if (!hasSession()) {
    return <Navigate replace to="/auth/login" />;
  }

  return <>{children}</>;
}

export function PublicOnly({ children }: GuardProps) {
  if (hasSession()) {
    return <Navigate replace to="/home" />;
  }

  return <>{children}</>;
}
