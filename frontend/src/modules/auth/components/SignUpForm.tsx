import { type FormEventHandler, useMemo } from 'react';
import { CalendarDays, Globe, Lock, Mail, MapPin, User } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { useToastMessages } from '@/hooks/useToastMessages';
import { getResolvedLanguage } from '@/lib/i18n';
import {
  AuthDivider,
  AuthLinkPrompt,
  AuthSubmitButton,
} from '@/modules/auth/components/AuthFormActions';
import { getCountryOptions, getGenderOptions } from '@/modules/auth/constants/signup';
import { OAuthButtons } from '@/modules/auth/components/OAuthButtons';
import { CountryCombobox } from '@/modules/auth/components/CountryCombobox';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import { type RegisterFormState } from '@/modules/auth/types/signup';

type SignUpFormProps = {
  form: RegisterFormState;
  canSubmit: boolean;
  fieldErrors?: Partial<Record<keyof RegisterFormState, string>>;
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
  fieldErrors = {},
  isSubmitting,
  errorMessage,
  successMessage,
  onSubmit,
  onFieldChange,
  onOAuthClick,
}: Readonly<SignUpFormProps>) {
  const { t, i18n } = useTranslation();
  const language = getResolvedLanguage(i18n);
  const genderOptions = getGenderOptions(t);
  const countryOptions = useMemo(() => getCountryOptions(language), [language]);

  useToastMessages({ errorMessage, successMessage });

  return (
    <form className="mt-8 space-y-3" onSubmit={onSubmit}>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormFieldControl
          icon={<User className="size-4" />}
          id="firstName"
          inputProps={{ name: 'firstName', placeholder: 'David' }}
          label={t('auth.signup.firstName')}
          message={fieldErrors.firstName}
          onValueChange={(value) => onFieldChange('firstName', value)}
          required
          value={form.firstName}
        />
        <FormFieldControl
          icon={<User aria-hidden className="size-4" />}
          id="lastName"
          inputProps={{ name: 'lastName', placeholder: 'García Fernández' }}
          label={t('auth.signup.lastName')}
          message={fieldErrors.lastName}
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
        message={fieldErrors.email}
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
        message={fieldErrors.password}
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
          message={fieldErrors.birth}
          onValueChange={(value) => onFieldChange('birth', value)}
          required
          value={form.birth}
        />
        <FormFieldControl
          controlType="select"
          icon={<User className="size-4" />}
          id="gender"
          label={t('auth.signup.gender')}
          message={fieldErrors.gender}
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
          message={fieldErrors.countryCode}
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
          message={fieldErrors.city}
          onValueChange={(value) => onFieldChange('city', value)}
          required
          value={form.city}
        />
      </div>

      <AuthSubmitButton
        canSubmit={canSubmit}
        idleLabel={t('auth.signup.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.signup.submitting')}
      />

      <AuthDivider label={t('auth.signup.orContinueWith')} />

      <OAuthButtons disabled={isSubmitting} onProviderClick={onOAuthClick} />

      <AuthLinkPrompt
        linkLabel={t('auth.signup.goToLogin')}
        message={t('auth.signup.alreadyInLab')}
        to="/auth/login"
      />
    </form>
  );
}
