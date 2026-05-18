import { type AxiosResponse } from 'axios';
import { api } from '@/lib/api';
import { extractApiErrorMessage } from '@/lib/apiErrors';
import i18n from '@/lib/i18n';
import { getSessionToken } from '@/modules/auth/services/sessionService';
import {
  type AssignProjectParticipantsPayload,
  type ConfigureProjectSetupPayload,
  type CreateProjectPayload,
  type ProjectAssignmentContext,
  type ProjectAssignedSummary,
  type ProjectAnnotationWorkspace,
  type ProjectDatasetItemContent,
  type ProjectDetail,
  type ProjectMetric,
  type ProjectMetricsResponse,
  type ProjectSetupResponse,
  type ProjectSummary,
  type SaveProjectAnnotationStepPayload,
  type SaveProjectAnnotationStepResponse,
  type SliceResponse,
  type UpdateProjectPayload,
  type UploadDatasetEvent,
  type UploadDatasetJobResponse,
  type UploadDatasetResponse,
} from '@/modules/project/types/project';
import { optionalTrimmedText, toProjectPayload } from '@/modules/project/utils/projectFormUtils';

const DATASET_UPLOAD_TIMEOUT_MS = 15 * 60 * 1000;

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

export async function uploadProjectDataset(
  groupId: number,
  projectId: number,
  files: File[],
): Promise<UploadDatasetResponse> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await api.post<UploadDatasetJobResponse>(
    `/research-groups/${groupId}/projects/${projectId}/dataset`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );

  return waitForDatasetUploadCompletion(projectId, response.data.jobId);
}

async function waitForDatasetUploadCompletion(
  projectId: number,
  jobId: string,
): Promise<UploadDatasetResponse> {
  const token = getSessionToken();
  const language = i18n.resolvedLanguage ?? i18n.language ?? 'en';
  const controller = new AbortController();
  let timedOut = false;
  const timeoutId = window.setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, DATASET_UPLOAD_TIMEOUT_MS);

  try {
    const response = await fetch(`/api/projects/${projectId}/dataset/upload-events/${jobId}`, {
      headers: {
        Accept: 'text/event-stream',
        'Accept-Language': language,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      signal: controller.signal,
    });

    if (!response.ok || !response.body) {
      throw new Error(i18n.t('project.errors.datasetUploadFailed'));
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      const events = buffer.split('\n\n');
      buffer = events.pop() ?? '';

      for (const rawEvent of events) {
        const uploadEvent = parseDatasetUploadEvent(rawEvent);
        if (!uploadEvent) {
          continue;
        }

        if (uploadEvent.status === 'COMPLETED' && uploadEvent.result) {
          return uploadEvent.result;
        }

        if (uploadEvent.status === 'FAILED') {
          throw new Error(uploadEvent.message ?? i18n.t('project.errors.datasetUploadFailed'));
        }
      }
    }

    throw new Error(i18n.t('project.errors.datasetUploadFailed'));
  } catch (error) {
    if (timedOut || (error instanceof DOMException && error.name === 'AbortError')) {
      throw new Error(i18n.t('project.errors.datasetUploadFailed'));
    }
    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

function parseDatasetUploadEvent(rawEvent: string): UploadDatasetEvent | null {
  const data = rawEvent
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice('data:'.length).trimStart())
    .join('\n');

  if (!data) {
    return null;
  }

  return JSON.parse(data) as UploadDatasetEvent;
}

export async function configureProjectSetup(
  groupId: number,
  projectId: number,
  payload: ConfigureProjectSetupPayload,
): Promise<ProjectSetupResponse> {
  const request = {
    projectType: payload.projectType,
    labels: payload.labels,
    guidelineText: optionalTrimmedText(payload.guidelineText),
    annotationTargetColumn: optionalTrimmedText(payload.annotationTargetColumn),
  };
  const formData = new FormData();
  formData.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));

  if (payload.guidelinePdfFile) {
    formData.append('guidelinePdfFile', payload.guidelinePdfFile);
  }

  const response = await api.put<ProjectSetupResponse>(
    `/research-groups/${groupId}/projects/${projectId}/setup`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );

  return response.data;
}

export async function assignProjectParticipants(
  groupId: number,
  projectId: number,
  payload: AssignProjectParticipantsPayload,
): Promise<void> {
  await api.post(`/research-groups/${groupId}/projects/${projectId}/participants`, payload);
}

export async function getProjectAssignmentContext(
  groupId: number,
): Promise<ProjectAssignmentContext> {
  const response = await api.get<ProjectAssignmentContext>(
    `/research-groups/${groupId}/projects/assignment-context`,
  );
  return response.data;
}

type ProjectListParams = {
  page?: number;
  size?: number;
  showArchived?: boolean;
};

export async function getAssignedProjectsByGroup(
  groupId: number,
  { page = 0, size = 3, showArchived = false }: ProjectListParams = {},
): Promise<SliceResponse<ProjectAssignedSummary>> {
  const response = await api.get<SliceResponse<ProjectAssignedSummary>>(
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
  const response = await api.get<SliceResponse<ProjectAssignedSummary>>('/projects/my', {
    params: { page, size, showArchived },
  });
  return response.data;
}

export async function getProjectDetail(projectId: number): Promise<ProjectDetail> {
  const response = await api.get<ProjectDetail>(`/projects/${projectId}`);
  return response.data;
}

export async function getProjectGuidelinePdf(
  projectId: number,
): Promise<ProjectDatasetItemContent> {
  const response = await api.get<ArrayBuffer>(`/projects/${projectId}/guideline-pdf`, {
    responseType: 'arraybuffer',
  });

  return toDatasetItemContent(response, 'application/pdf');
}

export async function getProjectMetrics(projectId: number): Promise<ProjectMetric[]> {
  const response = await api.get<ProjectMetricsResponse | ProjectMetric[]>(
    `/projects/${projectId}/metrics`,
  );

  return Array.isArray(response.data) ? response.data : response.data.metrics;
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

export async function getProjectParticipantAnnotationWorkspace(
  projectId: number,
  participantUserId: number,
  offset = 0,
  limit = 50,
): Promise<ProjectAnnotationWorkspace> {
  const response = await api.get<ProjectAnnotationWorkspace>(
    `/projects/${projectId}/annotations/participants/${participantUserId}/steps`,
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

export async function toggleProjectAnnotationWarning(
  projectId: number,
  participantUserId: number,
  datasetItemId: number,
  stepIndex: number,
): Promise<SaveProjectAnnotationStepResponse> {
  const { data } = await api.put<SaveProjectAnnotationStepResponse>(
    `/projects/${projectId}/annotations/participants/${participantUserId}/steps/warning`,
    {
      datasetItemId,
      stepIndex,
    },
  );
  return data;
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

  return toDatasetItemContent(response, 'application/octet-stream');
}

export async function exportProjectAnnotationResultsCsv(
  projectId: number,
): Promise<ProjectDatasetItemContent> {
  const response = await api.get<ArrayBuffer>(`/projects/${projectId}/annotations/export`, {
    responseType: 'arraybuffer',
  });

  return toDatasetItemContent(response, 'text/csv;charset=UTF-8');
}

function getResponseHeader(
  response: AxiosResponse<ArrayBuffer>,
  headerName: string,
): string | undefined {
  const headerValue = response.headers[headerName];
  return typeof headerValue === 'string' && headerValue.length > 0 ? headerValue : undefined;
}

function toDatasetItemContent(
  response: AxiosResponse<ArrayBuffer>,
  fallbackMimeType: string,
): ProjectDatasetItemContent {
  const mimeType = getResponseHeader(response, 'content-type') ?? fallbackMimeType;

  return {
    blob: new Blob([response.data], { type: mimeType }),
    mimeType,
    fileName: parseFileNameFromContentDisposition(
      getResponseHeader(response, 'content-disposition'),
    ),
  };
}

function projectErrorMessage(translationKey: string) {
  return (error: unknown): string => extractApiErrorMessage(error, i18n.t(translationKey));
}

export const getCreateProjectErrorMessage = projectErrorMessage('project.errors.createFailed');
export const getUpdateProjectErrorMessage = projectErrorMessage('project.errors.updateFailed');
export const getDeleteProjectErrorMessage = projectErrorMessage('project.errors.deleteFailed');
export const getArchiveProjectErrorMessage = projectErrorMessage('project.errors.archiveFailed');
export const getUnarchiveProjectErrorMessage = projectErrorMessage(
  'project.errors.unarchiveFailed',
);
export const getUploadDatasetErrorMessage = projectErrorMessage(
  'project.errors.datasetUploadFailed',
);
export const getProjectSetupErrorMessage = projectErrorMessage('project.errors.setupFailed');
export const getAssignParticipantsErrorMessage = projectErrorMessage(
  'project.errors.assignmentFailed',
);
export const getProjectsLoadErrorMessage = projectErrorMessage('project.errors.loadFailed');
export const getProjectDetailLoadErrorMessage = projectErrorMessage(
  'project.errors.detailLoadFailed',
);
export const getProjectAssignmentContextErrorMessage = projectErrorMessage(
  'project.errors.assignmentContextLoadFailed',
);
export const getProjectGuidelinePdfErrorMessage = projectErrorMessage(
  'project.errors.guidelinePdfLoadFailed',
);
export const getProjectMetricsLoadErrorMessage = projectErrorMessage(
  'project.errors.metricsLoadFailed',
);
export const getProjectAnnotationLoadErrorMessage = projectErrorMessage(
  'project.errors.annotationLoadFailed',
);
export const getProjectAnnotationSaveErrorMessage = projectErrorMessage(
  'project.errors.annotationSaveFailed',
);
export const getProjectAnnotationExportErrorMessage = projectErrorMessage(
  'project.errors.annotationExportFailed',
);
export const getProjectDatasetItemContentErrorMessage = projectErrorMessage(
  'project.errors.sourceContentLoadFailed',
);
