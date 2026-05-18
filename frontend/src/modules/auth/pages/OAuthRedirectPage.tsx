import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { useToastMessages } from '@/hooks/useToastMessages';
import { Spinner } from '@/components/ui/spinner';
import { AuthCard } from '@/modules/auth/components/AuthCard';
import { useOAuthRedirect } from '@/modules/auth/hooks/useOAuthRedirect';

export default function OAuthRedirectPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const { errorMessage, isCompleting, providerLabel } = useOAuthRedirect(searchParams);

  useToastMessages({ errorMessage });

  return (
    <AuthCard
      className="max-w-md text-center"
      title={t('auth.oauth.title', { provider: providerLabel })}
    >
      {isCompleting ? (
        <p className="mt-4 inline-flex items-center gap-2 text-sm text-muted-foreground">
          <Spinner aria-hidden className="size-4" />
          {t('auth.oauth.completing')}
        </p>
      ) : (
        <p className="mt-4 text-sm text-destructive">{errorMessage}</p>
      )}
    </AuthCard>
  );
}
