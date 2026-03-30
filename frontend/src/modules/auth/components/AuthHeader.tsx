import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import corpusLabLogo from '@/assets/CorpusLab.png';
import { Button } from '@/components/ui/button';

export function AuthHeader() {
  const { t } = useTranslation();

  return (
    <header className="border-b border-[color:var(--cl-line)] bg-white/85 backdrop-blur-md">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-3">
        <Link
          to="/auth/login"
          className="group inline-flex items-center gap-2 text-[color:var(--cl-neutral)]"
        >
          <img alt="CorpusLab" className="h-10 w-10 rounded-sm object-cover" src={corpusLabLogo} />
          <span className="text-lg font-semibold tracking-tight">{t('common.appName')}</span>
        </Link>

        <nav className="flex items-center gap-3 text-sm font-medium">
          <Link
            className="rounded-md px-3 py-1 text-[color:var(--cl-primary)] hover:bg-[color:var(--cl-primary-soft)]"
            to="/auth/login"
          >
            {t('auth.header.login')}
          </Link>
          <Link to="/auth/signup">
            <Button
              className="h-8 rounded-md bg-[color:var(--cl-primary)] px-4 text-xs font-semibold text-white hover:bg-[color:var(--cl-primary-deep)] cursor-pointer"
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
