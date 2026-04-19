import { ArrowLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { Button } from '@/components/ui/button';

type BackButtonProps = {
  fallbackTo?: string;
  className?: string;
  disabled?: boolean;
  onBeforeNavigate?: () => Promise<boolean | void> | boolean | void;
};

export function BackButton({
  fallbackTo,
  className,
  disabled = false,
  onBeforeNavigate,
}: Readonly<BackButtonProps>) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const handleClick = async () => {
    if (disabled) {
      return;
    }

    if (onBeforeNavigate) {
      const shouldNavigate = await onBeforeNavigate();
      if (shouldNavigate === false) {
        return;
      }
    }

    if (fallbackTo) {
      navigate(fallbackTo);
      return;
    }

    navigate(-1);
  };

  return (
    <Button
      className={[
        'h-10 rounded-md border border-border bg-surface-base px-4 text-sm font-semibold text-primary hover:bg-accent cursor-pointer',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      disabled={disabled}
      onClick={handleClick}
      type="button"
      variant="outline"
    >
      <ArrowLeft className="size-4" />
      {t('common.actions.goBack')}
    </Button>
  );
}
