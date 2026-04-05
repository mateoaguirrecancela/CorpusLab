import { type FormEventHandler } from 'react';
import { Github, Lock, Mail } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import { useToastMessages } from '@/hooks/useToastMessages';
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import { type LoginFormState } from '@/modules/auth/types/login';
import { isEmailValid } from '@/modules/auth/utils/validation';

type LogInFormProps = {
  form: LoginFormState;
  canSubmit: boolean;
  isSubmitting: boolean;
  errorMessage: string;
  successMessage: string;
  onSubmit: FormEventHandler<HTMLFormElement>;
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
}: Readonly<LogInFormProps>) {
  const { t } = useTranslation();
  const trimmedEmail = form.email.trim();
  const isEmailInvalid = trimmedEmail.length > 0 && !isEmailValid(trimmedEmail);

  useToastMessages({ errorMessage, successMessage });

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
            className="text-xs font-semibold text-muted-foreground hover:text-primary"
            to="/auth/forgot-password"
          >
            {t('auth.login.forgotPassword')}
          </Link>
        </div>
      </div>

      <SubmitButtonWithSpinner
        className="h-11 w-full rounded-md bg-primary text-sm font-semibold text-white shadow-[var(--shadow-primary-action)] hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
        disabled={!canSubmit}
        idleLabel={t('auth.login.submit')}
        isSubmitting={isSubmitting}
        submittingLabel={t('auth.login.submitting')}
      />

      <div className="my-7 flex items-center gap-3 text-[0.67rem] font-bold tracking-[0.12em] text-muted-foreground uppercase">
        <span className="h-px flex-1 bg-border" />
        <span>{t('auth.login.orContinueWith')}</span>
        <span className="h-px flex-1 bg-border" />
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <Button
          className="h-10 rounded-md border border-border bg-surface-base text-sm font-semibold text-foreground hover:bg-accent cursor-pointer"
          disabled={isSubmitting}
          onClick={() => onOAuthClick('google')}
          type="button"
          variant="outline"
        >
          <span className="text-base text-red-500">G</span>
          <span>Google</span>
        </Button>
        <Button
          className="h-10 rounded-md border border-border bg-surface-base text-sm font-semibold text-foreground hover:bg-accent cursor-pointer"
          disabled={isSubmitting}
          onClick={() => onOAuthClick('github')}
          type="button"
          variant="outline"
        >
          <Github className="size-4" />
          <span>GitHub</span>
        </Button>
      </div>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        {t('auth.login.newToLab')}{' '}
        <Link className="font-semibold text-primary hover:underline" to="/auth/signup">
          {t('auth.login.createAccount')}
        </Link>
      </p>
    </form>
  );
}
