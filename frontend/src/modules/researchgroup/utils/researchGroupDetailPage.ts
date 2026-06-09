import { type InfiniteData } from '@tanstack/react-query';
import { type SliceResponse } from '@/shared/types/slice';
import { type ResearchGroupAssignedProjectSummary } from '@/modules/researchgroup/types/researchGroup';

export function parseResearchGroupId(id: string | undefined): number {
  return Number(id);
}

export function isValidResearchGroupId(groupId: number): boolean {
  return Number.isFinite(groupId) && groupId > 0;
}

export function getAssignedProjectsFromPages(
  assignedProjectsData:
    | InfiniteData<SliceResponse<ResearchGroupAssignedProjectSummary>>
    | undefined,
): ResearchGroupAssignedProjectSummary[] {
  return assignedProjectsData?.pages.flatMap((page) => page.content) ?? [];
}
