import { api } from '@/app/config/axiosInstance';
import i18n from '@/app/config/i18n';
import { extractApiErrorMessage } from '@/shared/api/apiErrors';
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
import { type RegisterFormState } from '@/modules/auth/types/signup';
import {
  toForgotPasswordPayload,
  toLoginPayload,
  toOAuthExchangePayload,
  toProfileUpdatePayload,
  toResetPasswordPayload,
  toSignupPayload,
} from '@/modules/auth/utils/authPayloads';

const AUTH_ERROR_KEYS = {
  accountRecovery: 'auth.errors.unexpected.accountRecovery',
  credentialUpdate: 'auth.errors.unexpected.credentialUpdate',
  login: 'auth.errors.unexpected.login',
  logout: 'auth.errors.unexpected.logout',
  profile: 'auth.errors.unexpected.profile',
  signup: 'auth.errors.unexpected.signup',
  updateProfile: 'auth.errors.unexpected.updateProfile',
} as const;

function getAuthErrorMessage(error: unknown, key: keyof typeof AUTH_ERROR_KEYS): string {
  return extractApiErrorMessage(error, i18n.t(AUTH_ERROR_KEYS[key]));
}

export async function signup(form: RegisterFormState): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>('/auth/signup', toSignupPayload(form));
  return response.data;
}

export async function login(form: LoginFormState): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>('/auth/login', toLoginPayload(form));
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
  const response = await api.put<ProfileResponse>('/auth/profile', toProfileUpdatePayload(payload));
  return response.data;
}

export async function requestPasswordReset(
  form: ForgotPasswordFormState,
): Promise<ForgotPasswordResponse> {
  const response = await api.post<ForgotPasswordResponse>(
    '/auth/forgot-password',
    toForgotPasswordPayload(form),
  );
  return response.data;
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<ResetPasswordResponse> {
  const response = await api.post<ResetPasswordResponse>(
    '/auth/reset-password',
    toResetPasswordPayload(payload),
  );
  return response.data;
}

export async function exchangeOAuthCode(code: string): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>(
    '/auth/oauth/exchange',
    toOAuthExchangePayload(code),
  );
  return response.data;
}

export function getLoginErrorMessage(error: unknown): string {
  return getAuthErrorMessage(error, 'login');
}

export function getRegisterErrorMessage(error: unknown): string {
  return getAuthErrorMessage(error, 'signup');
}

export function getLogoutErrorMessage(error: unknown): string {
  return getAuthErrorMessage(error, 'logout');
}

export function getForgotPasswordErrorMessage(error: unknown): string {
  return getAuthErrorMessage(error, 'accountRecovery');
}

export function getResetPasswordErrorMessage(error: unknown): string {
  return getAuthErrorMessage(error, 'credentialUpdate');
}

export function getProfileErrorMessage(error: unknown): string {
  return getAuthErrorMessage(error, 'profile');
}

export function getUpdateProfileErrorMessage(error: unknown): string {
  return getAuthErrorMessage(error, 'updateProfile');
}

export function getOAuthAuthorizationUrl(provider: OAuthProvider): string {
  return `/oauth2/authorization/${provider}`;
}

export function redirectToOAuthAuthorization(provider: OAuthProvider): void {
  globalThis.location.assign(getOAuthAuthorizationUrl(provider));
}
