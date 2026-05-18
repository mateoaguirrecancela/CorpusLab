import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import corpusLabLogo from '@/assets/corpuslab.webp';
import notFoundIllustration from '@/assets/404.svg';
import { buttonVariants } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type NotFoundPageProps = {
  embedded?: boolean;
};

export default function NotFoundPage({ embedded = false }: Readonly<NotFoundPageProps>) {
  const { t } = useTranslation();

  return (
    <div className="bg-background text-foreground">
      {!embedded && (
        <header className="border-b border-border bg-surface-soft">
          <div className="mx-auto flex h-18 w-full max-w-7xl items-center px-6 sm:px-8">
            <Link className="inline-flex items-center gap-2" to="/home">
              <img
                alt={t('common.appName')}
                className="h-10 w-10 rounded-sm object-cover"
                src={corpusLabLogo}
              />
              <span className="text-lg font-semibold tracking-tight text-foreground">
                {t('common.appName')}
              </span>
            </Link>
          </div>
        </header>
      )}

      <section className="grid items-center gap-10 px-6 py-12 sm:px-10 lg:grid-cols-[minmax(0,0.85fr)_minmax(320px,1fr)]">
        <div className="mx-auto w-full max-w-lg lg:ml-auto lg:mr-0">
          <p className="mt-4 text-7xl font-semibold tracking-tight text-primary sm:text-8xl">
            {t('common.notFound.code')}
          </p>

          <h1 className="mt-4 text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
            {t('common.notFound.name')}
          </h1>

          <Link
            className={cn(
              buttonVariants(),
              'mt-8 h-10 rounded-md bg-primary px-4 text-sm font-semibold text-primary-foreground hover:bg-primary-strong',
            )}
            to="/home"
          >
            {t('common.notFound.primaryAction')}
          </Link>
        </div>

        <div className="mx-auto w-full max-w-xl lg:ml-0">
          <img
            alt={t('common.notFound.imageAlt')}
            className="h-auto w-full"
            src={notFoundIllustration}
          />
        </div>
      </section>
    </div>
  );
}
