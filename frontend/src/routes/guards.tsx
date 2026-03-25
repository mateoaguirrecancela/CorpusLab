import { type ReactNode } from 'react'
import { Navigate } from 'react-router'
import { SESSION_USER_STORAGE_KEY } from '@/modules/auth/constants/session'

function hasSession(): boolean {
  try {
    const rawValue = localStorage.getItem(SESSION_USER_STORAGE_KEY)

    if (!rawValue) {
      return false
    }

    const parsedValue = JSON.parse(rawValue) as {
      email?: string
      firstName?: string
      lastName?: string
    }

    return Boolean(parsedValue.email || parsedValue.firstName || parsedValue.lastName)
  } catch {
    return false
  }
}

type GuardProps = {
  children: ReactNode
}

export function RequireSession({ children }: GuardProps) {
  if (!hasSession()) {
    return <Navigate replace to="/auth/login" />
  }

  return <>{children}</>
}

export function PublicOnly({ children }: GuardProps) {
  if (hasSession()) {
    return <Navigate replace to="/home" />
  }

  return <>{children}</>
}
