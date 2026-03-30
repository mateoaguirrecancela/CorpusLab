import { useTranslation } from 'react-i18next';

export function DashboardGreeting() {
  const { t } = useTranslation();

  return (
    <section className="px-6 py-6 sm:px-8 sm:py-8">
      <h1 className="text-5xl font-black tracking-tight text-[color:var(--cl-primary)]">
        {t('home.dashboard.title')}
      </h1>
      <p className="mt-3 text-2xl leading-tight text-[color:var(--cl-primary)]">
        {t('home.dashboard.greeting')}
      </p>
    </section>
  );
}
