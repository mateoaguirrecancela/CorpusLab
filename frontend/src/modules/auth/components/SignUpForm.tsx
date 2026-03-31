import { type FormEvent, useMemo, useState } from 'react';
import { CalendarDays, ChevronDown, Eye, EyeOff, Github, Globe, Lock, Mail, MapPin, User } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import { Field, FieldLabel } from '@/components/ui/field';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import { Input } from '@/components/ui/input';
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner';
import { getCountryOptions, getGenderOptions } from '@/modules/auth/constants/signup';
import { CountryCombobox } from '@/modules/auth/components/CountryCombobox';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import { type RegisterFormState } from '@/modules/auth/types/signup';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function isAtLeast16YearsOld(birthDate: string): boolean {
  if (!birthDate) {
    return false;
  }

  const [yearText, monthText, dayText] = birthDate.split('-');
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);

  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return false;
  }

  const today = new Date();
  let age = today.getFullYear() - year;
  const currentMonth = today.getMonth() + 1;
  const currentDay = today.getDate();

  if (currentMonth < month || (currentMonth === month && currentDay < day)) {
    age -= 1;
  }

  return age >= 16;
}

type SignUpFormProps = {
  form: RegisterFormState;
  canSubmit: boolean;
  isSubmitting: boolean;
  errorMessage: string;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onFieldChange: <K extends keyof RegisterFormState>(field: K, value: RegisterFormState[K]) => void;
  onOAuthClick: (provider: OAuthProvider) => void;
};

export function SignUpForm({
  form,
  canSubmit,
  isSubmitting,
  errorMessage,
  onSubmit,
  onFieldChange,
  onOAuthClick,
}: SignUpFormProps) {
  const { t, i18n } = useTranslation();
  const trimmedEmail = form.email.trim();
  const isEmailInvalid = trimmedEmail.length > 0 && !EMAIL_REGEX.test(trimmedEmail);
  const isUnderage = form.birth.length > 0 && !isAtLeast16YearsOld(form.birth);
  const genderOptions = getGenderOptions(t);
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const countryOptions = useMemo(
    () => getCountryOptions(i18n.resolvedLanguage ?? i18n.language ?? 'en'),
    [i18n.language, i18n.resolvedLanguage],
  );

  return (
    <form className="mt-8 space-y-3" onSubmit={onSubmit}>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field>
          <FieldLabel htmlFor="firstName">
            <User className="size-4" />
            {`${t('auth.signup.firstName')} *`}
          </FieldLabel>
          <Input
            id="firstName"
            name="firstName"
            onChange={(e) => onFieldChange('firstName', e.currentTarget.value)}
            placeholder="David"
            value={form.firstName}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="lastName">
            <User aria-hidden className="size-4" />
            {`${t('auth.signup.lastName')} *`}
          </FieldLabel>
          <Input
            id="lastName"
            name="lastName"
            onChange={(e) => onFieldChange('lastName', e.currentTarget.value)}
            placeholder="García Fernández"
            value={form.lastName}
          />
        </Field>
      </div>

      <Field>
        <FieldLabel htmlFor="email">
          <Mail className="size-4" />
          {`${t('auth.signup.email')} *`}
        </FieldLabel>
        <Input
          id="email"
          onChange={(e) => onFieldChange('email', e.currentTarget.value)}
          placeholder="example@email.com"
          type="email"
          value={form.email}
        />
      </Field>

      {isEmailInvalid && (
        <p className="-mt-2 text-xs text-red-700">{t('auth.signup.invalidEmail')}</p>
      )}

      <Field>
        <FieldLabel htmlFor="password">
          <Lock className="size-4" />
          {`${t('auth.signup.password')} *`}
        </FieldLabel>
        <div className="relative">
          <Input
            id="password"
            minLength={8}
            onChange={(e) => onFieldChange('password', e.currentTarget.value)}
            placeholder="********"
            type={isPasswordVisible ? 'text' : 'password'}
            value={form.password}
          />
          <button
            aria-label={
              isPasswordVisible ? t('common.aria.hidePassword') : t('common.aria.showPassword')
            }
            className="absolute top-1/2 right-3 -translate-y-1/2 text-[color:var(--cl-tertiary)] transition hover:text-[color:var(--cl-primary)]"
            onClick={() => setIsPasswordVisible((current) => !current)}
            type="button"
          >
            {isPasswordVisible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
      </Field>

      {form.password.length > 0 && form.password.length < 8 && (
        <p className="-mt-2 text-xs text-red-700">{t('auth.signup.passwordLength')}</p>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        <Field>
          <FieldLabel htmlFor="birth">
            <CalendarDays className="size-4" />
            {`${t('auth.signup.dateOfBirth')} *`}
          </FieldLabel>
          <Input
            id="birth"
            onChange={(e) => onFieldChange('birth', e.currentTarget.value)}
            type="date"
            value={form.birth}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="gender">
            <User className="size-4" />
            {`${t('auth.signup.gender')} *`}
          </FieldLabel>
          <div className="relative">
            <select
              id="gender"
              className="h-10 w-full appearance-none rounded-md border border-input bg-white px-3 pr-10 text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50"
              onChange={(e) => onFieldChange('gender', e.currentTarget.value as RegisterFormState['gender'])}
              value={form.gender}
            >
              {genderOptions.map((option) => (
                <option key={option.label} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <ChevronDown className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-[color:var(--cl-tertiary)]" />
          </div>
        </Field>
      </div>

      {isUnderage && <p className="-mt-2 text-xs text-red-700">{t('auth.signup.underAge')}</p>}

      <div className="grid gap-4 sm:grid-cols-2">
        <Field>
          <FieldLabel htmlFor="countryCode">
            <Globe className="size-4" />
            {`${t('auth.signup.country')} *`}
          </FieldLabel>
          <CountryCombobox
            id="countryCode"
            options={countryOptions}
            placeholder={t('auth.signup.searchCountry')}
            value={form.countryCode}
            onChange={(value) => onFieldChange('countryCode', value)}
          />
        </Field>

        <Field>
          <FieldLabel htmlFor="city">
            <MapPin className="size-4" />
            {`${t('auth.signup.city')} *`}
          </FieldLabel>
          <Input
            id="city"
            onChange={(e) => onFieldChange('city', e.currentTarget.value)}
            placeholder="Madrid"
            value={form.city}
          />
        </Field>
      </div>

      <SubmitButtonWithSpinner
        className="h-11 w-full rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white shadow-[0_8px_16px_-10px_rgba(49,46,129,0.95)] hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
        disabled={!canSubmit}
        idleLabel={t('auth.signup.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.signup.submitting')}
      />

      <FeedbackMessage message={errorMessage} variant="error" />

      <div className="my-8 flex items-center gap-3 text-[0.67rem] font-bold tracking-[0.12em] text-[color:var(--cl-tertiary)] uppercase">
        <span className="h-px flex-1 bg-[color:var(--cl-line)]" />
        <span>{t('auth.signup.orContinueWith')}</span>
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
        {t('auth.signup.alreadyInLab')}{' '}
        <Link
          className="font-semibold text-[color:var(--cl-primary)] hover:underline"
          to="/auth/login"
        >
          {t('auth.signup.goToLogin')}
        </Link>
      </p>
    </form>
  );
}

