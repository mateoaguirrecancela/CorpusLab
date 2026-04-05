import { type FormEventHandler } from 'react';
import { Mail } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { useToastMessages } from '@/hooks/useToastMessages';
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner';
import { type ForgotPasswordFormState } from '@/modules/auth/types/forgotPassword';

type ForgotPasswordFormProps = {
  form: ForgotPasswordFormState;
  canSubmit: boolean;
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
      <FormFieldControl
        icon={<Mail className="size-4" />}
        id="email"
        inputProps={{ placeholder: 'example@email.com' }}
        inputType="email"
        label={t('auth.login.email')}
        onValueChange={(value) => onFieldChange('email', value)}
        required
        value={form.email}
      />

      <SubmitButtonWithSpinner
        className="h-11 w-full rounded-md bg-primary text-sm font-semibold text-white shadow-[var(--shadow-primary-action)] hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
        disabled={!canSubmit}
        idleLabel={t('auth.forgotPassword.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.forgotPassword.submitting')}
      />

      <p className="mt-8 text-center text-sm text-muted-foreground">
        {t('auth.forgotPassword.remembered')}{' '}
        <Link className="font-semibold text-primary hover:underline" to="/auth/login">
          {t('auth.forgotPassword.backToLogin')}
        </Link>
      </p>
    </form>
  );
}
