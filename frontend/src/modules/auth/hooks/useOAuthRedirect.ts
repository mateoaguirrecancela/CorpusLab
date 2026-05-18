import { useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { exchangeOAuthCode } from '@/modules/auth/services/authService';
import { clearSession, storeAuthenticatedSession } from '@/modules/auth/services/sessionService';
import { type LoginResponse } from '@/modules/auth/types/login';
import {
  getOAuthErrorMessage,
  getOAuthProviderLabel,
  getOAuthRedirectParams,
  hasOAuthRedirectParams,
} from '@/modules/auth/utils/oauthRedirect';

const LOGIN_REDIRECT_DELAY_MS = 1600;

const oauthExchangeCache = new Map<string, Promise<LoginResponse>>();

function exchangeOAuthCodeOnce(code: string): Promise<LoginResponse> {
  const cachedExchange = oauthExchangeCache.get(code);

  if (cachedExchange) {
    return cachedExchange;
  }

  const exchange = exchangeOAuthCode(code);
  oauthExchangeCache.set(code, exchange);
  return exchange;
}

export function useOAuthRedirect(searchParams: URLSearchParams) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [errorMessage, setErrorMessage] = useState('');
  const params = useMemo(() => getOAuthRedirectParams(searchParams), [searchParams]);
  const providerLabel = useMemo(
    () => getOAuthProviderLabel(params.provider, t),
    [params.provider, t],
  );

  useEffect(() => {
    let isCurrent = true;
    let loginRedirectTimeout: ReturnType<typeof globalThis.setTimeout> | undefined;

    const redirectToLoginSoon = () => {
      loginRedirectTimeout = globalThis.setTimeout(() => {
        if (isCurrent) {
          navigate('/auth/login', { replace: true });
        }
      }, LOGIN_REDIRECT_DELAY_MS);
    };

    const failOAuthLogin = (message: string) => {
      setErrorMessage(message);
      redirectToLoginSoon();
    };

    const completeOAuthLogin = async () => {
      if (params.errorCode) {
        failOAuthLogin(getOAuthErrorMessage(params.errorCode, t));
        return;
      }

      if (!params.code) {
        failOAuthLogin(t('auth.oauth.errors.missingCode'));
        return;
      }

      try {
        const session = await exchangeOAuthCodeOnce(params.code);

        if (!isCurrent) {
          return;
        }

        storeAuthenticatedSession(queryClient, session);
        navigate('/home', { replace: true });
      } catch {
        if (!isCurrent) {
          return;
        }

        clearSession(queryClient);
        failOAuthLogin(t('auth.oauth.errors.profileLoad'));
      }
    };

    if (hasOAuthRedirectParams(params)) {
      globalThis.history.replaceState(null, '', globalThis.location.pathname);
    }

    void completeOAuthLogin();

    return () => {
      isCurrent = false;

      if (loginRedirectTimeout) {
        globalThis.clearTimeout(loginRedirectTimeout);
      }
    };
  }, [navigate, params, queryClient, t]);

  return {
    errorMessage,
    isCompleting: errorMessage.length === 0,
    providerLabel,
  };
}
