import { type FormEventHandler } from 'react';
import { Lock } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { useToastMessages } from '@/hooks/useToastMessages';
import { AuthLinkPrompt, AuthSubmitButton } from '@/modules/auth/components/AuthFormActions';
import { type ResetPasswordFormState } from '@/modules/auth/types/resetPassword';

type ResetPasswordFormProps = {
  form: ResetPasswordFormState;
  canSubmit: boolean;
  fieldErrors?: Partial<Record<keyof ResetPasswordFormState, string>>;
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
  fieldErrors = {},
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
        message={fieldErrors.newPassword}
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
        message={fieldErrors.confirmPassword}
        onValueChange={(value) => onFieldChange('confirmPassword', value)}
        showPasswordLabel={t('common.aria.showPassword')}
        value={form.confirmPassword}
      />

      <AuthSubmitButton
        canSubmit={canSubmit}
        idleLabel={t('auth.resetPassword.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.resetPassword.submitting')}
      />

      <AuthLinkPrompt
        linkLabel={t('auth.resetPassword.goToLogin')}
        message={t('auth.resetPassword.alreadyUpdated')}
        to="/auth/login"
      />
    </form>
  );
}
