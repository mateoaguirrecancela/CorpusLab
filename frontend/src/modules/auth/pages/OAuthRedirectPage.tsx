import { useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import { Spinner } from '@/components/ui/spinner';
import {
  OAUTH_PROVIDER_LABEL,
  SESSION_AUTH_TOKEN_STORAGE_KEY,
} from '@/modules/auth/constants/session';
import { PROFILE_QUERY_KEY } from '@/modules/auth/hooks/useProfileQuery';
import { getProfile } from '@/modules/auth/services/authService';

function mapOAuthError(
  errorCode: string | null,
  t: (key: string, options?: Record<string, string>) => string,
): string {
  if (!errorCode) {
    return t('auth.oauth.errors.default');
  }

  if (errorCode === 'missing_email') {
    return t('auth.oauth.errors.missingEmail');
  }

  if (errorCode === 'authentication_failed') {
    return t('auth.oauth.errors.authenticationFailed');
  }

  if (errorCode === 'invalid_principal') {
    return t('auth.oauth.errors.invalidPrincipal');
  }

  return t('auth.oauth.errors.withCode', { code: errorCode });
}

export default function OAuthRedirectPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const [errorMessage, setErrorMessage] = useState('');

  const token = searchParams.get('token');
  const oauthError = searchParams.get('oauthError');
  const provider = searchParams.get('provider');

  const providerLabel = useMemo(() => {
    if (provider === 'google' || provider === 'github') {
      return OAUTH_PROVIDER_LABEL[provider];
    }

    return t('auth.oauth.providerFallback');
  }, [provider, t]);

  useEffect(() => {
    const completeOAuthLogin = async () => {
      if (oauthError) {
        setErrorMessage(mapOAuthError(oauthError, t));
        globalThis.setTimeout(() => {
          navigate('/auth/login', { replace: true });
        }, 1600);
        return;
      }

      if (!token) {
        setErrorMessage(t('auth.oauth.errors.missingToken'));
        globalThis.setTimeout(() => {
          navigate('/auth/login', { replace: true });
        }, 1600);
        return;
      }

      localStorage.setItem(SESSION_AUTH_TOKEN_STORAGE_KEY, token);

      try {
        const profile = await getProfile();
        queryClient.setQueryData(PROFILE_QUERY_KEY, profile);
      } catch {
        localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
        setErrorMessage(t('auth.oauth.errors.profileLoad'));
        globalThis.setTimeout(() => {
          navigate('/auth/login', { replace: true });
        }, 1600);
        return;
      }

      navigate('/home', { replace: true });
    };

    void completeOAuthLogin();
  }, [navigate, oauthError, queryClient, t, token]);

  useEffect(() => {
    if (errorMessage.trim().length > 0) {
      toast.error(errorMessage);
    }
  }, [errorMessage]);

  return (
    <section className="signup-card w-full max-w-md rounded-xl border border-[color:var(--cl-line)] bg-white/80 p-6 text-center shadow-[0_20px_60px_-45px_rgba(15,23,42,0.75)] backdrop-blur sm:p-8">
      <h1 className="reveal text-3xl font-extrabold tracking-tight text-[color:var(--cl-primary)]">
        {t('auth.oauth.title', { provider: providerLabel })}
      </h1>

      {errorMessage.length > 0 ? (
        <p className="mt-4 text-sm text-red-700">{errorMessage}</p>
      ) : (
        <p className="mt-4 inline-flex items-center gap-2 text-sm text-[color:var(--cl-secondary)]">
          <Spinner aria-hidden className="size-4" />
          {t('auth.oauth.completing')}
        </p>
      )}
    </section>
  );
}
