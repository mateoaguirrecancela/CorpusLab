import { type FormEvent } from 'react';
import { Lock } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import { Spinner } from '@/components/ui/spinner';
import { type ResetPasswordFormState } from '@/modules/auth/types/resetPassword';

type ResetPasswordFormProps = {
  form: ResetPasswordFormState;
  canSubmit: boolean;
  isSubmitting: boolean;
  errorMessage: string;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
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
}: ResetPasswordFormProps) {
  const { t } = useTranslation();

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
        <p className="-mt-2 text-xs text-red-700">{t('auth.resetPassword.passwordMismatch')}</p>
      )}

      <Button
        className="h-11 w-full rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white shadow-[0_8px_16px_-10px_rgba(49,46,129,0.95)] hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
        disabled={!canSubmit}
        type="submit"
      >
        {isSubmitting ? (
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('auth.resetPassword.submitting')}
          </span>
        ) : (
          t('auth.resetPassword.submit')
        )}
      </Button>

      <FeedbackMessage message={errorMessage} variant="error" />

      <p className="mt-8 text-center text-sm text-[color:var(--cl-secondary)]">
        {t('auth.resetPassword.alreadyUpdated')}{' '}
        <Link
          className="font-semibold text-[color:var(--cl-primary)] hover:underline"
          to="/auth/login"
        >
          {t('auth.resetPassword.goToLogin')}
        </Link>
      </p>
    </form>
  );
}
