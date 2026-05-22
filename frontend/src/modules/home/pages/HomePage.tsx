import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { ActivityFeedAside } from '@/modules/home/components/dashboard/ActivityFeedAside';
import { AnnotationTrendsChart } from '@/modules/home/components/dashboard/AnnotationTrendsChart';
import { DashboardKPIs } from '@/modules/home/components/dashboard/DashboardKPIs';
import { DashboardProjectsSection } from '@/modules/home/components/dashboard/DashboardProjectsSection';
import { useActivityFeed } from '@/modules/home/hooks/useActivityFeed';
import { useAnnotationTrends } from '@/modules/home/hooks/useAnnotationTrends';
import { useDashboardMetrics } from '@/modules/home/hooks/useDashboardMetrics';
import { useDashboardProjects } from '@/modules/home/hooks/useDashboardProjects';

export default function HomePage() {
  const { t } = useTranslation();
  const metricsState = useDashboardMetrics();
  const trendsState = useAnnotationTrends();
  const projectsState = useDashboardProjects();
  const activityState = useActivityFeed();

  return (
    <PageContainer className="space-y-6">
      <PageHeader title={t('home.dashboard.title')} />

      <div className="space-y-5">
        <DashboardKPIs
          errorMessage={metricsState.errorMessage}
          isLoading={metricsState.isLoading}
          metrics={metricsState.metrics}
        />

        <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_22rem]">
          <div className="min-w-0 space-y-5">
            <AnnotationTrendsChart
              data={trendsState.data}
              errorMessage={trendsState.errorMessage}
              filterOptions={trendsState.filterOptions}
              isLoading={trendsState.isLoading}
              selectedGroupFilter={trendsState.selectedGroupFilter}
              series={trendsState.series}
              onGroupFilterChange={trendsState.setSelectedGroupFilter}
            />

            <DashboardProjectsSection
              advancedProjects={projectsState.advancedProjects}
              errorMessage={projectsState.errorMessage}
              isLoading={projectsState.isLoading}
              recentProjects={projectsState.recentProjects}
              onOpenProject={projectsState.openProject}
              onOpenProjects={projectsState.openProjects}
            />
          </div>

          <div className="min-h-0 xl:relative">
            <div className="xl:absolute xl:inset-0">
              <ActivityFeedAside
                listStatus={activityState.listStatus}
                locale={activityState.locale}
                notifications={activityState.notifications}
                onNotificationClick={activityState.markNotificationAsRead}
              />
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
