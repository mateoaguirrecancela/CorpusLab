import { useQuery } from '@tanstack/react-query';
import { researchGroupQueryKeys } from '@/modules/researchgroup/queryKeys';
import { getProjectSetupResearchGroups } from '@/modules/project/setup/services/projectSetupResearchGroupService';

export function useProjectSetupResearchGroupsQuery() {
  return useQuery({
    queryKey: researchGroupQueryKeys.all,
    queryFn: getProjectSetupResearchGroups,
  });
}
