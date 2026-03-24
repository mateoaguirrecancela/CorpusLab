import { api } from '@/lib/api'
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

export function getRegisterErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } }).response
    return response?.data?.message ?? response?.data?.error ?? 'The sign up request could not be completed.'
  }

  return 'Unexpected error during sign up. Please try again.'
}
