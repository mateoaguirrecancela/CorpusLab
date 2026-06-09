export const researchGroupQueryKeys = {
  all: ['research-groups'] as const,
  detail: (groupId: number) => ['research-groups', groupId] as const,
  invitations: ['research-groups', 'invitations'] as const,
};
