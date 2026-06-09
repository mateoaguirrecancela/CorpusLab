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
      className={className}
      disabled={disabled}
      onClick={handleClick}
      size="action"
      type="button"
      variant="secondaryAction"
    >
      <ArrowLeft className="size-4" />
      {t('common.actions.goBack')}
    </Button>
  );
}
