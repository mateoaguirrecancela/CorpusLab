import { type FormEventHandler, useMemo } from 'react';
import { CalendarDays, Github, Globe, Lock, Mail, MapPin, User } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import { useToastMessages } from '@/hooks/useToastMessages';
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner';
import { getCountryOptions, getGenderOptions } from '@/modules/auth/constants/signup';
import { CountryCombobox } from '@/modules/auth/components/CountryCombobox';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import { type RegisterFormState } from '@/modules/auth/types/signup';
import { isAtLeast16YearsOld, isEmailValid } from '@/modules/auth/utils/validation';

type SignUpFormProps = {
  form: RegisterFormState;
  canSubmit: boolean;
  isSubmitting: boolean;
  errorMessage: string;
  successMessage: string;
  onSubmit: FormEventHandler<HTMLFormElement>;
  onFieldChange: <K extends keyof RegisterFormState>(field: K, value: RegisterFormState[K]) => void;
  onOAuthClick: (provider: OAuthProvider) => void;
};

export function SignUpForm({
  form,
  canSubmit,
  isSubmitting,
  errorMessage,
  successMessage,
  onSubmit,
  onFieldChange,
  onOAuthClick,
}: Readonly<SignUpFormProps>) {
  const { t, i18n } = useTranslation();
  const trimmedEmail = form.email.trim();
  const isEmailInvalid = trimmedEmail.length > 0 && !isEmailValid(trimmedEmail);
  const isUnderage = form.birth.length > 0 && !isAtLeast16YearsOld(form.birth);
  const genderOptions = getGenderOptions(t);
  const countryOptions = useMemo(
    () => getCountryOptions(i18n.resolvedLanguage ?? i18n.language ?? 'en'),
    [i18n.language, i18n.resolvedLanguage],
  );

  useToastMessages({ errorMessage, successMessage });

  return (
    <form className="mt-8 space-y-3" onSubmit={onSubmit}>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormFieldControl
          icon={<User className="size-4" />}
          id="firstName"
          inputProps={{ name: 'firstName', placeholder: 'David' }}
          label={t('auth.signup.firstName')}
          onValueChange={(value) => onFieldChange('firstName', value)}
          required
          value={form.firstName}
        />
        <FormFieldControl
          icon={<User aria-hidden className="size-4" />}
          id="lastName"
          inputProps={{ name: 'lastName', placeholder: 'García Fernández' }}
          label={t('auth.signup.lastName')}
          onValueChange={(value) => onFieldChange('lastName', value)}
          required
          value={form.lastName}
        />
      </div>

      <FormFieldControl
        icon={<Mail className="size-4" />}
        id="email"
        inputProps={{ placeholder: 'example@email.com' }}
        inputType="email"
        label={t('auth.signup.email')}
        message={isEmailInvalid ? t('auth.signup.invalidEmail') : undefined}
        onValueChange={(value) => onFieldChange('email', value)}
        required
        value={form.email}
      />

      <FormFieldControl
        hidePasswordLabel={t('common.aria.hidePassword')}
        icon={<Lock className="size-4" />}
        id="password"
        inputProps={{ minLength: 8, placeholder: '********' }}
        inputType="password"
        label={t('auth.signup.password')}
        message={
          form.password.length > 0 && form.password.length < 8
            ? t('auth.signup.passwordLength')
            : undefined
        }
        onValueChange={(value) => onFieldChange('password', value)}
        required
        showPasswordLabel={t('common.aria.showPassword')}
        value={form.password}
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <FormFieldControl
          icon={<CalendarDays className="size-4" />}
          id="birth"
          inputType="date"
          label={t('auth.signup.dateOfBirth')}
          message={isUnderage ? t('auth.signup.underAge') : undefined}
          onValueChange={(value) => onFieldChange('birth', value)}
          required
          value={form.birth}
        />
        <FormFieldControl
          controlType="select"
          icon={<User className="size-4" />}
          id="gender"
          label={t('auth.signup.gender')}
          onValueChange={(value) => onFieldChange('gender', value as RegisterFormState['gender'])}
          options={genderOptions}
          required
          value={form.gender}
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <FormFieldControl
          controlType="custom"
          icon={<Globe className="size-4" />}
          id="countryCode"
          label={t('auth.signup.country')}
          renderControl={({ id }) => (
            <CountryCombobox
              id={id}
              options={countryOptions}
              placeholder={t('auth.signup.searchCountry')}
              value={form.countryCode}
              onChange={(value) => onFieldChange('countryCode', value)}
            />
          )}
          required
        />

        <FormFieldControl
          icon={<MapPin className="size-4" />}
          id="city"
          inputProps={{ placeholder: 'Madrid' }}
          label={t('auth.signup.city')}
          onValueChange={(value) => onFieldChange('city', value)}
          required
          value={form.city}
        />
      </div>

      <SubmitButtonWithSpinner
        className="h-11 w-full rounded-md bg-primary text-sm font-semibold text-white shadow-[var(--shadow-primary-action)] hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
        disabled={!canSubmit}
        idleLabel={t('auth.signup.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.signup.submitting')}
      />

      <div className="my-8 flex items-center gap-3 text-[0.67rem] font-bold tracking-[0.12em] text-muted-foreground uppercase">
        <span className="h-px flex-1 bg-border" />
        <span>{t('auth.signup.orContinueWith')}</span>
        <span className="h-px flex-1 bg-border" />
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <Button
          className="h-10 rounded-md border border-border bg-surface-base text-sm font-semibold text-foreground hover:bg-accent cursor-pointer"
          disabled={isSubmitting}
          onClick={() => onOAuthClick('google')}
          type="button"
          variant="outline"
        >
          <span className="text-base text-red-500">G</span>
          <span>Google</span>
        </Button>
        <Button
          className="h-10 rounded-md border border-border bg-surface-base text-sm font-semibold text-foreground hover:bg-accent cursor-pointer"
          disabled={isSubmitting}
          onClick={() => onOAuthClick('github')}
          type="button"
          variant="outline"
        >
          <Github className="size-4" />
          <span>GitHub</span>
        </Button>
      </div>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        {t('auth.signup.alreadyInLab')}{' '}
        <Link className="font-semibold text-primary hover:underline" to="/auth/login">
          {t('auth.signup.goToLogin')}
        </Link>
      </p>
    </form>
  );
}
