import { type FormEvent } from 'react'
import { Github, Lock, Mail } from 'lucide-react'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { AuthFormField } from '@/modules/auth/components/AuthFormField'
import { type LoginFormState } from '@/modules/auth/types/login'

type LogInFormProps = {
  form: LoginFormState
  canSubmit: boolean
  isSubmitting: boolean
  errorMessage: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onFieldChange: <K extends keyof LoginFormState>(field: K, value: LoginFormState[K]) => void
}

export function LogInForm({
  form,
  canSubmit,
  isSubmitting,
  errorMessage,
  onSubmit,
  onFieldChange,
}: LogInFormProps) {
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

      <div className="space-y-1.5">
        <AuthFormField
          icon={<Lock className="size-3.5" />}
          id="password"
          label="Password"
          placeholder="********"
          minLength={8}
          type="password"
          value={form.password}
          onChange={(value) => onFieldChange('password', value)}
        />

        <div className="flex items-end justify-end">
          <Link className="text-xs font-semibold text-[color:var(--cl-tertiary)] hover:text-[color:var(--cl-primary)]" to="/auth/forgot-password">
            Forgot password?
          </Link>
        </div>
      </div>

      <Button
        className="h-11 w-full rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white shadow-[0_8px_16px_-10px_rgba(49,46,129,0.95)] hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
        disabled={!canSubmit}
        type="submit"
      >
        {isSubmitting ? 'Logging in...' : 'Log In'}
      </Button>

      {errorMessage.length > 0 && (
        <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{errorMessage}</p>
      )}

      <div className="my-7 flex items-center gap-3 text-[0.67rem] font-bold tracking-[0.12em] text-[color:var(--cl-tertiary)] uppercase">
        <span className="h-px flex-1 bg-[color:var(--cl-line)]" />
        <span>Or continue with</span>
        <span className="h-px flex-1 bg-[color:var(--cl-line)]" />
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <Button
          className="h-10 rounded-md border border-[color:var(--cl-line)] bg-white text-sm font-semibold text-[color:var(--cl-neutral)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
          type="button"
          variant="outline"
        >
          <span className="text-base text-red-500">G</span>
          Google
        </Button>
        <Button
          className="h-10 rounded-md border border-[color:var(--cl-line)] bg-white text-sm font-semibold text-[color:var(--cl-neutral)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
          type="button"
          variant="outline"
        >
          <Github className="size-4" />
          GitHub
        </Button>
      </div>

      <p className="mt-8 text-center text-sm text-[color:var(--cl-secondary)]">
        New to the lab?{' '}
        <Link className="font-semibold text-[color:var(--cl-primary)] hover:underline" to="/auth/signup">
          Create an account
        </Link>
      </p>
    </form>
  )
}
