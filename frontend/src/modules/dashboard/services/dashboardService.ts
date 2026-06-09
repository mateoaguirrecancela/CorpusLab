import { api } from '@/app/config/axiosInstance';
import i18n from '@/app/config/i18n';
import { extractApiErrorMessage } from '@/shared/api/apiErrors';
import {
  type AnnotationTrendsResponse,
  type DashboardMetricsResponse,
  type DashboardProjectsResponse,
} from '@/modules/dashboard/types/dashboard';

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
  return extractApiErrorMessage(error, i18n.t('home.dashboard.errors.metricsLoadFailed'));
}

export function getAnnotationTrendsErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('home.dashboard.errors.trendsLoadFailed'));
}

export function getDashboardProjectsErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('home.dashboard.errors.projectsLoadFailed'));
}
