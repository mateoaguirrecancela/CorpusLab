import { useEffect } from 'react';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
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

  useEffect(() => {
    if (errorMessage.length > 0) {
      toast.error(errorMessage, { id: 'research-groups-load-error' });
    }
  }, [errorMessage]);

  return {
    errorMessage,
    groups,
    invitations,
    isLoading,
    openResearchGroup: (groupId: number) => navigate(`/home/research-groups/${groupId}`),
  };
}
