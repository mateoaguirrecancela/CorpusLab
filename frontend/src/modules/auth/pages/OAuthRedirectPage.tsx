import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { FeedbackMessage } from '@/components/ui/feedback-message'
import { Spinner } from '@/components/ui/spinner'
import {
  OAUTH_PROVIDER_LABEL,
  SESSION_AUTH_TOKEN_STORAGE_KEY,
  SESSION_USER_STORAGE_KEY,
} from '@/modules/auth/constants/session'
import { getProfile } from '@/modules/auth/services/authService'

function mapOAuthError(errorCode: string | null): string {
  if (!errorCode) {
    return 'OAuth sign-in could not be completed. Please try again.'
  }

  if (errorCode === 'missing_email') {
    return 'Your OAuth provider did not return an email address. Please use another account.'
  }

  if (errorCode === 'authentication_failed') {
    return 'OAuth authentication failed. Please try again.'
  }

  if (errorCode === 'invalid_principal') {
    return 'Unexpected OAuth response. Please try again.'
  }

  return `OAuth sign-in failed (${errorCode}). Please try again.`
}

export default function OAuthRedirectPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [errorMessage, setErrorMessage] = useState('')

  const token = searchParams.get('token')
  const oauthError = searchParams.get('oauthError')
  const provider = searchParams.get('provider')

  const providerLabel = useMemo(() => {
    if (provider === 'google' || provider === 'github') {
      return OAUTH_PROVIDER_LABEL[provider]
    }

    return 'OAuth'
  }, [provider])

  useEffect(() => {
    const completeOAuthLogin = async () => {
      if (oauthError) {
        setErrorMessage(mapOAuthError(oauthError))
        window.setTimeout(() => {
          navigate('/auth/login', { replace: true })
        }, 1600)
        return
      }

      if (!token) {
        setErrorMessage('OAuth sign-in returned without a token. Please try again.')
        window.setTimeout(() => {
          navigate('/auth/login', { replace: true })
        }, 1600)
        return
      }

      localStorage.setItem(SESSION_AUTH_TOKEN_STORAGE_KEY, token)

      try {
        const profile = await getProfile()
        localStorage.setItem(
          SESSION_USER_STORAGE_KEY,
          JSON.stringify({
            firstName: profile.firstName,
            lastName: profile.lastName,
            email: profile.email,
          }),
        )
      } catch {
        localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY)
        setErrorMessage('OAuth sign-in completed, but we could not load your profile. Please try again.')
        window.setTimeout(() => {
          navigate('/auth/login', { replace: true })
        }, 1600)
        return
      }

      navigate('/home', { replace: true })
    }

    void completeOAuthLogin()
  }, [navigate, oauthError, token])

  return (
    <section className="signup-card w-full max-w-md rounded-xl border border-[color:var(--cl-line)] bg-white/80 p-6 text-center shadow-[0_20px_60px_-45px_rgba(15,23,42,0.75)] backdrop-blur sm:p-8">
      <h1 className="reveal text-3xl font-extrabold tracking-tight text-[color:var(--cl-primary)]">{providerLabel} Sign In</h1>

      {errorMessage.length > 0 ? (
        <FeedbackMessage className="mt-4" message={errorMessage} variant="error" />
      ) : (
        <p className="mt-4 inline-flex items-center gap-2 text-sm text-[color:var(--cl-secondary)]">
          <Spinner aria-hidden className="size-4" />
          Completing authentication...
        </p>
      )}
    </section>
  )
}
