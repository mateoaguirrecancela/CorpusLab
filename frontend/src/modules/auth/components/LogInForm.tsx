import { type FormEvent } from 'react';
import { Github, Lock, Mail } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner';
import { AuthFormField } from '@/modules/auth/components/AuthFormField';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import { type LoginFormState } from '@/modules/auth/types/login';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type LogInFormProps = {
  form: LoginFormState;
  canSubmit: boolean;
  isSubmitting: boolean;
  errorMessage: string;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
  onFieldChange: <K extends keyof LoginFormState>(field: K, value: LoginFormState[K]) => void;
  onOAuthClick: (provider: OAuthProvider) => void;
};

export function LogInForm({
  form,
  canSubmit,
  isSubmitting,
  errorMessage,
  onSubmit,
  onFieldChange,
  onOAuthClick,
}: LogInFormProps) {
  const { t } = useTranslation();
  const trimmedEmail = form.email.trim();
  const isEmailInvalid = trimmedEmail.length > 0 && !EMAIL_REGEX.test(trimmedEmail);

  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <AuthFormField
        icon={<Mail className="size-3.5" />}
        id="email"
        label={`${t('auth.login.email')} *`}
        placeholder="example@email.com"
        type="email"
        value={form.email}
        onChange={(value) => onFieldChange('email', value)}
      />

      {isEmailInvalid && (
        <p className="-mt-2 text-xs text-red-700">{t('auth.login.invalidEmail')}</p>
      )}

      <div className="space-y-1.5">
        <AuthFormField
          icon={<Lock className="size-3.5" />}
          id="password"
          label={`${t('auth.login.password')} *`}
          placeholder="********"
          minLength={8}
          type="password"
          value={form.password}
          onChange={(value) => onFieldChange('password', value)}
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

      <FeedbackMessage message={errorMessage} variant="error" />

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
