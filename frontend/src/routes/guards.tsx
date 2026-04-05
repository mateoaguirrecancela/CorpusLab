import { type ReactNode } from 'react';
import { Navigate } from 'react-router';
import { hasSessionToken } from '@/modules/auth/services/sessionService';

type GuardProps = {
  children: ReactNode;
};

export function RequireSession({ children }: GuardProps) {
  if (!hasSessionToken()) {
    return <Navigate replace to="/auth/login" />;
  }

  return <>{children}</>;
}

export function PublicOnly({ children }: GuardProps) {
  if (hasSessionToken()) {
    return <Navigate replace to="/home" />;
  }

  return <>{children}</>;
}
