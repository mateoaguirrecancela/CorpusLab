import { api } from '@/app/config/axiosInstance';
import type { SliceResponse, SliceResponseDto } from '@/shared/types/slice';
import type {
  CreateProjectPayload,
  ProjectAssignedSummary,
  ProjectDetail,
  ProjectSummary,
  UpdateProjectPayload,
} from '@/modules/project/shared/types/project';
import { toProjectPayload } from '@/modules/project/shared/utils/projectFormUtils';

type ProjectListParams = {
  page?: number;
  size?: number;
  showArchived?: boolean;
};

export async function createProject(
  researchGroupId: number,
  payload: CreateProjectPayload,
): Promise<ProjectSummary> {
  const response = await api.post<ProjectSummary>(
    `/research-groups/${researchGroupId}/projects`,
    toProjectPayload(payload),
  );
  return response.data;
}

export async function updateProject(
  groupId: number,
  projectId: number,
  payload: UpdateProjectPayload,
): Promise<ProjectDetail> {
  const body: UpdateProjectPayload = {
    ...toProjectPayload(payload),
    participantUserIds: payload.participantUserIds,
    participantAssignments: payload.participantAssignments,
  };

  const response = await api.put<ProjectDetail>(
    `/research-groups/${groupId}/projects/${projectId}`,
    body,
  );
  return response.data;
}

export async function deleteProject(groupId: number, projectId: number): Promise<void> {
  await api.delete(`/research-groups/${groupId}/projects/${projectId}`);
}

export async function cleanupIncompleteProject(groupId: number, projectId: number): Promise<void> {
  await api.delete(`/research-groups/${groupId}/projects/${projectId}/wizard-cleanup`);
}

export async function archiveProject(projectId: number): Promise<ProjectDetail> {
  const response = await api.put<ProjectDetail>(`/projects/${projectId}/archive`);
  return response.data;
}

export async function unarchiveProject(projectId: number): Promise<ProjectDetail> {
  const response = await api.put<ProjectDetail>(`/projects/${projectId}/unarchive`);
  return response.data;
}

export async function getAssignedProjectsByGroup(
  groupId: number,
  { page = 0, size = 3, showArchived = false }: ProjectListParams = {},
): Promise<SliceResponse<ProjectAssignedSummary>> {
  const response = await api.get<SliceResponseDto<ProjectAssignedSummary>>(
    `/research-groups/${groupId}/projects/my`,
    {
      params: { page, size, showArchived },
    },
  );
  return response.data;
}

export async function getMyAssignedProjects({
  page = 0,
  size = 6,
  showArchived = false,
}: ProjectListParams = {}): Promise<SliceResponse<ProjectAssignedSummary>> {
  const response = await api.get<SliceResponseDto<ProjectAssignedSummary>>('/projects/my', {
    params: { page, size, showArchived },
  });
  return response.data;
}

export async function getProjectDetail(projectId: number): Promise<ProjectDetail> {
  const response = await api.get<ProjectDetail>(`/projects/${projectId}`);
  return response.data;
}
