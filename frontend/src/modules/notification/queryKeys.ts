export const notificationQueryKeys = {
  all: ['notifications'] as const,
  list: (limit: number) => ['notifications', limit] as const,
};
