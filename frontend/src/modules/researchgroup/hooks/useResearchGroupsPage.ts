import { useNavigate } from 'react-router';
import { useToastMessages } from '@/hooks/useToastMessages';
import {
  useResearchGroupInvitationsQuery,
  useResearchGroupsQuery,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { getResearchGroupsErrorMessage } from '@/modules/researchgroup/services/researchGroupService';
import {
  type ResearchGroupInvitation,
  type ResearchGroupSummary,
} from '@/modules/researchgroup/types/researchGroup';

type ResearchGroupsPageState = Readonly<{
  errorMessage: string;
  groups: ResearchGroupSummary[];
  invitations: ResearchGroupInvitation[];
  isLoading: boolean;
  openResearchGroup: (groupId: number) => void;
}>;

export function useResearchGroupsPage(): ResearchGroupsPageState {
  const navigate = useNavigate();
  const { data: groups = [], isLoading, isError, error } = useResearchGroupsQuery();
  const { data: invitations = [] } = useResearchGroupInvitationsQuery();
  const errorMessage = isError ? getResearchGroupsErrorMessage(error) : '';

  useToastMessages({
    errorMessage,
    errorToastId: 'research-groups-load-error',
  });

  return {
    errorMessage,
    groups,
    invitations,
    isLoading,
    openResearchGroup: (groupId: number) => navigate(`/home/research-groups/${groupId}`),
  };
}
