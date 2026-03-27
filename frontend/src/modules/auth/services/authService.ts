import { api } from '@/lib/api'
import { type LoginFormState, type LoginResponse, type LogoutResponse } from '@/modules/auth/types/login'
import { type ProfileResponse, type UpdateProfilePayload } from '@/modules/auth/types/profile'
import { type ForgotPasswordFormState, type ForgotPasswordResponse } from '@/modules/auth/types/forgotPassword'
import { type ResetPasswordPayload, type ResetPasswordResponse } from '@/modules/auth/types/resetPassword'
import { type RegisterFormState, type RegisterResponse } from '@/modules/auth/types/signup'

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
  }

  const response = await api.post<RegisterResponse>('/auth/signup', payload)
  return response.data
}

export async function login(form: LoginFormState): Promise<LoginResponse> {
  const payload = {
    email: form.email.trim().toLowerCase(),
    password: form.password,
  }

  const response = await api.post<LoginResponse>('/auth/login', payload)
  return response.data
}

export async function logout(): Promise<LogoutResponse> {
  const response = await api.post<LogoutResponse>('/auth/logout')
  return response.data
}

export async function getProfile(): Promise<ProfileResponse> {
  const response = await api.get<ProfileResponse>('/auth/profile')
  return response.data
}

export async function updateProfile(payload: UpdateProfilePayload): Promise<ProfileResponse> {
  const body = {
    firstName: payload.firstName.trim(),
    lastName: payload.lastName.trim(),
    birth: payload.birth,
    gender: payload.gender,
    countryCode: payload.countryCode,
    city: payload.city,
  }

  const response = await api.put<ProfileResponse>('/auth/profile', body)
  return response.data
}

export async function requestPasswordReset(form: ForgotPasswordFormState): Promise<ForgotPasswordResponse> {
  const payload = {
    email: form.email.trim().toLowerCase(),
  }

  const response = await api.post<ForgotPasswordResponse>('/auth/forgot-password', payload)
  return response.data
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<ResetPasswordResponse> {
  const body = {
    token: payload.token.trim(),
    newPassword: payload.newPassword,
  }

  const response = await api.post<ResetPasswordResponse>('/auth/reset-password', body)
  return response.data
}

function extractApiErrorMessage(error: unknown, fallbackMessage: string): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } }).response
    return response?.data?.message ?? response?.data?.error ?? fallbackMessage
  }

  return fallbackMessage
}

export function getLoginErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'Unexpected error during log in. Please try again.')
}

export function getRegisterErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'Unexpected error during sign up. Please try again.')
}

export function getLogoutErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'Unexpected error during sign out. Please try again.')
}

export function getForgotPasswordErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'Unexpected error requesting a password reset. Please try again.')
}

export function getResetPasswordErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'Unexpected error resetting your password. Please try again.')
}

export function getProfileErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'Unexpected error loading your profile. Please try again.')
}

export function getUpdateProfileErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'Unexpected error updating your profile. Please try again.')
}
