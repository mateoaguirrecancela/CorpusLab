import { useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router';
import { useToastMessages } from '@/hooks/useToastMessages';
import { Spinner } from '@/components/ui/spinner';
import { AuthCard } from '@/modules/auth/components/AuthCard';
import { OAUTH_PROVIDER_LABEL } from '@/modules/auth/constants/session';
import { PROFILE_QUERY_KEY } from '@/modules/auth/constants/queryKeys';
import { exchangeOAuthCode, getProfile } from '@/modules/auth/services/authService';
import { clearSession, storeSessionToken } from '@/modules/auth/services/sessionService';

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

  if (errorCode === 'unverified_email') {
    return t('auth.oauth.errors.unverifiedEmail');
  }

  return t('auth.oauth.errors.withCode', { code: errorCode });
}

export default function OAuthRedirectPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const [errorMessage, setErrorMessage] = useState('');

  const code = searchParams.get('code');
  const oauthError = searchParams.get('oauthError');
  const provider = searchParams.get('provider');

  useEffect(() => {
    if (code || oauthError || provider) {
      globalThis.history.replaceState(null, '', globalThis.location.pathname);
    }
  }, [code, oauthError, provider]);

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

      if (!code) {
        setErrorMessage(t('auth.oauth.errors.missingCode'));
        globalThis.setTimeout(() => {
          navigate('/auth/login', { replace: true });
        }, 1600);
        return;
      }

      try {
        const session = await exchangeOAuthCode(code);
        storeSessionToken(session.token);
        const profile = await getProfile();
        queryClient.setQueryData(PROFILE_QUERY_KEY, profile);
      } catch {
        clearSession(queryClient);
        setErrorMessage(t('auth.oauth.errors.profileLoad'));
        globalThis.setTimeout(() => {
          navigate('/auth/login', { replace: true });
        }, 1600);
        return;
      }

      navigate('/home', { replace: true });
    };

    void completeOAuthLogin();
  }, [code, navigate, oauthError, queryClient, t]);

  useToastMessages({ errorMessage });

  return (
    <AuthCard
      className="max-w-md text-center"
      title={t('auth.oauth.title', { provider: providerLabel })}
    >
      {errorMessage.length > 0 ? (
        <p className="mt-4 text-sm text-destructive">{errorMessage}</p>
      ) : (
        <p className="mt-4 inline-flex items-center gap-2 text-sm text-muted-foreground">
          <Spinner aria-hidden className="size-4" />
          {t('auth.oauth.completing')}
        </p>
      )}
    </AuthCard>
  );
}
