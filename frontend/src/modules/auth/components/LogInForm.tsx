import { type FormEvent, useState } from 'react';
import { Eye, EyeOff, Github, Lock, Mail } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import { Field, FieldLabel } from '@/components/ui/field';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import { Input } from '@/components/ui/input';
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner';
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
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);

  return (
    <form className="mt-8 space-y-4" onSubmit={onSubmit}>
      <Field>
        <FieldLabel htmlFor="email">
          <Mail className="size-4" />
          {`${t('auth.login.email')} *`}
        </FieldLabel>
        <Input
          id="email"
          onChange={(e) => onFieldChange('email', e.currentTarget.value)}
          placeholder="example@email.com"
          type="email"
          value={form.email}
        />
      </Field>

      {isEmailInvalid && (
        <p className="-mt-2 text-xs text-red-700">{t('auth.login.invalidEmail')}</p>
      )}

      <div className="space-y-1.5">
        <Field>
          <FieldLabel htmlFor="password">
            <Lock className="size-4" />
            {`${t('auth.login.password')} *`}
          </FieldLabel>
          <div className="relative">
            <Input
              id="password"
              minLength={8}
              onChange={(e) => onFieldChange('password', e.currentTarget.value)}
              placeholder="********"
              type={isPasswordVisible ? 'text' : 'password'}
              value={form.password}
            />
            <button
              aria-label={
                isPasswordVisible ? t('common.aria.hidePassword') : t('common.aria.showPassword')
              }
              className="absolute top-1/2 right-3 -translate-y-1/2 text-[color:var(--cl-tertiary)] transition hover:text-[color:var(--cl-primary)]"
              onClick={() => setIsPasswordVisible((current) => !current)}
              type="button"
            >
              {isPasswordVisible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
        </Field>

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

