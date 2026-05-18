import { type TFunction } from 'i18next';
import { OAUTH_PROVIDER_LABEL, type OAuthProvider } from '@/modules/auth/constants/session';

const OAUTH_ERROR_KEYS: Record<string, string> = {
  authentication_failed: 'auth.oauth.errors.authenticationFailed',
  invalid_principal: 'auth.oauth.errors.invalidPrincipal',
  missing_email: 'auth.oauth.errors.missingEmail',
  unverified_email: 'auth.oauth.errors.unverifiedEmail',
};

export type OAuthRedirectParams = {
  code: string;
  errorCode: string;
  provider: string;
};

function readSearchParam(searchParams: URLSearchParams, key: string): string {
  return searchParams.get(key)?.trim() ?? '';
}

export function getOAuthRedirectParams(searchParams: URLSearchParams): OAuthRedirectParams {
  return {
    code: readSearchParam(searchParams, 'code'),
    errorCode: readSearchParam(searchParams, 'oauthError'),
    provider: readSearchParam(searchParams, 'provider'),
  };
}

export function hasOAuthRedirectParams(params: OAuthRedirectParams): boolean {
  return Boolean(params.code || params.errorCode || params.provider);
}

export function isOAuthProvider(provider: string): provider is OAuthProvider {
  return provider === 'google' || provider === 'github';
}

export function getOAuthProviderLabel(provider: string, t: TFunction): string {
  if (isOAuthProvider(provider)) {
    return OAUTH_PROVIDER_LABEL[provider];
  }

  return t('auth.oauth.providerFallback');
}

export function getOAuthErrorMessage(errorCode: string, t: TFunction): string {
  if (!errorCode) {
    return t('auth.oauth.errors.default');
  }

  const translationKey = OAUTH_ERROR_KEYS[errorCode];
  if (translationKey) {
    return t(translationKey);
  }

  return t('auth.oauth.errors.withCode', { code: errorCode });
}
