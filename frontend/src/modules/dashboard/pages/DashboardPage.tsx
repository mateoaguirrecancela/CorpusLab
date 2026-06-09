import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { ActivityFeedAside } from '@/modules/dashboard/components/ActivityFeedAside';
import { AnnotationTrendsChart } from '@/modules/dashboard/components/AnnotationTrendsChart';
import { DashboardKPIs } from '@/modules/dashboard/components/DashboardKPIs';
import { DashboardProjectsSection } from '@/modules/dashboard/components/DashboardProjectsSection';
import { useActivityFeed } from '@/modules/dashboard/hooks/useActivityFeed';
import { useAnnotationTrends } from '@/modules/dashboard/hooks/useAnnotationTrends';
import { useDashboardMetrics } from '@/modules/dashboard/hooks/useDashboardMetrics';
import { useDashboardProjects } from '@/modules/dashboard/hooks/useDashboardProjects';

export default function DashboardPage() {
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
