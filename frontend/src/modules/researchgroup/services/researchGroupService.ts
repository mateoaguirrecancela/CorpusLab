import { api } from '@/lib/api';
import i18n from '@/lib/i18n';
import { type ResearchGroupSummary } from '@/modules/researchgroup/types/researchGroup';

export async function getMyResearchGroups(): Promise<ResearchGroupSummary[]> {
  const response = await api.get<ResearchGroupSummary[]>('/research-groups');
  return response.data;
}

export function getResearchGroupsErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } })
      .response;
    return response?.data?.message ?? response?.data?.error ?? i18n.t('researchGroup.errors.loadFailed');
  }

  return i18n.t('researchGroup.errors.loadFailed');
}
