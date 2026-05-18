import { Link } from 'react-router';
import { SubmitButtonWithSpinner } from '@/components/ui/submit-button-with-spinner';

const AUTH_SUBMIT_BUTTON_CLASS =
  'h-11 w-full cursor-pointer rounded-md bg-primary text-sm font-semibold text-white shadow-[var(--shadow-primary-action)] hover:bg-primary-strong disabled:bg-secondary';

type AuthSubmitButtonProps = {
  canSubmit: boolean;
  idleLabel: string;
  isSubmitting: boolean;
  submittingLabel: string;
};

type AuthDividerProps = {
  label: string;
  className?: string;
};

type AuthLinkPromptProps = {
  message: string;
  linkLabel: string;
  to: string;
};

export function AuthSubmitButton({
  canSubmit,
  idleLabel,
  isSubmitting,
  submittingLabel,
}: Readonly<AuthSubmitButtonProps>) {
  return (
    <SubmitButtonWithSpinner
      className={AUTH_SUBMIT_BUTTON_CLASS}
      disabled={!canSubmit}
      idleLabel={idleLabel}
      isSubmitting={isSubmitting}
      submittingLabel={submittingLabel}
    />
  );
}

export function AuthDivider({ label, className = 'my-8' }: Readonly<AuthDividerProps>) {
  return (
    <div
      className={`${className} flex items-center gap-3 text-[0.67rem] font-bold tracking-[0.12em] text-muted-foreground uppercase`}
    >
      <span className="h-px flex-1 bg-border" />
      <span>{label}</span>
      <span className="h-px flex-1 bg-border" />
    </div>
  );
}

export function AuthLinkPrompt({ message, linkLabel, to }: Readonly<AuthLinkPromptProps>) {
  return (
    <p className="mt-8 text-center text-sm text-muted-foreground">
      {message}{' '}
      <Link className="font-semibold text-primary hover:underline" to={to}>
        {linkLabel}
      </Link>
    </p>
  );
}
