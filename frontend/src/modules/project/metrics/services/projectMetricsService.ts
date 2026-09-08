import { api } from '@/app/config/axiosInstance';
import { toProjectMetrics } from '@/modules/project/shared/mappers/projectMappers';
import type { ProjectMetric, ProjectMetricsResponse } from '@/modules/project/shared/types/project';

export async function getProjectMetrics(projectId: number): Promise<ProjectMetric[]> {
  const response = await api.get<ProjectMetricsResponse>(`/projects/${projectId}/metrics`);
  return toProjectMetrics(response.data);
}
