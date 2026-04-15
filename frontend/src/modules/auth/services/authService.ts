import { api } from '@/lib/api';
import { extractApiErrorMessage } from '@/lib/apiErrors';
import i18n from '@/lib/i18n';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import {
  type LoginFormState,
  type LoginResponse,
  type LogoutResponse,
} from '@/modules/auth/types/login';
import { type ProfileResponse, type UpdateProfilePayload } from '@/modules/auth/types/profile';
import {
  type ForgotPasswordFormState,
  type ForgotPasswordResponse,
} from '@/modules/auth/types/forgotPassword';
import {
  type ResetPasswordPayload,
  type ResetPasswordResponse,
} from '@/modules/auth/types/resetPassword';
import { type RegisterFormState, type RegisterResponse } from '@/modules/auth/types/signup';

const OAUTH_BACKEND_BASE_URL =
  (import.meta.env.VITE_BACKEND_URL as string | undefined)?.replace(/\/$/, '') ??
  'http://localhost:8080';

export async function signup(form: RegisterFormState): Promise<RegisterResponse> {
  const payload = {
    email: form.email.trim().toLowerCase(),
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    birth: form.birth,
    gender: form.gender.length > 0 ? form.gender : undefined,
    countryCode: form.countryCode,
    city: form.city.trim(),
    password: form.password,
  };

  const response = await api.post<RegisterResponse>('/auth/signup', payload);
  return response.data;
}

export async function login(form: LoginFormState): Promise<LoginResponse> {
  const payload = {
    email: form.email.trim().toLowerCase(),
    password: form.password,
  };

  const response = await api.post<LoginResponse>('/auth/login', payload);
  return response.data;
}

export async function logout(): Promise<LogoutResponse> {
  const response = await api.post<LogoutResponse>('/auth/logout');
  return response.data;
}

export async function getProfile(): Promise<ProfileResponse> {
  const response = await api.get<ProfileResponse>('/auth/profile');
  return response.data;
}

export async function updateProfile(payload: UpdateProfilePayload): Promise<ProfileResponse> {
  const body = {
    firstName: payload.firstName.trim(),
    lastName: payload.lastName.trim(),
    birth: payload.birth,
    gender: payload.gender,
    countryCode: payload.countryCode,
    city: payload.city,
  };

  const response = await api.put<ProfileResponse>('/auth/profile', body);
  return response.data;
}

export async function requestPasswordReset(
  form: ForgotPasswordFormState,
): Promise<ForgotPasswordResponse> {
  const payload = {
    email: form.email.trim().toLowerCase(),
  };

  const response = await api.post<ForgotPasswordResponse>('/auth/forgot-password', payload);
  return response.data;
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<ResetPasswordResponse> {
  const body = {
    token: payload.token.trim(),
    newPassword: payload.newPassword,
  };

  const response = await api.post<ResetPasswordResponse>('/auth/reset-password', body);
  return response.data;
}

export function getLoginErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('auth.errors.unexpected.login'));
}

export function getRegisterErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('auth.errors.unexpected.signup'));
}

export function getLogoutErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('auth.errors.unexpected.logout'));
}

export function getForgotPasswordErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('auth.errors.unexpected.forgotPassword'));
}

export function getResetPasswordErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('auth.errors.unexpected.resetPassword'));
}

export function getProfileErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('auth.errors.unexpected.profile'));
}

export function getUpdateProfileErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('auth.errors.unexpected.updateProfile'));
}

export function getOAuthAuthorizationUrl(provider: OAuthProvider): string {
  return `${OAUTH_BACKEND_BASE_URL}/oauth2/authorization/${provider}`;
}

export function redirectToOAuthAuthorization(provider: OAuthProvider): void {
  globalThis.location.assign(getOAuthAuthorizationUrl(provider));
}

function decodeBase64Url(input: string): string | null {
  try {
    const normalized = input.replaceAll('-', '+').replaceAll('_', '/');
    const paddingLength = (4 - (normalized.length % 4)) % 4;
    const padded = normalized + '='.repeat(paddingLength);
    return atob(padded);
  } catch {
    return null;
  }
}

export function extractEmailFromJwt(token: string): string | null {
  const parts = token.split('.');
  if (parts.length < 2) {
    return null;
  }

  const payloadRaw = decodeBase64Url(parts[1]);
  if (!payloadRaw) {
    return null;
  }

  try {
    const payload = JSON.parse(payloadRaw) as { email?: unknown; sub?: unknown };
    if (typeof payload.email === 'string' && payload.email.trim().length > 0) {
      return payload.email.trim().toLowerCase();
    }

    if (typeof payload.sub === 'string' && payload.sub.includes('@')) {
      return payload.sub.trim().toLowerCase();
    }

    return null;
  } catch {
    return null;
  }
}
