import { api } from '@/lib/api';
import i18n from '@/lib/i18n';
import { type CreateResearchGroupPayload } from '@/modules/researchgroup/types/createResearchGroup';
import {
  type InviteResearchGroupMemberPayload,
  type ResearchGroupDetail,
  type ResearchGroupInvitation,
  type ResearchGroupSummary,
  type UpdateResearchGroupPayload,
  type UpdateResearchGroupMemberRolePayload,
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

export async function updateResearchGroup(
  groupId: number,
  payload: UpdateResearchGroupPayload,
): Promise<ResearchGroupDetail> {
  const body = {
    name: payload.name.trim(),
    description: payload.description?.trim() || undefined,
  };

  const response = await api.put<ResearchGroupDetail>(`/research-groups/${groupId}`, body);
  return response.data;
}

export async function inviteResearchGroupMember(
  groupId: number,
  payload: InviteResearchGroupMemberPayload,
): Promise<void> {
  const body = {
    email: payload.email.trim(),
    role: payload.role,
    expiresAt: payload.expiresAt,
  };

  await api.post(`/research-groups/${groupId}/invitations`, body);
}

export async function getMyResearchGroupInvitations(): Promise<ResearchGroupInvitation[]> {
  const response = await api.get<ResearchGroupInvitation[]>('/research-groups/my-invitations');
  return response.data;
}

export async function joinResearchGroupByCode(code: string): Promise<ResearchGroupSummary> {
  const response = await api.post<ResearchGroupSummary>('/research-groups/join-by-code', {
    code: code.trim(),
  });
  return response.data;
}

export async function acceptResearchGroupInvitation(
  invitationId: number,
): Promise<ResearchGroupSummary> {
  const response = await api.post<ResearchGroupSummary>(
    `/research-groups/my-invitations/${invitationId}/accept`,
  );
  return response.data;
}

export async function declineResearchGroupInvitation(invitationId: number): Promise<void> {
  await api.post(`/research-groups/my-invitations/${invitationId}/decline`);
}

export async function updateResearchGroupMemberRole(
  groupId: number,
  memberUserId: number,
  payload: UpdateResearchGroupMemberRolePayload,
): Promise<void> {
  await api.post(`/research-groups/${groupId}/members/${memberUserId}/role`, {
    role: payload.role,
  });
}

export async function removeResearchGroupMember(
  groupId: number,
  memberUserId: number,
): Promise<void> {
  await api.post(`/research-groups/${groupId}/members/${memberUserId}/remove`);
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

export function getUpdateGroupErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.updateFailed');
}

export function getResearchGroupDetailErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.detailLoadFailed');
}

export function getInviteResearcherErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.inviteFailed');
}

export function getInvitationsErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.invitationsLoadFailed');
}

export function getJoinByCodeErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.joinByCodeFailed');
}

export function getAcceptInvitationErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.acceptInvitationFailed');
}

export function getDeclineInvitationErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.declineInvitationFailed');
}

export function getUpdateMemberRoleErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.updateMemberRoleFailed');
}

export function getRemoveMemberErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'researchGroup.errors.removeMemberFailed');
}
