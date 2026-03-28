import { type FormEvent } from 'react'
import { Mail } from 'lucide-react'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { FeedbackMessage } from '@/components/ui/feedback-message'
import { Spinner } from '@/components/ui/spinner'
import { AuthFormField } from '@/modules/auth/components/AuthFormField'
import { type ForgotPasswordFormState } from '@/modules/auth/types/forgotPassword'

type ForgotPasswordFormProps = {
  form: ForgotPasswordFormState
  canSubmit: boolean
  isSubmitting: boolean
  errorMessage: string
  successMessage: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onFieldChange: <K extends keyof ForgotPasswordFormState>(field: K, value: ForgotPasswordFormState[K]) => void
}

export function ForgotPasswordForm({
  form,
  canSubmit,
  isSubmitting,
  errorMessage,
  successMessage,
  onSubmit,
  onFieldChange,
}: ForgotPasswordFormProps) {
  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <AuthFormField
        icon={<Mail className="size-3.5" />}
        id="email"
        label="Email"
        placeholder="example@email.com"
        type="email"
        value={form.email}
        onChange={(value) => onFieldChange('email', value)}
      />

      <Button
        className="h-11 w-full rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white shadow-[0_8px_16px_-10px_rgba(49,46,129,0.95)] hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
        disabled={!canSubmit}
        type="submit"
      >
        {isSubmitting ? (
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            Sending link...
          </span>
        ) : (
          'Send reset link'
        )}
      </Button>

      <FeedbackMessage message={errorMessage} variant="error" />
      <FeedbackMessage message={successMessage} variant="success" />

      <p className="mt-8 text-center text-sm text-[color:var(--cl-secondary)]">
        Remembered your password?{' '}
        <Link className="font-semibold text-[color:var(--cl-primary)] hover:underline" to="/auth/login">
          Back to log in
        </Link>
      </p>
    </form>
  )
}