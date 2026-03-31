import { api } from '@/lib/api';
import i18n from '@/lib/i18n';
import { type CreateResearchGroupPayload } from '@/modules/researchgroup/types/createResearchGroup';
import {
  type ResearchGroupDetail,
  type ResearchGroupSummary,
} from '@/modules/researchgroup/types/researchGroup';

export async function getMyResearchGroups(): Promise<ResearchGroupSummary[]> {
  const response = await api.get<ResearchGroupSummary[]>('/research-groups');
  return response.data;
}

export async function createResearchGroup(
  payload: CreateResearchGroupPayload,
): Promise<ResearchGroupSummary> {
  const body = {
    name: payload.name.trim(),
    description: payload.description?.trim() || undefined,
  };

  const response = await api.post<ResearchGroupSummary>('/research-groups', body);
  return response.data;
}

export async function getResearchGroupDetail(groupId: number): Promise<ResearchGroupDetail> {
  const response = await api.get<ResearchGroupDetail>(`/research-groups/${groupId}`);
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

export function getResearchGroupsErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.loadFailed');
}

export function getCreateGroupErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.createFailed');
}

export function getResearchGroupDetailErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.detailLoadFailed');
}
