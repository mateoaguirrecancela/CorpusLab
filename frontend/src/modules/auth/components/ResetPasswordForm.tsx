import { type FormEvent } from 'react'
import { Lock } from 'lucide-react'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { AuthFormField } from '@/modules/auth/components/AuthFormField'
import { type ResetPasswordFormState } from '@/modules/auth/types/resetPassword'

type ResetPasswordFormProps = {
  form: ResetPasswordFormState
  canSubmit: boolean
  isSubmitting: boolean
  errorMessage: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onFieldChange: <K extends keyof ResetPasswordFormState>(field: K, value: ResetPasswordFormState[K]) => void
}

export function ResetPasswordForm({
  form,
  canSubmit,
  isSubmitting,
  errorMessage,
  onSubmit,
  onFieldChange,
}: ResetPasswordFormProps) {
  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <AuthFormField
        icon={<Lock className="size-3.5" />}
        id="newPassword"
        label="New Password"
        minLength={8}
        placeholder="********"
        type="password"
        value={form.newPassword}
        onChange={(value) => onFieldChange('newPassword', value)}
      />

      <AuthFormField
        icon={<Lock className="size-3.5" />}
        id="confirmPassword"
        label="Confirm Password"
        minLength={8}
        placeholder="********"
        type="password"
        value={form.confirmPassword}
        onChange={(value) => onFieldChange('confirmPassword', value)}
      />

      <Button
        className="h-11 w-full rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white shadow-[0_8px_16px_-10px_rgba(49,46,129,0.95)] hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
        disabled={!canSubmit}
        type="submit"
      >
        {isSubmitting ? 'Updating password...' : 'Reset password'}
      </Button>

      {errorMessage.length > 0 && (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{errorMessage}</p>
      )}

      <p className="mt-8 text-center text-sm text-[color:var(--cl-secondary)]">
        Password already updated?{' '}
        <Link className="font-semibold text-[color:var(--cl-primary)] hover:underline" to="/auth/login">
          Go to log in
        </Link>
      </p>
    </form>
  )
}