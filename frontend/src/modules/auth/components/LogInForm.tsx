import { type FormEvent, useEffect } from 'react';
import { Github, Lock, Mail } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { toast } from 'sonner';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import { type LoginFormState } from '@/modules/auth/types/login';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type LogInFormProps = {
  form: LoginFormState;
  canSubmit: boolean;
  isSubmitting: boolean;
  errorMessage: string;
  successMessage: string;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onFieldChange: <K extends keyof LoginFormState>(field: K, value: LoginFormState[K]) => void;
  onOAuthClick: (provider: OAuthProvider) => void;
};

export function LogInForm({
  form,
  canSubmit,
  isSubmitting,
  errorMessage,
  successMessage,
  onSubmit,
  onFieldChange,
  onOAuthClick,
}: LogInFormProps) {
  const { t } = useTranslation();
  const trimmedEmail = form.email.trim();
  const isEmailInvalid = trimmedEmail.length > 0 && !EMAIL_REGEX.test(trimmedEmail);

  useEffect(() => {
    if (errorMessage.trim().length > 0) {
      toast.error(errorMessage);
    }
  }, [errorMessage]);

  useEffect(() => {
    if (successMessage.trim().length > 0) {
      toast.success(successMessage);
    }
  }, [successMessage]);

  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <FormFieldControl
        icon={<Mail className="size-4" />}
        id="email"
        inputProps={{ placeholder: 'example@email.com' }}
        inputType="email"
        label={t('auth.login.email')}
        message={isEmailInvalid ? t('auth.login.invalidEmail') : undefined}
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
          onValueChange={(value) => onFieldChange('password', value)}
          required
          showPasswordLabel={t('common.aria.showPassword')}
          value={form.password}
        />

        <div className="flex items-end justify-end">
          <Link
            className="text-xs font-semibold text-[color:var(--cl-tertiary)] hover:text-[color:var(--cl-primary)]"
            to="/auth/forgot-password"
          >
            {t('auth.login.forgotPassword')}
          </Link>
        </div>
      </div>

      <SubmitButtonWithSpinner
        className="h-11 w-full rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white shadow-[0_8px_16px_-10px_rgba(49,46,129,0.95)] hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
        disabled={!canSubmit}
        idleLabel={t('auth.login.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.login.submitting')}
      />

      <div className="my-7 flex items-center gap-3 text-[0.67rem] font-bold tracking-[0.12em] text-[color:var(--cl-tertiary)] uppercase">
        <span className="h-px flex-1 bg-[color:var(--cl-line)]" />
        <span>{t('auth.login.orContinueWith')}</span>
        <span className="h-px flex-1 bg-[color:var(--cl-line)]" />
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <Button
          className="h-10 rounded-md border border-[color:var(--cl-line)] bg-white text-sm font-semibold text-[color:var(--cl-neutral)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
          disabled={isSubmitting}
          onClick={() => onOAuthClick('google')}
          type="button"
          variant="outline"
        >
          <span className="text-base text-red-500">G</span>
          Google
        </Button>
        <Button
          className="h-10 rounded-md border border-[color:var(--cl-line)] bg-white text-sm font-semibold text-[color:var(--cl-neutral)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
          disabled={isSubmitting}
          onClick={() => onOAuthClick('github')}
          type="button"
          variant="outline"
        >
          <Github className="size-4" />
          GitHub
        </Button>
      </div>

      <p className="mt-8 text-center text-sm text-[color:var(--cl-secondary)]">
        {t('auth.login.newToLab')}{' '}
        <Link
          className="font-semibold text-[color:var(--cl-primary)] hover:underline"
          to="/auth/signup"
        >
          {t('auth.login.createAccount')}
        </Link>
      </p>
    </form>
  );
}
