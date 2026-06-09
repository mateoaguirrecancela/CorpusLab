import { api } from '@/app/config/axiosInstance';
import type {
  ProjectAnnotationWorkspace,
  ProjectAnnotationWarningResponse,
  SaveProjectAnnotationStepPayload,
  SaveProjectAnnotationStepResponse,
} from '@/modules/project/shared/types/project';

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
): Promise<ProjectAnnotationWarningResponse> {
  const { data } = await api.put<ProjectAnnotationWarningResponse>(
    `/projects/${projectId}/annotations/participants/${participantUserId}/steps/warning`,
    {
      datasetItemId,
      stepIndex,
    },
  );
  return data;
}

export async function resolveOwnProjectAnnotationWarning(
  projectId: number,
  datasetItemId: number,
  stepIndex: number,
): Promise<ProjectAnnotationWarningResponse> {
  const { data } = await api.put<ProjectAnnotationWarningResponse>(
    `/projects/${projectId}/annotations/steps/warning-resolution`,
    {
      datasetItemId,
      stepIndex,
    },
  );
  return data;
}
