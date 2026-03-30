import { cn } from '@/lib/utils';

type FeedbackVariant = 'error' | 'success';

type FeedbackMessageProps = {
  message: string;
  variant: FeedbackVariant;
  className?: string;
};

const FEEDBACK_VARIANT_STYLES: Record<FeedbackVariant, string> = {
  error: 'border-red-200 bg-red-50 text-red-700',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-700',
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
