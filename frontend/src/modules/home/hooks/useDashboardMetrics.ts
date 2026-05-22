import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { type TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import {
  dashboardQueryKeys,
  getDashboardMetrics,
  getDashboardMetricsErrorMessage,
} from '@/modules/home/services/dashboardService';
import {
  type DashboardMetric,
  type DashboardMetricTrendDirection,
  type DashboardMetricsResponse,
} from '@/modules/home/types/dashboard';

type DashboardMetricsState = Readonly<{
  errorMessage: string;
  isLoading: boolean;
  metrics: DashboardMetric[];
}>;

function toTrendDirection(value: number): DashboardMetricTrendDirection {
  if (value > 0) {
    return 'up';
  }

  if (value < 0) {
    return 'down';
  }

  return 'neutral';
}

function buildMetrics(data: DashboardMetricsResponse | undefined, t: TFunction): DashboardMetric[] {
  const annotationsDelta = (data?.annotationsToday ?? 0) - (data?.annotationsYesterday ?? 0);
  const alertsSinceYesterday = data?.alertsSinceYesterday ?? 0;

  return [
    {
      kind: 'active-projects',
      supportingText: t('home.dashboard.kpis.activeProjects.supportingText', {
        count: data?.activeResearchGroups ?? 0,
      }),
      value: data?.activeProjects ?? 0,
    },
    {
      kind: 'annotations-today',
      supportingText: t('home.dashboard.kpis.annotationsToday.supportingText'),
      trend: {
        direction: toTrendDirection(annotationsDelta),
        label: t(
          annotationsDelta >= 0
            ? 'home.dashboard.kpis.annotationsToday.trend'
            : 'home.dashboard.kpis.annotationsToday.trendDown',
        ),
        value: Math.abs(annotationsDelta),
      },
      value: data?.annotationsToday ?? 0,
    },
    {
      kind: 'pending-annotations',
      supportingText: t('home.dashboard.kpis.pendingAnnotations.supportingText', {
        count: data?.pendingProjects ?? 0,
      }),
      value: data?.pendingAnnotations ?? 0,
    },
    {
      kind: 'open-alerts',
      supportingText: t('home.dashboard.kpis.openAlerts.supportingText'),
      trend: {
        direction: alertsSinceYesterday > 0 ? 'up' : 'neutral',
        label: t('home.dashboard.kpis.openAlerts.trend'),
        value: alertsSinceYesterday,
      },
      value: data?.openAlerts ?? 0,
    },
  ] satisfies DashboardMetric[];
}

export function useDashboardMetrics(): DashboardMetricsState {
  const { t } = useTranslation();
  const query = useQuery({
    queryKey: dashboardQueryKeys.metrics,
    queryFn: getDashboardMetrics,
  });

  const metrics = useMemo<DashboardMetric[]>(() => buildMetrics(query.data, t), [query.data, t]);

  return {
    errorMessage: query.isError ? getDashboardMetricsErrorMessage(query.error) : '',
    isLoading: query.isLoading,
    metrics,
  };
}
