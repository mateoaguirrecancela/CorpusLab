import { api } from '@/lib/api';
import { extractTranslatedApiErrorMessage } from '@/lib/apiErrors';
import {
  type AnnotationTrendsResponse,
  type DashboardMetricsResponse,
  type DashboardProjectsResponse,
} from '@/modules/home/types/dashboard';

export const DASHBOARD_QUERY_KEY = ['dashboard'] as const;

export const dashboardQueryKeys = {
  all: DASHBOARD_QUERY_KEY,
  metrics: [...DASHBOARD_QUERY_KEY, 'metrics'] as const,
  projects: [...DASHBOARD_QUERY_KEY, 'projects'] as const,
  trends: [...DASHBOARD_QUERY_KEY, 'annotation-trends'] as const,
};

export async function getDashboardMetrics(): Promise<DashboardMetricsResponse> {
  const response = await api.get<DashboardMetricsResponse>('/dashboard/metrics');
  return response.data;
}

export async function getAnnotationTrends(): Promise<AnnotationTrendsResponse> {
  const response = await api.get<AnnotationTrendsResponse>('/dashboard/annotation-trends');
  return response.data;
}

export async function getDashboardProjects(): Promise<DashboardProjectsResponse> {
  const response = await api.get<DashboardProjectsResponse>('/dashboard/projects');
  return response.data;
}

export function getDashboardMetricsErrorMessage(error: unknown): string {
  return extractTranslatedApiErrorMessage(error, 'home.dashboard.errors.metricsLoadFailed');
}

export function getAnnotationTrendsErrorMessage(error: unknown): string {
  return extractTranslatedApiErrorMessage(error, 'home.dashboard.errors.trendsLoadFailed');
}

export function getDashboardProjectsErrorMessage(error: unknown): string {
  return extractTranslatedApiErrorMessage(error, 'home.dashboard.errors.projectsLoadFailed');
}
