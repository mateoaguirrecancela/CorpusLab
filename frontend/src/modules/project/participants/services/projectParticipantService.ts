import { api } from '@/app/config/axiosInstance';
import type {
  AssignProjectParticipantsPayload,
  ProjectAssignmentContext,
} from '@/modules/project/shared/types/project';

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
