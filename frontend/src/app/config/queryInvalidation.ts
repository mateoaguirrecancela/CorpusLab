import { type QueryClient, type QueryKey } from '@tanstack/react-query';
import { dashboardQueryKeys } from '@/modules/dashboard/queryKeys';
import { notificationQueryKeys } from '@/modules/notification/queryKeys';
import { projectQueryKeys } from '@/modules/project/shared/queryKeys';
import { researchGroupQueryKeys } from '@/modules/researchgroup/queryKeys';

export function invalidateQueryKeys(
  queryClient: QueryClient,
  queryKeys: readonly QueryKey[],
): void {
  for (const queryKey of queryKeys) {
    void queryClient.invalidateQueries({ queryKey });
  }
}

type ProjectWriteInvalidationInput = Readonly<{
  groupId: number;
  projectId?: number;
}>;

export function invalidateProjectWriteQueries(
  queryClient: QueryClient,
  { groupId, projectId }: ProjectWriteInvalidationInput,
): void {
  invalidateQueryKeys(queryClient, [
    projectQueryKeys.myAssigned(),
    dashboardQueryKeys.all,
    projectQueryKeys.assignedByGroup(groupId),
    researchGroupQueryKeys.detail(groupId),
    researchGroupQueryKeys.all,
    ...(projectId ? [projectQueryKeys.detail(projectId)] : []),
  ]);
}

export function invalidateProjectAnnotationSaveQueries(
  queryClient: QueryClient,
  projectId: number,
): void {
  invalidateQueryKeys(queryClient, [
    projectQueryKeys.myAssigned(),
    dashboardQueryKeys.all,
    projectQueryKeys.metrics(projectId),
  ]);
}

export function invalidateProjectAnnotationWarningQueries(
  queryClient: QueryClient,
  queryKey: QueryKey,
): void {
  invalidateQueryKeys(queryClient, [dashboardQueryKeys.all, queryKey]);
}

export function invalidateResearchGroupWriteQueries(
  queryClient: QueryClient,
  groupId?: number,
): void {
  invalidateQueryKeys(queryClient, [
    researchGroupQueryKeys.all,
    ...(groupId ? [researchGroupQueryKeys.detail(groupId)] : []),
  ]);
}

export function invalidateResearchGroupProjectMembershipQueries(
  queryClient: QueryClient,
  groupId: number,
): void {
  invalidateQueryKeys(queryClient, [
    researchGroupQueryKeys.all,
    researchGroupQueryKeys.detail(groupId),
    projectQueryKeys.assignedByGroup(groupId),
    projectQueryKeys.myAssigned(),
  ]);
}

export function invalidateResearchGroupInvitationQueries(queryClient: QueryClient): void {
  invalidateQueryKeys(queryClient, [
    researchGroupQueryKeys.all,
    researchGroupQueryKeys.invitations,
  ]);
}

export function invalidateNotificationChangeQueries(queryClient: QueryClient): void {
  invalidateQueryKeys(queryClient, [notificationQueryKeys.all, dashboardQueryKeys.all]);
}
