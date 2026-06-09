import { api } from '@/app/config/axiosInstance';
import type { ProjectSetupResearchGroupSummary } from '@/modules/project/setup/types/projectSetup';

export async function getProjectSetupResearchGroups(): Promise<ProjectSetupResearchGroupSummary[]> {
  const response = await api.get<ProjectSetupResearchGroupSummary[]>('/research-groups');
  return response.data;
}
