import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';

type DialogActionButtonProps = Readonly<{
  disabled: boolean;
  isPending: boolean;
  label: string;
  loadingLabel: string;
  minWidthClassName?: string;
  onClick: () => void;
}>;

export function DialogActionButton({
  disabled,
  isPending,
  label,
  loadingLabel,
  minWidthClassName = 'min-w-28',
  onClick,
}: DialogActionButtonProps) {
  return (
    <Button
      className={`h-10 ${minWidthClassName} cursor-pointer rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary`}
      disabled={disabled}
      onClick={onClick}
      type="button"
    >
      {isPending ? (
        <span className="inline-flex items-center gap-2">
          <Spinner aria-hidden className="size-4" />
          {loadingLabel}
        </span>
      ) : (
        label
      )}
    </Button>
  );
}
