import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import corpusLabLogo from '@/assets/corpuslab.webp';
import { Button } from '@/components/ui/button';

export function AuthHeader() {
  const { t } = useTranslation();

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-surface-soft backdrop-blur-md">
      <div className="mx-auto flex w-full max-w-7xl items-center justify-between px-6 py-3">
        <Link to="/auth/login" className="group inline-flex items-center gap-2 text-foreground">
          <img alt="CorpusLab" className="h-10 w-10 rounded-sm object-cover" src={corpusLabLogo} />
          <span className="text-lg font-semibold tracking-tight">{t('common.appName')}</span>
        </Link>

        <nav className="flex items-center gap-3 text-sm font-medium">
          <Link className="rounded-md px-3 py-1 text-primary hover:bg-accent" to="/auth/login">
            {t('auth.header.login')}
          </Link>
          <Link to="/auth/signup">
            <Button
              className="h-8 rounded-md bg-primary px-4 text-xs font-semibold text-white hover:bg-primary-strong cursor-pointer"
              type="button"
            >
              {t('auth.header.signup')}
            </Button>
          </Link>
        </nav>
      </div>
    </header>
  );
}
