import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  invalidateResearchGroupInvitationQueries,
  invalidateResearchGroupProjectMembershipQueries,
  invalidateResearchGroupWriteQueries,
} from '@/app/config/queryInvalidation';
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
  type CreateResearchGroupPayload,
  type InvitableResearchGroupMemberRole,
  type InviteResearchGroupMemberPayload,
  type UpdateResearchGroupPayload,
} from '@/modules/researchgroup/types/researchGroup';
import { researchGroupQueryKeys } from '@/modules/researchgroup/queryKeys';

export function useResearchGroupsQuery() {
  return useQuery({
    queryKey: researchGroupQueryKeys.all,
    queryFn: getMyResearchGroups,
  });
}

export function useResearchGroupInvitationsQuery() {
  return useQuery({
    queryKey: researchGroupQueryKeys.invitations,
    queryFn: getMyResearchGroupInvitations,
  });
}

export function useResearchGroupDetailQuery(groupId: number) {
  return useQuery({
    queryKey: researchGroupQueryKeys.detail(groupId),
    queryFn: () => getResearchGroupDetail(groupId),
    enabled: Number.isFinite(groupId) && groupId > 0,
  });
}

export function useCreateResearchGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateResearchGroupPayload) => createResearchGroup(payload),
    onSuccess: () => {
      invalidateResearchGroupWriteQueries(queryClient);
    },
  });
}

export function useUpdateResearchGroupMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateResearchGroupPayload) => updateResearchGroup(groupId, payload),
    onSuccess: () => {
      invalidateResearchGroupWriteQueries(queryClient, groupId);
    },
  });
}

export function useDeleteResearchGroupMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => deleteResearchGroup(groupId),
    onSuccess: () => {
      invalidateResearchGroupProjectMembershipQueries(queryClient, groupId);
    },
  });
}

export function useInviteResearchGroupMemberMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: InviteResearchGroupMemberPayload) =>
      inviteResearchGroupMember(groupId, payload),
    onSuccess: () => {
      invalidateResearchGroupWriteQueries(queryClient, groupId);
    },
  });
}

export function useJoinResearchGroupByCodeMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (code: string) => joinResearchGroupByCode(code),
    onSuccess: () => {
      invalidateResearchGroupInvitationQueries(queryClient);
    },
  });
}

export function useAcceptResearchGroupInvitationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: number) => acceptResearchGroupInvitation(invitationId),
    onSuccess: () => {
      invalidateResearchGroupInvitationQueries(queryClient);
    },
  });
}

export function useDeclineResearchGroupInvitationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: number) => declineResearchGroupInvitation(invitationId),
    onSuccess: () => {
      invalidateResearchGroupInvitationQueries(queryClient);
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
      invalidateResearchGroupWriteQueries(queryClient, groupId);
    },
  });
}

export function useRemoveResearchGroupMemberMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (memberUserId: number) => removeResearchGroupMember(groupId, memberUserId),
    onSuccess: () => {
      invalidateResearchGroupProjectMembershipQueries(queryClient, groupId);
    },
  });
}
