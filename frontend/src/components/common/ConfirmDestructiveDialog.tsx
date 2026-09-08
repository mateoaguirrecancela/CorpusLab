import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Spinner } from '@/components/ui/spinner';

type ConfirmDestructiveDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  confirmingLabel?: string;
  isConfirming?: boolean;
  onConfirm: () => void | Promise<void>;
};

export function ConfirmDestructiveDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  confirmingLabel,
  isConfirming = false,
  onConfirm,
}: Readonly<ConfirmDestructiveDialogProps>) {
  const { t } = useTranslation();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl" showCloseButton={!isConfirming}>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>

        <DialogDescription>{description}</DialogDescription>

        <DialogFooter>
          <Button
            disabled={isConfirming}
            onClick={() => onOpenChange(false)}
            size="action"
            type="button"
            variant="secondaryAction"
          >
            {t('common.actions.cancel')}
          </Button>

          <Button
            disabled={isConfirming}
            onClick={() => void onConfirm()}
            size="action-md"
            type="button"
            variant="danger"
          >
            {isConfirming ? (
              <span className="inline-flex items-center gap-2">
                <Spinner aria-hidden className="size-4" />
                {confirmingLabel ?? confirmLabel}
              </span>
            ) : (
              confirmLabel
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
