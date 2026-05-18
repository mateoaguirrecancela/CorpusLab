import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { invalidateQueryKeys } from '@/lib/queryInvalidation';
import { type CreateResearchGroupPayload } from '@/modules/researchgroup/types/createResearchGroup';
import {
  acceptResearchGroupInvitation,
  createResearchGroup,
  deleteResearchGroup,
  declineResearchGroupInvitation,
  getMyResearchGroupInvitations,
  getMyResearchGroups,
  getResearchGroupDetail,
  inviteResearchGroupMember,
  joinResearchGroupByCode,
  removeResearchGroupMember,
  updateResearchGroup,
  updateResearchGroupMemberRole,
} from '@/modules/researchgroup/services/researchGroupService';
import {
  assignedProjectsByGroupQueryKey,
  myAssignedProjectsQueryKey,
} from '@/modules/project/hooks/useProjectQueries';
import {
  type InvitableResearchGroupMemberRole,
  type InviteResearchGroupMemberPayload,
  type UpdateResearchGroupPayload,
} from '@/modules/researchgroup/types/researchGroup';

export const RESEARCH_GROUPS_QUERY_KEY = ['research-groups'] as const;
export const RESEARCH_GROUP_INVITATIONS_QUERY_KEY = ['research-groups', 'invitations'] as const;

export function researchGroupDetailQueryKey(groupId: number) {
  return ['research-groups', groupId] as const;
}

export function useResearchGroupsQuery() {
  return useQuery({
    queryKey: RESEARCH_GROUPS_QUERY_KEY,
    queryFn: getMyResearchGroups,
  });
}

export function useResearchGroupInvitationsQuery() {
  return useQuery({
    queryKey: RESEARCH_GROUP_INVITATIONS_QUERY_KEY,
    queryFn: getMyResearchGroupInvitations,
  });
}

export function useResearchGroupDetailQuery(groupId: number) {
  return useQuery({
    queryKey: researchGroupDetailQueryKey(groupId),
    queryFn: () => getResearchGroupDetail(groupId),
    enabled: Number.isFinite(groupId) && groupId > 0,
  });
}

export function useCreateResearchGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateResearchGroupPayload) => createResearchGroup(payload),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [RESEARCH_GROUPS_QUERY_KEY]);
    },
  });
}

export function useUpdateResearchGroupMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateResearchGroupPayload) => updateResearchGroup(groupId, payload),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [
        researchGroupDetailQueryKey(groupId),
        RESEARCH_GROUPS_QUERY_KEY,
      ]);
    },
  });
}

export function useDeleteResearchGroupMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => deleteResearchGroup(groupId),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [
        RESEARCH_GROUPS_QUERY_KEY,
        researchGroupDetailQueryKey(groupId),
        assignedProjectsByGroupQueryKey(groupId),
        myAssignedProjectsQueryKey(),
      ]);
    },
  });
}

export function useInviteResearchGroupMemberMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: InviteResearchGroupMemberPayload) =>
      inviteResearchGroupMember(groupId, payload),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [
        researchGroupDetailQueryKey(groupId),
        RESEARCH_GROUPS_QUERY_KEY,
      ]);
    },
  });
}

export function useJoinResearchGroupByCodeMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (code: string) => joinResearchGroupByCode(code),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [
        RESEARCH_GROUPS_QUERY_KEY,
        RESEARCH_GROUP_INVITATIONS_QUERY_KEY,
      ]);
    },
  });
}

export function useAcceptResearchGroupInvitationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: number) => acceptResearchGroupInvitation(invitationId),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [
        RESEARCH_GROUPS_QUERY_KEY,
        RESEARCH_GROUP_INVITATIONS_QUERY_KEY,
      ]);
    },
  });
}

export function useDeclineResearchGroupInvitationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: number) => declineResearchGroupInvitation(invitationId),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [RESEARCH_GROUP_INVITATIONS_QUERY_KEY]);
    },
  });
}

export function useUpdateResearchGroupMemberRoleMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      memberUserId,
      role,
    }: {
      memberUserId: number;
      role: InvitableResearchGroupMemberRole;
    }) => updateResearchGroupMemberRole(groupId, memberUserId, { role }),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [
        researchGroupDetailQueryKey(groupId),
        RESEARCH_GROUPS_QUERY_KEY,
      ]);
    },
  });
}

export function useRemoveResearchGroupMemberMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (memberUserId: number) => removeResearchGroupMember(groupId, memberUserId),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [
        researchGroupDetailQueryKey(groupId),
        RESEARCH_GROUPS_QUERY_KEY,
        assignedProjectsByGroupQueryKey(groupId),
        myAssignedProjectsQueryKey(),
      ]);
    },
  });
}
