import { useTranslation } from 'react-i18next';

export function AppFooter() {
  const { t } = useTranslation();

  return (
    <footer className="relative z-0 border-t border-border bg-surface-footer">
      <div className="mx-auto flex w-full max-w-6xl flex-col items-center justify-between gap-3 px-6 py-5 text-xs text-muted-foreground sm:flex-row">
        <p>&copy; {t('common.footer.copyright')}</p>
        <div className="flex items-center gap-6">
          <a href="#" className="hover:text-primary">
            {t('common.footer.privacy')}
          </a>
          <a href="#" className="hover:text-primary">
            {t('common.footer.terms')}
          </a>
          <a href="#" className="hover:text-primary">
            {t('common.footer.contact')}
          </a>
        </div>
      </div>
    </footer>
  );
}
