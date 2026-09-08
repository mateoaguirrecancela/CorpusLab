import { api } from '@/app/config/axiosInstance';
import { extractApiErrorMessage } from '@/shared/api/apiErrors';
import i18n from '@/app/config/i18n';
import type { SliceResponse, SliceResponseDto } from '@/shared/types/slice';
import type { ResearchGroupAssignedProjectSummary } from '@/modules/researchgroup/types/researchGroup';

type ResearchGroupProjectListParams = {
  page?: number;
  size?: number;
  showArchived?: boolean;
};

export async function getAssignedProjectsByGroup(
  groupId: number,
  { page = 0, size = 3, showArchived = false }: ResearchGroupProjectListParams = {},
): Promise<SliceResponse<ResearchGroupAssignedProjectSummary>> {
  const response = await api.get<SliceResponseDto<ResearchGroupAssignedProjectSummary>>(
    `/research-groups/${groupId}/projects/my`,
    {
      params: { page, size, showArchived },
    },
  );

  return response.data;
}

export function getResearchGroupProjectsLoadErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('project.errors.loadFailed'));
}
