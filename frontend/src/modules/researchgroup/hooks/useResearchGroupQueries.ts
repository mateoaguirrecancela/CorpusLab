import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type CreateResearchGroupPayload } from '@/modules/researchgroup/types/createResearchGroup';
import {
  acceptResearchGroupInvitation,
  createResearchGroup,
  declineResearchGroupInvitation,
  getMyResearchGroupInvitations,
  getMyResearchGroups,
  getResearchGroupDetail,
  inviteResearchGroupMember,
  joinResearchGroupByCode,
} from '@/modules/researchgroup/services/researchGroupService';
import { type InviteResearchGroupMemberPayload } from '@/modules/researchgroup/types/researchGroup';

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
      void queryClient.invalidateQueries({ queryKey: RESEARCH_GROUPS_QUERY_KEY });
    },
  });
}

export function useInviteResearchGroupMemberMutation(groupId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: InviteResearchGroupMemberPayload) =>
      inviteResearchGroupMember(groupId, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: researchGroupDetailQueryKey(groupId) });
      void queryClient.invalidateQueries({ queryKey: RESEARCH_GROUPS_QUERY_KEY });
    },
  });
}

export function useJoinResearchGroupByCodeMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (code: string) => joinResearchGroupByCode(code),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: RESEARCH_GROUPS_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: RESEARCH_GROUP_INVITATIONS_QUERY_KEY });
    },
  });
}

export function useAcceptResearchGroupInvitationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: number) => acceptResearchGroupInvitation(invitationId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: RESEARCH_GROUPS_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: RESEARCH_GROUP_INVITATIONS_QUERY_KEY });
    },
  });
}

export function useDeclineResearchGroupInvitationMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: number) => declineResearchGroupInvitation(invitationId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: RESEARCH_GROUP_INVITATIONS_QUERY_KEY });
    },
  });
}
