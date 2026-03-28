import { type FormEvent, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { INITIAL_LOGIN_STATE } from '@/modules/auth/constants/login'
import { type OAuthProvider, SESSION_AUTH_TOKEN_STORAGE_KEY, SESSION_USER_STORAGE_KEY } from '@/modules/auth/constants/session'
import { getLoginErrorMessage, getLogoutErrorMessage, login, logout, redirectToOAuthAuthorization } from '@/modules/auth/services/authService'
import { type LoginFormState } from '@/modules/auth/types/login'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function useSignInForm() {
  const navigate = useNavigate()
  const [form, setForm] = useState<LoginFormState>(INITIAL_LOGIN_STATE)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const canSubmit = useMemo(
    () => EMAIL_REGEX.test(form.email.trim()) && form.password.length >= 8 && !isSubmitting,
    [form, isSubmitting],
  )

  const updateField = <K extends keyof LoginFormState>(field: K, value: LoginFormState[K]) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!canSubmit) {
      setErrorMessage('Please complete email and password before continuing.')
      setSuccessMessage('')
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const response = await login(form)
      localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY)
      localStorage.setItem(SESSION_USER_STORAGE_KEY, JSON.stringify({
        firstName: response.firstName,
        lastName: response.lastName,
        email: response.email,
      }))
      setIsLoggedIn(true)
      setSuccessMessage(`Welcome back, ${response.firstName}. Session started successfully.`)
      setForm((current) => ({ ...current, password: '' }))
      navigate('/home')
    } catch (error) {
      setErrorMessage(getLoginErrorMessage(error))
      setIsLoggedIn(false)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleLogout = async () => {
    setIsSubmitting(true)
    setErrorMessage('')

    try {
      const response = await logout()
      localStorage.removeItem(SESSION_USER_STORAGE_KEY)
      localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY)
      setIsLoggedIn(false)
      setSuccessMessage(response.message)
    } catch (error) {
      setErrorMessage(getLogoutErrorMessage(error))
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
    isLoggedIn,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
    handleLogout,
    handleOAuthClick,
  }
}
