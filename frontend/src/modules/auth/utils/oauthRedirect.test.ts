import { describe, expect, it } from 'vitest';
import { type TFunction } from 'i18next';
import {
  getOAuthErrorMessage,
  getOAuthProviderLabel,
  getOAuthRedirectParams,
  hasOAuthRedirectParams,
  isOAuthProvider,
} from '@/modules/auth/utils/oauthRedirect';

const t = ((key: string, opts?: Record<string, unknown>) =>
  opts ? `${key}:${JSON.stringify(opts)}` : key) as TFunction;

describe('getOAuthRedirectParams', () => {
  it('reads and trims known params', () => {
    const params = getOAuthRedirectParams(
      new URLSearchParams('code=%20abc%20&oauthError=missing_email&provider=google'),
    );

    expect(params).toEqual({ code: 'abc', errorCode: 'missing_email', provider: 'google' });
  });

  it('defaults to empty strings when params are missing', () => {
    const params = getOAuthRedirectParams(new URLSearchParams(''));
    expect(params).toEqual({ code: '', errorCode: '', provider: '' });
  });
});

describe('hasOAuthRedirectParams', () => {
  it('is true when any field is present', () => {
    expect(hasOAuthRedirectParams({ code: 'x', errorCode: '', provider: '' })).toBe(true);
    expect(hasOAuthRedirectParams({ code: '', errorCode: 'x', provider: '' })).toBe(true);
    expect(hasOAuthRedirectParams({ code: '', errorCode: '', provider: 'x' })).toBe(true);
  });

  it('is false when all fields are empty', () => {
    expect(hasOAuthRedirectParams({ code: '', errorCode: '', provider: '' })).toBe(false);
  });
});

describe('isOAuthProvider', () => {
  it('accepts known providers', () => {
    expect(isOAuthProvider('google')).toBe(true);
    expect(isOAuthProvider('github')).toBe(true);
  });

  it('rejects unknown providers', () => {
    expect(isOAuthProvider('facebook')).toBe(false);
    expect(isOAuthProvider('')).toBe(false);
  });
});

describe('getOAuthProviderLabel', () => {
  it('returns the mapped label for known providers', () => {
    expect(getOAuthProviderLabel('google', t)).not.toBe('auth.oauth.providerFallback');
  });

  it('falls back for unknown providers', () => {
    expect(getOAuthProviderLabel('twitter', t)).toBe('auth.oauth.providerFallback');
  });
});

describe('getOAuthErrorMessage', () => {
  it('returns the default message when there is no error code', () => {
    expect(getOAuthErrorMessage('', t)).toBe('auth.oauth.errors.default');
  });

  it('maps known error codes to translation keys', () => {
    expect(getOAuthErrorMessage('missing_email', t)).toBe('auth.oauth.errors.missingEmail');
    expect(getOAuthErrorMessage('invalid_principal', t)).toBe('auth.oauth.errors.invalidPrincipal');
  });

  it('falls back to a generic message with the raw code for unknown errors', () => {
    expect(getOAuthErrorMessage('weird_error', t)).toBe(
      'auth.oauth.errors.withCode:{"code":"weird_error"}',
    );
  });
});
