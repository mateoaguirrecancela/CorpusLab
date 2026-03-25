import { api } from '@/lib/api'
import { type LoginFormState, type LoginResponse, type LogoutResponse } from '@/modules/auth/types/login'
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

export function getLoginErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } }).response
    return response?.data?.message ?? response?.data?.error ?? 'The log in request could not be completed.'
  }

  return 'Unexpected error during log in. Please try again.'
}

export function getRegisterErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } }).response
    return response?.data?.message ?? response?.data?.error ?? 'The sign up request could not be completed.'
  }

  return 'Unexpected error during sign up. Please try again.'
}

export function getLogoutErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } }).response
    return response?.data?.message ?? response?.data?.error ?? 'Could not close the current session.'
  }

  return 'Unexpected error during sign out. Please try again.'
}
