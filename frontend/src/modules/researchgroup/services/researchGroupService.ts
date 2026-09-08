import { api } from '@/app/config/axiosInstance';
import { extractApiErrorMessage } from '@/shared/api/apiErrors';
import i18n from '@/app/config/i18n';
import {
  type CreateResearchGroupPayload,
  type InviteResearchGroupMemberPayload,
  type ResearchGroupDetail,
  type ResearchGroupInvitation,
  type ResearchGroupSummary,
  type UpdateResearchGroupPayload,
  type UpdateResearchGroupMemberRolePayload,
} from '@/modules/researchgroup/types/researchGroup';
import {
  toInviteResearchGroupMemberPayload,
  toResearchGroupPayload,
} from '@/modules/researchgroup/utils/researchGroupForm';

export async function getMyResearchGroups(): Promise<ResearchGroupSummary[]> {
  const response = await api.get<ResearchGroupSummary[]>('/research-groups');
  return response.data;
}

export async function createResearchGroup(
  payload: CreateResearchGroupPayload,
): Promise<ResearchGroupSummary> {
  const response = await api.post<ResearchGroupSummary>(
    '/research-groups',
    toResearchGroupPayload(payload),
  );
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
  const response = await api.put<ResearchGroupDetail>(
    `/research-groups/${groupId}`,
    toResearchGroupPayload(payload),
  );
  return response.data;
}

export async function deleteResearchGroup(groupId: number): Promise<void> {
  await api.delete(`/research-groups/${groupId}`);
}

export async function inviteResearchGroupMember(
  groupId: number,
  payload: InviteResearchGroupMemberPayload,
): Promise<void> {
  await api.post(
    `/research-groups/${groupId}/invitations`,
    toInviteResearchGroupMemberPayload(payload),
  );
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

function researchGroupErrorMessage(translationKey: string) {
  return (error: unknown): string => extractApiErrorMessage(error, i18n.t(translationKey));
}

export const getResearchGroupsErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.loadFailed',
);
export const getCreateGroupErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.createFailed',
);
export const getUpdateGroupErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.updateFailed',
);
export const getDeleteGroupErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.deleteFailed',
);
export const getResearchGroupDetailErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.detailLoadFailed',
);
export const getInviteResearcherErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.inviteFailed',
);
export const getInvitationsErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.invitationsLoadFailed',
);
export const getJoinByCodeErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.joinByCodeFailed',
);
export const getAcceptInvitationErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.acceptInvitationFailed',
);
export const getDeclineInvitationErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.declineInvitationFailed',
);
export const getUpdateMemberRoleErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.updateMemberRoleFailed',
);
export const getRemoveMemberErrorMessage = researchGroupErrorMessage(
  'researchGroup.errors.removeMemberFailed',
);
