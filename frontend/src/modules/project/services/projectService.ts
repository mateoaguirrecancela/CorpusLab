import { api } from '@/lib/api';
import i18n from '@/lib/i18n';
import {
  type AssignProjectParticipantsPayload,
  type ConfigureProjectSetupPayload,
  type CreateProjectPayload,
  type ProjectAssignedSummary,
  type ProjectDetail,
  type ProjectSetupResponse,
  type ProjectSummary,
  type UploadDatasetResponse,
} from '@/modules/project/types/project';

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

export async function uploadProjectDataset(
  groupId: number,
  projectId: number,
  files: File[],
): Promise<UploadDatasetResponse> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await api.post<UploadDatasetResponse>(
    `/research-groups/${groupId}/projects/${projectId}/dataset`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );

  return response.data;
}

export function getUploadDatasetErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'project.errors.datasetUploadFailed');
}

export async function configureProjectSetup(
  groupId: number,
  projectId: number,
  payload: ConfigureProjectSetupPayload,
): Promise<ProjectSetupResponse> {
  const body = {
    projectType: payload.projectType,
    labels: payload.labels,
    guidelineText: payload.guidelineText?.trim() || undefined,
    guidelinePdfBase64: payload.guidelinePdfBase64?.trim() || undefined,
  };

  const response = await api.put<ProjectSetupResponse>(
    `/research-groups/${groupId}/projects/${projectId}/setup`,
    body,
  );

  return response.data;
}

export function getProjectSetupErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'project.errors.setupFailed');
}

export async function assignProjectParticipants(
  groupId: number,
  projectId: number,
  payload: AssignProjectParticipantsPayload,
): Promise<void> {
  await api.post(`/research-groups/${groupId}/projects/${projectId}/participants`, payload);
}

export function getAssignParticipantsErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'project.errors.assignmentFailed');
}

export async function getAssignedProjectsByGroup(
  groupId: number,
): Promise<ProjectAssignedSummary[]> {
  const response = await api.get<ProjectAssignedSummary[]>(
    `/research-groups/${groupId}/projects/my`,
  );
  return response.data;
}

export async function getMyAssignedProjects(): Promise<ProjectAssignedSummary[]> {
  const response = await api.get<ProjectAssignedSummary[]>('/projects/my');
  return response.data;
}

export async function getProjectDetail(projectId: number): Promise<ProjectDetail> {
  const response = await api.get<ProjectDetail>(`/projects/${projectId}`);
  return response.data;
}

export function getProjectsLoadErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'project.errors.loadFailed');
}

export function getProjectDetailLoadErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'project.errors.detailLoadFailed');
}
