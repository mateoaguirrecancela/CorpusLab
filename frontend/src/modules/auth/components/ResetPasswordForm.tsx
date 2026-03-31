import { type FormEvent, useState } from 'react';
import { Eye, EyeOff, Lock } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import { Field, FieldLabel } from '@/components/ui/field';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import { Input } from '@/components/ui/input';
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
  const [isNewPasswordVisible, setIsNewPasswordVisible] = useState(false);
  const [isConfirmPasswordVisible, setIsConfirmPasswordVisible] = useState(false);

  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <Field>
        <FieldLabel htmlFor="newPassword">
          <Lock className="size-3.5" />
          {t('auth.resetPassword.newPassword')}
        </FieldLabel>
        <div className="relative">
          <Input
            id="newPassword"
            minLength={8}
            onChange={(e) => onFieldChange('newPassword', e.currentTarget.value)}
            placeholder="********"
            type={isNewPasswordVisible ? 'text' : 'password'}
            value={form.newPassword}
          />
          <button
            aria-label={
              isNewPasswordVisible ? t('common.aria.hidePassword') : t('common.aria.showPassword')
            }
            className="absolute top-1/2 right-3 -translate-y-1/2 text-[color:var(--cl-tertiary)] transition hover:text-[color:var(--cl-primary)] outline-none focus-visible:text-[color:var(--cl-primary)]"
            onClick={() => setIsNewPasswordVisible((current) => !current)}
            type="button"
          >
            {isNewPasswordVisible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
      </Field>

      {form.newPassword.length > 0 && form.newPassword.length < 8 && (
        <p className="-mt-2 text-xs text-red-700">{t('auth.signup.passwordLength')}</p>
      )}

      <Field>
        <FieldLabel htmlFor="confirmPassword">
          <Lock className="size-3.5" />
          {t('auth.resetPassword.confirmPassword')}
        </FieldLabel>
        <div className="relative">
          <Input
            id="confirmPassword"
            minLength={8}
            onChange={(e) => onFieldChange('confirmPassword', e.currentTarget.value)}
            placeholder="********"
            type={isConfirmPasswordVisible ? 'text' : 'password'}
            value={form.confirmPassword}
          />
          <button
            aria-label={
              isConfirmPasswordVisible
                ? t('common.aria.hidePassword')
                : t('common.aria.showPassword')
            }
            className="absolute top-1/2 right-3 -translate-y-1/2 text-[color:var(--cl-tertiary)] transition hover:text-[color:var(--cl-primary)] outline-none focus-visible:text-[color:var(--cl-primary)]"
            onClick={() => setIsConfirmPasswordVisible((current) => !current)}
            type="button"
          >
            {isConfirmPasswordVisible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
      </Field>

      {form.confirmPassword.length > 0 && form.confirmPassword.length < 8 && (
        <p className="-mt-2 text-xs text-red-700">{t('auth.signup.passwordLength')}</p>
      )}

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
