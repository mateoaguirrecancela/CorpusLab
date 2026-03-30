import { type FormEvent } from 'react';
import { Lock } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import { Spinner } from '@/components/ui/spinner';
import { AuthFormField } from '@/modules/auth/components/AuthFormField';
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
      <AuthFormField
        icon={<Lock className="size-3.5" />}
        id="newPassword"
        label={t('auth.resetPassword.newPassword')}
        minLength={8}
        placeholder="********"
        type="password"
        value={form.newPassword}
        onChange={(value) => onFieldChange('newPassword', value)}
      />

      <AuthFormField
        icon={<Lock className="size-3.5" />}
        id="confirmPassword"
        label={t('auth.resetPassword.confirmPassword')}
        minLength={8}
        placeholder="********"
        type="password"
        value={form.confirmPassword}
        onChange={(value) => onFieldChange('confirmPassword', value)}
      />

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
