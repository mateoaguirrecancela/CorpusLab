import { type FormEvent, useMemo, useState } from 'react'
import { INITIAL_FORGOT_PASSWORD_STATE } from '@/modules/auth/constants/forgotPassword'
import { getForgotPasswordErrorMessage, requestPasswordReset } from '@/modules/auth/services/authService'
import { type ForgotPasswordFormState } from '@/modules/auth/types/forgotPassword'

export function useForgotPasswordForm() {
  const [form, setForm] = useState<ForgotPasswordFormState>(INITIAL_FORGOT_PASSWORD_STATE)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const canSubmit = useMemo(
    () => form.email.trim().length > 0 && !isSubmitting,
    [form.email, isSubmitting],
  )

  const updateField = <K extends keyof ForgotPasswordFormState>(
    field: K,
    value: ForgotPasswordFormState[K],
  ) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!canSubmit) {
      setErrorMessage('Please enter a valid email before continuing.')
      setSuccessMessage('')
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const response = await requestPasswordReset(form)
      setSuccessMessage(response.message)
    } catch (error) {
      setErrorMessage(getForgotPasswordErrorMessage(error))
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