import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';

type DialogActionButtonProps = Readonly<{
  disabled: boolean;
  isPending: boolean;
  label: string;
  loadingLabel: string;
  size?: 'action-sm' | 'action-md' | 'action-wide' | 'action-xl';
  onClick: () => void;
}>;

export function DialogActionButton({
  disabled,
  isPending,
  label,
  loadingLabel,
  size = 'action-sm',
  onClick,
}: DialogActionButtonProps) {
  return (
    <Button disabled={disabled} onClick={onClick} size={size} type="button" variant="primaryAction">
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
