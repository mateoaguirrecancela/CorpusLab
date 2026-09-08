import { useInfiniteQuery } from '@tanstack/react-query';
import { projectQueryKeys } from '@/modules/project/shared/queryKeys';
import { getAssignedProjectsByGroup } from '@/modules/researchgroup/services/researchGroupProjectService';
import { isValidResearchGroupId } from '@/modules/researchgroup/utils/researchGroupDetailPage';

const RESEARCH_GROUP_PROJECTS_PAGE_SIZE = 3;

export function useResearchGroupAssignedProjectsQuery(groupId: number, showArchived = false) {
  return useInfiniteQuery({
    queryKey: projectQueryKeys.assignedByGroupList(groupId, showArchived),
    queryFn: ({ pageParam }) =>
      getAssignedProjectsByGroup(groupId, {
        page: pageParam,
        size: RESEARCH_GROUP_PROJECTS_PAGE_SIZE,
        showArchived,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 1),
    enabled: isValidResearchGroupId(groupId),
  });
}
