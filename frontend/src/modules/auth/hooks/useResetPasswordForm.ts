import { type FormEvent, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { buildInitialResetPasswordState } from '@/modules/auth/constants/resetPassword'
import { getResetPasswordErrorMessage, resetPassword } from '@/modules/auth/services/authService'
import { type ResetPasswordFormState } from '@/modules/auth/types/resetPassword'

export function useResetPasswordForm(initialToken: string) {
  const navigate = useNavigate()
  const [form, setForm] = useState<ResetPasswordFormState>(() => buildInitialResetPasswordState(initialToken))
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState(initialToken.trim().length === 0
    ? 'Missing reset token in URL. Open the link sent to your email.'
    : '')
  const [successMessage, setSuccessMessage] = useState('')

  const canSubmit = useMemo(
    () =>
      form.token.trim().length >= 16
      && form.newPassword.length >= 8
      && form.confirmPassword === form.newPassword
      && !isSubmitting,
    [form, isSubmitting],
  )

  const updateField = <K extends keyof ResetPasswordFormState>(
    field: K,
    value: ResetPasswordFormState[K],
  ) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (form.token.trim().length === 0) {
      setErrorMessage('Missing reset token in URL. Open the link sent to your email.')
      setSuccessMessage('')
      return
    }

    if (form.confirmPassword !== form.newPassword) {
      setErrorMessage('Password confirmation does not match.')
      setSuccessMessage('')
      return
    }

    if (!canSubmit) {
      setErrorMessage('Please complete all required fields before continuing.')
      setSuccessMessage('')
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      await resetPassword({
        token: form.token,
        newPassword: form.newPassword,
      })
      setForm((current) => ({ ...current, newPassword: '', confirmPassword: '' }))
      navigate('/auth/login', { replace: true })
    } catch (error) {
      setErrorMessage(getResetPasswordErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return {
    form,
    canSubmit,
    isSubmitting,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
  }
}