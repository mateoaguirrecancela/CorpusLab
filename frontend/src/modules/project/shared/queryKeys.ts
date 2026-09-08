export const projectQueryKeys = {
  all: ['projects'] as const,
  myAssigned: () => ['projects', 'my'] as const,
  myAssignedList: (showArchived: boolean) =>
    ['projects', 'my', showArchived ? 'archived' : 'active'] as const,
  assignedByGroup: (groupId: number) => ['projects', 'group', groupId, 'my'] as const,
  assignedByGroupList: (groupId: number, showArchived: boolean) =>
    ['projects', 'group', groupId, 'my', showArchived ? 'archived' : 'active'] as const,
  assignmentContext: (groupId: number) => ['projects', 'assignment-context', groupId] as const,
  detail: (projectId: number) => ['projects', 'detail', projectId] as const,
  metrics: (projectId: number) => ['projects', 'metrics', projectId] as const,
  annotationWorkspace: (projectId: number, offset: number, limit: number) =>
    ['projects', 'annotation-workspace', projectId, offset, limit] as const,
  participantAnnotationWorkspace: (
    projectId: number,
    participantUserId: number,
    offset: number,
    limit: number,
  ) =>
    [
      'projects',
      'annotation-workspace',
      projectId,
      'participant',
      participantUserId,
      offset,
      limit,
    ] as const,
};
