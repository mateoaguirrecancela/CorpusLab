import { cn } from '@/shared/utils/cn';

type FeedbackVariant = 'error' | 'success';

type FeedbackMessageProps = Readonly<{
  message: string;
  variant: FeedbackVariant;
  className?: string;
}>;

const FEEDBACK_VARIANT_STYLES: Record<FeedbackVariant, string> = {
  error: 'border-danger-border bg-danger-soft text-destructive',
  success: 'border-success-border bg-success-soft text-success',
};

export function FeedbackMessage({ message, variant, className }: FeedbackMessageProps) {
  if (message.trim().length === 0) {
    return null;
  }

  return (
    <p
      aria-live={variant === 'error' ? 'assertive' : 'polite'}
      className={cn(
        'rounded-md border px-3 py-2 text-sm',
        FEEDBACK_VARIANT_STYLES[variant],
        className,
      )}
      role="status"
    >
      {message}
    </p>
  );
}
