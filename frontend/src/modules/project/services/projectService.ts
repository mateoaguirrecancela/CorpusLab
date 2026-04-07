import { api } from '@/lib/api';
import i18n from '@/lib/i18n';
import { type CreateProjectPayload, type ProjectSummary } from '@/modules/project/types/project';

export async function createProject(
  researchGroupId: number,
  payload: CreateProjectPayload,
): Promise<ProjectSummary> {
  const body = {
    name: payload.name.trim(),
    description: payload.description?.trim() || undefined,
  };

  const response = await api.post<ProjectSummary>(
    `/research-groups/${researchGroupId}/projects`,
    body,
  );
  return response.data;
}

function extractApiErrorMessage(error: unknown, fallbackKey: string): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } })
      .response;
    return response?.data?.message ?? response?.data?.error ?? i18n.t(fallbackKey);
  }

  return i18n.t(fallbackKey);
}

export function getCreateProjectErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'project.errors.createFailed');
}
