import { type FormEvent } from 'react'
import { CalendarDays, Github, Globe, Lock, Mail, MapPin, User } from 'lucide-react'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { FeedbackMessage } from '@/components/ui/feedback-message'
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner'
import { COUNTRY_OPTIONS, GENDER_OPTIONS } from '@/modules/auth/constants/signup'
import { AuthCombobox } from '@/modules/auth/components/CountryCombobox'
import { AuthFormField } from '@/modules/auth/components/AuthFormField'
import { AuthSelectField } from '@/modules/auth/components/AuthSelectField'
import { type OAuthProvider } from '@/modules/auth/constants/session'
import { type RegisterFormState } from '@/modules/auth/types/signup'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function isAtLeast16YearsOld(birthDate: string): boolean {
  if (!birthDate) {
    return false
  }

  const [yearText, monthText, dayText] = birthDate.split('-')
  const year = Number(yearText)
  const month = Number(monthText)
  const day = Number(dayText)

  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return false
  }

  const today = new Date()
  let age = today.getFullYear() - year
  const currentMonth = today.getMonth() + 1
  const currentDay = today.getDate()

  if (currentMonth < month || (currentMonth === month && currentDay < day)) {
    age -= 1
  }

  return age >= 16
}

type SignUpFormProps = {
  form: RegisterFormState
  canSubmit: boolean
  isSubmitting: boolean
  errorMessage: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onFieldChange: <K extends keyof RegisterFormState>(field: K, value: RegisterFormState[K]) => void
  onOAuthClick: (provider: OAuthProvider) => void
}

export function SignUpForm({
  form,
  canSubmit,
  isSubmitting,
  errorMessage,
  onSubmit,
  onFieldChange,
  onOAuthClick,
}: SignUpFormProps) {
  const trimmedEmail = form.email.trim()
  const isEmailInvalid = trimmedEmail.length > 0 && !EMAIL_REGEX.test(trimmedEmail)
  const isUnderage = form.birth.length > 0 && !isAtLeast16YearsOld(form.birth)

  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <div className="grid gap-4 sm:grid-cols-2">
        <AuthFormField
          icon={<User className="size-3.5" />}
          id="firstName"
          label="First Name *"
          placeholder="David"
          value={form.firstName}
          onChange={(value) => onFieldChange('firstName', value)}
        />
        <AuthFormField
          icon={<User aria-hidden className="size-3.5" />}
          id="lastName"
          label="Last Name *"
          placeholder="García Fernández"
          value={form.lastName}
          onChange={(value) => onFieldChange('lastName', value)}
        />
      </div>

      <AuthFormField
        icon={<Mail className="size-3.5" />}
        id="email"
        label="Email *"
        placeholder="example@email.com"
        type="email"
        value={form.email}
        onChange={(value) => onFieldChange('email', value)}
      />

      {isEmailInvalid && (
        <p className="-mt-2 text-xs text-red-700">Enter a valid email address.</p>
      )}

      <AuthFormField
        icon={<Lock className="size-3.5" />}
        id="password"
        label="Password *"
        placeholder="********"
        minLength={8}
        type="password"
        value={form.password}
        onChange={(value) => onFieldChange('password', value)}
      />

      {form.password.length > 0 && form.password.length < 8 && (
        <p className="-mt-2 text-xs text-red-700">Password must contain at least 8 characters.</p>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        <AuthFormField
          icon={<CalendarDays className="size-3.5" />}
          id="birth"
          label="Date Of Birth *"
          type="date"
          value={form.birth}
          onChange={(value) => onFieldChange('birth', value)}
        />
        <AuthSelectField
          icon={<User className="size-3.5" />}
          id="gender"
          label="Gender *"
          options={GENDER_OPTIONS}
          value={form.gender}
          onChange={(value) => onFieldChange('gender', value as RegisterFormState['gender'])}
        />
      </div>

      {isUnderage && (
        <p className="-mt-2 text-xs text-red-700">You must be at least 16 years old.</p>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        <AuthCombobox
          icon={<Globe className="size-3.5" />}
          id="countryCode"
          label="Country *"
          options={COUNTRY_OPTIONS}
          placeholder="Search country"
          value={form.countryCode}
          onChange={(value) => onFieldChange('countryCode', value)}
        />
        <AuthFormField
          icon={<MapPin className="size-3.5" />}
          id="city"
          label="City *"
          placeholder="Madrid"
          value={form.city}
          onChange={(value) => onFieldChange('city', value)}
        />
      </div>

      <SubmitButtonWithSpinner
        className="h-11 w-full rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white shadow-[0_8px_16px_-10px_rgba(49,46,129,0.95)] hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
        disabled={!canSubmit}
        idleLabel="Sign Up"
        isSubmitting={isSubmitting}
        submittingLabel="Creating account..."
      />

      <FeedbackMessage message={errorMessage} variant="error" />

      <div className="my-8 flex items-center gap-3 text-[0.67rem] font-bold tracking-[0.12em] text-[color:var(--cl-tertiary)] uppercase">
        <span className="h-px flex-1 bg-[color:var(--cl-line)]" />
        <span>Or continue with</span>
        <span className="h-px flex-1 bg-[color:var(--cl-line)]" />
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <Button
          className="h-10 rounded-md border border-[color:var(--cl-line)] bg-white text-sm font-semibold text-[color:var(--cl-neutral)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
          disabled={isSubmitting}
          onClick={() => onOAuthClick('google')}
          type="button"
          variant="outline"
        >
          <span className="text-base text-red-500">G</span>
          Google
        </Button>
        <Button
          className="h-10 rounded-md border border-[color:var(--cl-line)] bg-white text-sm font-semibold text-[color:var(--cl-neutral)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
          disabled={isSubmitting}
          onClick={() => onOAuthClick('github')}
          type="button"
          variant="outline"
        >
          <Github className="size-4" />
          GitHub
        </Button>
      </div>

      <p className="mt-8 text-center text-sm text-[color:var(--cl-secondary)]">
        Already part of the laboratory?{' '}
        <Link className="font-semibold text-[color:var(--cl-primary)] hover:underline" to="/auth/login">
          Log in
        </Link>
      </p>
    </form>
  )
}
