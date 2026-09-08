import { type FormEventHandler } from 'react';
import { Mail } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { useToastMessages } from '@/shared/hooks/useToastMessages';
import { AuthLinkPrompt, AuthSubmitButton } from '@/modules/auth/components/AuthFormActions';
import { type ForgotPasswordFormState } from '@/modules/auth/types/forgotPassword';

type ForgotPasswordFormProps = {
  form: ForgotPasswordFormState;
  canSubmit: boolean;
  fieldErrors?: Partial<Record<keyof ForgotPasswordFormState, string>>;
  isSubmitting: boolean;
  errorMessage: string;
  successMessage: string;
  onSubmit: FormEventHandler<HTMLFormElement>;
  onFieldChange: <K extends keyof ForgotPasswordFormState>(
    field: K,
    value: ForgotPasswordFormState[K],
  ) => void;
};

export function ForgotPasswordForm({
  form,
  canSubmit,
  fieldErrors = {},
  isSubmitting,
  errorMessage,
  successMessage,
  onSubmit,
  onFieldChange,
}: Readonly<ForgotPasswordFormProps>) {
  const { t } = useTranslation();

  useToastMessages({ errorMessage, successMessage });

  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <div className="space-y-4">
        <FormFieldControl
          icon={<Mail className="size-4" />}
          id="email"
          inputProps={{ placeholder: 'example@email.com' }}
          inputType="email"
          label={t('auth.forgotPassword.email')}
          message={fieldErrors.email}
          onValueChange={(value) => onFieldChange('email', value)}
          required
          value={form.email}
        />
      </div>

      <AuthSubmitButton
        canSubmit={canSubmit}
        idleLabel={t('auth.forgotPassword.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.forgotPassword.submitting')}
      />

      <AuthLinkPrompt
        linkLabel={t('auth.forgotPassword.backToLogin')}
        message={t('auth.forgotPassword.remembered')}
        to="/auth/login"
      />
    </form>
  );
}
