import { type FormEventHandler } from 'react';
import { Lock, Mail } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { useToastMessages } from '@/hooks/useToastMessages';
import {
  AuthDivider,
  AuthLinkPrompt,
  AuthSubmitButton,
} from '@/modules/auth/components/AuthFormActions';
import { OAuthButtons } from '@/modules/auth/components/OAuthButtons';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import { type LoginFormState } from '@/modules/auth/types/login';

type LogInFormProps = {
  form: LoginFormState;
  canSubmit: boolean;
  fieldErrors?: Partial<Record<keyof LoginFormState, string>>;
  isSubmitting: boolean;
  errorMessage: string;
  successMessage: string;
  onSubmit: FormEventHandler<HTMLFormElement>;
  onFieldChange: <K extends keyof LoginFormState>(field: K, value: LoginFormState[K]) => void;
  onOAuthClick: (provider: OAuthProvider) => void;
};

export function LogInForm({
  form,
  canSubmit,
  fieldErrors = {},
  isSubmitting,
  errorMessage,
  successMessage,
  onSubmit,
  onFieldChange,
  onOAuthClick,
}: Readonly<LogInFormProps>) {
  const { t } = useTranslation();

  useToastMessages({ errorMessage, successMessage });

  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <FormFieldControl
        icon={<Mail className="size-4" />}
        id="email"
        inputProps={{ placeholder: 'example@email.com' }}
        inputType="email"
        label={t('auth.login.email')}
        message={fieldErrors.email}
        onValueChange={(value) => onFieldChange('email', value)}
        required
        value={form.email}
      />

      <div className="space-y-1.5">
        <FormFieldControl
          hidePasswordLabel={t('common.aria.hidePassword')}
          icon={<Lock className="size-4" />}
          id="password"
          inputProps={{ minLength: 8, placeholder: '********' }}
          inputType="password"
          label={t('auth.login.password')}
          message={fieldErrors.password}
          onValueChange={(value) => onFieldChange('password', value)}
          required
          showPasswordLabel={t('common.aria.showPassword')}
          value={form.password}
        />

        <div className="flex items-end justify-end">
          <Link
            className="text-xs font-semibold text-muted-foreground hover:text-primary"
            to="/auth/forgot-password"
          >
            {t('auth.login.forgotPassword')}
          </Link>
        </div>
      </div>

      <AuthSubmitButton
        canSubmit={canSubmit}
        idleLabel={t('auth.login.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.login.submitting')}
      />

      <AuthDivider className="my-7" label={t('auth.login.orContinueWith')} />

      <OAuthButtons disabled={isSubmitting} onProviderClick={onOAuthClick} />

      <AuthLinkPrompt
        linkLabel={t('auth.login.createAccount')}
        message={t('auth.login.newToLab')}
        to="/auth/signup"
      />
    </form>
  );
}
