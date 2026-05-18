import { type ResearchGroupInvitation } from '@/modules/researchgroup/types/researchGroup';

export type ResearchGroupInvitationListStatus = 'empty' | 'error' | 'loading' | 'ready';

type InvitationListStatusInput = {
  invitations: readonly ResearchGroupInvitation[];
  isError: boolean;
  isLoading: boolean;
};

export function getInvitationListStatus({
  invitations,
  isError,
  isLoading,
}: InvitationListStatusInput): ResearchGroupInvitationListStatus {
  if (isLoading) {
    return 'loading';
  }

  if (isError) {
    return 'error';
  }

  return invitations.length > 0 ? 'ready' : 'empty';
}

export function getPendingInvitationId(
  acceptingInvitationId: number | undefined,
  decliningInvitationId: number | undefined,
): number | undefined {
  return acceptingInvitationId ?? decliningInvitationId;
}
