import { type FormEvent, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { INITIAL_REGISTER_STATE } from '@/modules/auth/constants/signup'
import { type OAuthProvider, SESSION_AUTH_TOKEN_STORAGE_KEY, SESSION_USER_STORAGE_KEY } from '@/modules/auth/constants/session'
import {
  getLoginErrorMessage,
  getRegisterErrorMessage,
  login,
  redirectToOAuthAuthorization,
  signup,
} from '@/modules/auth/services/authService'
import { type RegisterFormState } from '@/modules/auth/types/signup'

export function useSignUpForm() {
  const navigate = useNavigate()
  const [form, setForm] = useState<RegisterFormState>(INITIAL_REGISTER_STATE)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const canSubmit = useMemo(
    () =>
      form.firstName.trim().length > 0
      && form.lastName.trim().length > 0
      && form.email.trim().length > 0
      && form.password.length >= 8
      && form.birth.length > 0
      && form.countryCode.length === 2
      && form.city.trim().length > 0
      && !isSubmitting,
    [form, isSubmitting],
  )

  const updateField = <K extends keyof RegisterFormState>(field: K, value: RegisterFormState[K]) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!canSubmit) {
      setErrorMessage('Please complete all required fields before continuing.')
      setSuccessMessage('')
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      await signup(form)
      const loginResponse = await login({
        email: form.email,
        password: form.password,
      })

      localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY)
      localStorage.setItem(SESSION_USER_STORAGE_KEY, JSON.stringify({
        firstName: loginResponse.firstName,
        lastName: loginResponse.lastName,
        email: loginResponse.email,
      }))
      setSuccessMessage(`Account created for ${loginResponse.firstName}.`)
      setForm(INITIAL_REGISTER_STATE)
      navigate('/home')
    } catch (error) {
      const registerErrorMessage = getRegisterErrorMessage(error)
      const loginErrorMessage = getLoginErrorMessage(error)
      const isRegisterError = registerErrorMessage !== 'Unexpected error during sign up. Please try again.'
      setErrorMessage(isRegisterError ? registerErrorMessage : loginErrorMessage)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleOAuthClick = (provider: OAuthProvider) => {
    if (isSubmitting) {
      return
    }

    redirectToOAuthAuthorization(provider)
  }

  return {
    form,
    canSubmit,
    isSubmitting,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
    handleOAuthClick,
  }
}

