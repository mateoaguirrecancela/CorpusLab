import { api } from '@/lib/api';
import { extractApiErrorMessage } from '@/lib/apiErrors';
import i18n from '@/lib/i18n';
import {
  type AssignProjectParticipantsPayload,
  type ConfigureProjectSetupPayload,
  type CreateProjectPayload,
  type ProjectAssignedSummary,
  type ProjectAnnotationWorkspace,
  type ProjectDatasetItemContent,
  type ProjectDetail,
  type ProjectSetupResponse,
  type ProjectSummary,
  type SaveProjectAnnotationStepPayload,
  type SaveProjectAnnotationStepResponse,
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

export function getCreateProjectErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('project.errors.createFailed'));
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
  return extractApiErrorMessage(error, i18n.t('project.errors.datasetUploadFailed'));
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
  return extractApiErrorMessage(error, i18n.t('project.errors.setupFailed'));
}

export async function assignProjectParticipants(
  groupId: number,
  projectId: number,
  payload: AssignProjectParticipantsPayload,
): Promise<void> {
  await api.post(`/research-groups/${groupId}/projects/${projectId}/participants`, payload);
}

export function getAssignParticipantsErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('project.errors.assignmentFailed'));
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

export async function getProjectAnnotationWorkspace(
  projectId: number,
  offset = 0,
  limit = 50,
): Promise<ProjectAnnotationWorkspace> {
  const response = await api.get<ProjectAnnotationWorkspace>(
    `/projects/${projectId}/annotations/steps`,
    {
      params: { offset, limit },
    },
  );

  return response.data;
}

export async function saveProjectAnnotationStep(
  projectId: number,
  payload: SaveProjectAnnotationStepPayload,
): Promise<SaveProjectAnnotationStepResponse> {
  const response = await api.put<SaveProjectAnnotationStepResponse>(
    `/projects/${projectId}/annotations/steps`,
    payload,
  );

  return response.data;
}

function parseFileNameFromContentDisposition(
  contentDisposition: string | undefined,
): string | null {
  if (!contentDisposition) {
    return null;
  }

  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  const utf8FileName = utf8Match?.[1];
  if (utf8FileName) {
    try {
      return decodeURIComponent(utf8FileName);
    } catch {
      return utf8FileName;
    }
  }

  const quotedMatch = /filename="([^"]+)"/i.exec(contentDisposition);
  const quotedFileName = quotedMatch?.[1];
  if (quotedFileName) {
    return quotedFileName;
  }

  const plainMatch = /filename=([^;]+)/i.exec(contentDisposition);
  const plainFileName = plainMatch?.[1];
  if (plainFileName) {
    return plainFileName.trim();
  }

  return null;
}

export async function getProjectDatasetItemContent(
  projectId: number,
  datasetItemId: number,
): Promise<ProjectDatasetItemContent> {
  const response = await api.get<ArrayBuffer>(
    `/projects/${projectId}/dataset-items/${datasetItemId}/content`,
    {
      responseType: 'arraybuffer',
    },
  );

  const mimeTypeHeader = response.headers['content-type'];
  const mimeType =
    typeof mimeTypeHeader === 'string' && mimeTypeHeader.length > 0
      ? mimeTypeHeader
      : 'application/octet-stream';

  const fileName = parseFileNameFromContentDisposition(response.headers['content-disposition']);

  return {
    blob: new Blob([response.data], { type: mimeType }),
    mimeType,
    fileName,
  };
}

export function getProjectsLoadErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('project.errors.loadFailed'));
}

export function getProjectDetailLoadErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('project.errors.detailLoadFailed'));
}

export function getProjectAnnotationLoadErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('project.errors.annotationLoadFailed'));
}

export function getProjectAnnotationSaveErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('project.errors.annotationSaveFailed'));
}

export function getProjectDatasetItemContentErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, i18n.t('project.errors.sourceContentLoadFailed'));
}
