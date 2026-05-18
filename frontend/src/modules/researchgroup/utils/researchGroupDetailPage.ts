import { type InfiniteData } from '@tanstack/react-query';
import { type ProjectAssignedSummary, type SliceResponse } from '@/modules/project/types/project';

export function parseResearchGroupId(id: string | undefined): number {
  return Number(id);
}

export function isValidResearchGroupId(groupId: number): boolean {
  return Number.isFinite(groupId) && groupId > 0;
}

export function getAssignedProjectsFromPages(
  assignedProjectsData: InfiniteData<SliceResponse<ProjectAssignedSummary>> | undefined,
): ProjectAssignedSummary[] {
  return assignedProjectsData?.pages.flatMap((page) => page.content) ?? [];
}
