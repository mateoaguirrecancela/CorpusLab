import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';

export default function HomePage() {
  const { t } = useTranslation();

  return (
    <PageContainer>
      <h1 className="text-4xl font-black tracking-tight text-primary">
        {t('home.dashboard.title')}
      </h1>
      <p className="mt-3 text-2xl leading-tight text-primary">Dashboard</p>
    </PageContainer>
  );
}
