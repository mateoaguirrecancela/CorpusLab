import { type FormEventHandler } from 'react';
import { Lock } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { useToastMessages } from '@/hooks/useToastMessages';
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner';
import { type ResetPasswordFormState } from '@/modules/auth/types/resetPassword';

type ResetPasswordFormProps = {
  form: ResetPasswordFormState;
  canSubmit: boolean;
  isSubmitting: boolean;
  errorMessage: string;
  onSubmit: FormEventHandler<HTMLFormElement>;
  onFieldChange: <K extends keyof ResetPasswordFormState>(
    field: K,
    value: ResetPasswordFormState[K],
  ) => void;
};

export function ResetPasswordForm({
  form,
  canSubmit,
  isSubmitting,
  errorMessage,
  onSubmit,
  onFieldChange,
}: Readonly<ResetPasswordFormProps>) {
  const { t } = useTranslation();

  useToastMessages({ errorMessage });

  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <FormFieldControl
        hidePasswordLabel={t('common.aria.hidePassword')}
        icon={<Lock className="size-3.5" />}
        id="newPassword"
        inputProps={{ minLength: 8, placeholder: '********' }}
        inputType="password"
        label={t('auth.resetPassword.newPassword')}
        message={
          form.newPassword.length > 0 && form.newPassword.length < 8
            ? t('auth.signup.passwordLength')
            : undefined
        }
        messageClassName="-mt-2"
        onValueChange={(value) => onFieldChange('newPassword', value)}
        showPasswordLabel={t('common.aria.showPassword')}
        value={form.newPassword}
      />

      <FormFieldControl
        hidePasswordLabel={t('common.aria.hidePassword')}
        icon={<Lock className="size-3.5" />}
        id="confirmPassword"
        inputProps={{ minLength: 8, placeholder: '********' }}
        inputType="password"
        label={t('auth.resetPassword.confirmPassword')}
        message={
          form.confirmPassword.length > 0 && form.confirmPassword.length < 8
            ? t('auth.signup.passwordLength')
            : undefined
        }
        messageClassName="-mt-2"
        onValueChange={(value) => onFieldChange('confirmPassword', value)}
        showPasswordLabel={t('common.aria.showPassword')}
        value={form.confirmPassword}
      />

      {form.confirmPassword.length > 0 && form.confirmPassword !== form.newPassword && (
        <p className="-mt-2 text-xs text-destructive">{t('auth.resetPassword.passwordMismatch')}</p>
      )}

      <SubmitButtonWithSpinner
        className="h-11 w-full rounded-md bg-primary text-sm font-semibold text-white shadow-[var(--shadow-primary-action)] hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
        disabled={!canSubmit}
        idleLabel={t('auth.resetPassword.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.resetPassword.submitting')}
      />

      <p className="mt-8 text-center text-sm text-muted-foreground">
        {t('auth.resetPassword.alreadyUpdated')}{' '}
        <Link className="font-semibold text-primary hover:underline" to="/auth/login">
          {t('auth.resetPassword.goToLogin')}
        </Link>
      </p>
    </form>
  );
}
