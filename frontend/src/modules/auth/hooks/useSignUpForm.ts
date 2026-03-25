import { type FormEvent, useMemo, useState } from 'react'
import { INITIAL_REGISTER_STATE } from '@/modules/auth/constants/signup'
import { getRegisterErrorMessage, signup } from '@/modules/auth/services/userService'
import { type RegisterFormState } from '@/modules/auth/types/signup'

export function useSignUpForm() {
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
      const response = await signup(form)
      setSuccessMessage(`Account created for ${response.firstName}. You can now sign in.`)
      setForm(INITIAL_REGISTER_STATE)
    } catch (error) {
      setErrorMessage(getRegisterErrorMessage(error))
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

