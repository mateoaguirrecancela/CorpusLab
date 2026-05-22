import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { useToastMessages } from '@/hooks/useToastMessages';
import { Spinner } from '@/components/ui/spinner';
import { useOAuthRedirect } from '@/modules/auth/hooks/useOAuthRedirect';

export default function OAuthRedirectPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const { errorMessage } = useOAuthRedirect(searchParams);

  useToastMessages({ errorMessage });

  return (
    <div className="flex min-h-screen items-center justify-center text-sm text-muted-foreground">
      <span className="inline-flex items-center gap-2">
        <Spinner aria-hidden className="size-4" />
        {t('auth.oauth.completing')}
      </span>
    </div>
  );
}
