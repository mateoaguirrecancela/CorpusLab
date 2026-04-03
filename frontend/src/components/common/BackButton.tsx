import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router';
import { Button } from '@/components/ui/button';

type BackButtonProps = {
  fallbackTo?: string;
  className?: string;
};

export function BackButton({ fallbackTo, className }: Readonly<BackButtonProps>) {
  const navigate = useNavigate();

  const handleClick = () => {
    if (fallbackTo) {
      navigate(fallbackTo);
      return;
    }

    navigate(-1);
  };

  return (
    <Button
      className={[
        'h-10 rounded-md border border-[color:var(--cl-line)] bg-white px-4 text-sm font-semibold text-[color:var(--cl-primary)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      onClick={handleClick}
      type="button"
      variant="outline"
    >
      <ArrowLeft className="size-4" />
      Volver
    </Button>
  );
}
